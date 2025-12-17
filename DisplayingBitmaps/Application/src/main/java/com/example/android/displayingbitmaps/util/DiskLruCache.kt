/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused", "SameParameterValue", "UNNECESSARY_NOT_NULL_ASSERTION", "ReplaceNotNullAssertionWithElvisReturn", "JoinDeclarationAndAssignment", "ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate",
    "UnusedImport",
    "RedundantSuppression"
)

package com.example.android.displayingbitmaps.util

import com.example.android.displayingbitmaps.util.DiskLruCache.Editor
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.Charset
import java.util.Arrays
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 *
 * Taken from the JB source code, can be found in:
 * libcore/luni/src/main/java/libcore/io/DiskLruCache.java
 * or direct link:
 * https://android.googlesource.com/platform/libcore/+/android-4.1.1_r1/luni/src/main/java/libcore/io/DiskLruCache.java
 *
 * A cache that uses a bounded amount of space on a filesystem. Each cache entry has a string key
 * and a fixed number of values. Values are byte sequences, accessible as streams or files. Each
 * value must be between `0` and [Integer.MAX_VALUE] bytes in length.
 *
 * The cache stores its data in a directory on the filesystem. This directory must be exclusive to
 * the cache; the cache may delete or overwrite files from its directory. It is an error for
 * multiple processes to use the same cache directory at the same time.
 *
 * This cache limits the number of bytes that it will store on the filesystem. When the number of
 * stored bytes exceeds the limit, the cache will remove entries in the background until the limit
 * is satisfied. The limit is not strict: the cache may temporarily exceed it while waiting for
 * files to be deleted. The limit does not include filesystem overhead or the cache journal so
 * space-sensitive applications should set a conservative limit.
 *
 * Clients call [edit] to create or update the values of an entry. An entry may have only one editor
 * at one time; if a value is not available to be edited then [edit] will return `null`.
 *
 *  * When an entry is being **created** it is necessary to supply a full set of values; the empty
 *  value should be used as a placeholder if necessary.
 *
 *  * When an entry is being **edited**, it is not necessary to supply data for every value; values
 *  default to their previous value.
 *
 * Every [edit] call must be matched by a call to [Editor.commit] or [Editor.abort]. Committing is
 * atomic: a read observes the full set of values as they were before or after the commit, but never
 * a mix of values.
 *
 * Clients call [get] to read a snapshot of an entry. The read will observe the value at the time
 * that [get] was called. Updates and removals after the call do not impact ongoing reads.
 *
 * This class is tolerant of some I/O errors. If files are missing from the filesystem, the
 * corresponding entries will be dropped from the cache. If an error occurs while writing a cache
 * value, the edit will fail silently. Callers should handle other problems by catching
 * [IOException] and responding appropriately.
 */
@Suppress("UNCHECKED_CAST")
class DiskLruCache private constructor(
    /**
     * Directory used for writing our files to, set by our constructor.
     */
    val directory: File,
    /**
     * Version of app using us, set by our constructor and written to journal to verify on restart.
     */
    private val appVersion: Int, valueCount: Int, maxSize: Long
) : Closeable {
    /*
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dace6 832 21054
     *     DIRTY 335c4c6028171123452441a9c313c52
     *     CLEAN 335c4c60281713838317179c313c52 3934 2342
     *     REMOVE 335c4c602817112837475631a9c313c52
     *     DIRTY 1ab96a1711c3b4df538496d8b330771a7a
     *     CLEAN 1ab96a171f3a3f438496d8b330771a7a 1600 234
     *     READ 335c4c602817125ad5da5a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803d4d46
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */

    /**
     * File we write our journal to (and read back when restarting) named: [JOURNAL_FILE] = "journal"
     */
    private val journalFile: File

    /**
     * Temporary file to use when compacting journal. named: [JOURNAL_FILE_TMP] = "journal.tmp"
     */
    private val journalFileTmp: File

    /**
     * Maximum size of all the files in cache, set in constructor.
     */
    private val maxSize: Long

    /**
     * The number of values per cache entry (always 1 in our case)
     */
    private val valueCount: Int

    /**
     * Number of bytes currently being used to store the values in this cache. This may be greater
     * than the max size if a background deletion is pending.
     */
    private var size: Long = 0

    /**
     * [BufferedWriter] being used to write to journal.
     */
    private var journalWriter: Writer? = null

    /**
     * Hash of [Entry] objects pointing to disk cache keyed by a string generated by the
     * client. It is a linked hash map whose order of iteration is the order in which its entries
     * were last accessed, from least-recently accessed to most-recently (access-order).
     * This kind of map is well-suited to building LRU caches.
     */
    private val lruEntries = LinkedHashMap<String, Entry?>(
        /* initialCapacity = */ 0,
        /* loadFactor = */ 0.75f,
        /* accessOrder = */ true
    )

    /**
     * Count of number of operations committed to journal, used to determine whether a journal
     * rebuild will be profitable.
     */
    private var redundantOpCount = 0

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private var nextSequenceNumber: Long = 0

    /**
     * This cache uses a single background thread to evict entries.
     */
    private val executorService: ExecutorService = ThreadPoolExecutor(
        /* corePoolSize = */ 0,
        /* maximumPoolSize = */ 1,
        /* keepAliveTime = */ 60L,
        /* unit = */ TimeUnit.SECONDS,
        /* workQueue = */ LinkedBlockingQueue()
    )

    /**
     * Background thread used to evict entries. [Callable] used to cleanup our cache. Synchronized
     * on this, if [Writer] field [journalWriter] is `null` we return `null` (it is already closed).
     * Otherwise we call our method [trimToSize] which removes cache entries until [size] (the number
     * of bytes used) is less than [maxSize] (the maximum number of bytes to use for our cache).
     * Then if our method [journalRebuildRequired] returns `true` (it determines that a rebuild of
     * the journal would be profitable) we call our method [rebuildJournal] to rebuild the journal,
     * and set [redundantOpCount] to 0. Once outside of the synchronized block we return `null` to
     * the caller.
     *
     * @return `null`
     * @throws Exception all exceptions thrown by methods we call.
     */
    private val cleanupCallable: Callable<Void> = Callable {
        synchronized(this@DiskLruCache) {
            if (journalWriter == null) {
                return@Callable null // closed
            }
            trimToSize()
            if (journalRebuildRequired()) {
                rebuildJournal()
                redundantOpCount = 0
            }
        }
        null
    }

    /**
     * Our constructor. We store our parameter `File directory` in our field `directory`,
     * `int appVersion` in our field `appVersion`, initialize our field `File journalFile`
     * with a new file in `directory` with the name JOURNAL_FILE, initialize our field
     * `File journalFileTmp` with a new file in `directory` with the name JOURNAL_FILE_TMP,
     * store our parameter `int valueCount` in our field `valueCount`, and store our
     * parameter `int maxSize` in our field `maxSize`.
     *
     * param directory a writable directory
     * param appVersion version of the app
     * param valueCount the number of values per cache entry. Must be positive.
     * param maxSize the maximum number of bytes this cache should use to store
     */
    init {
        journalFile = File(directory, JOURNAL_FILE)
        journalFileTmp = File(directory, JOURNAL_FILE_TMP)
        this.valueCount = valueCount
        this.maxSize = maxSize
    }

    /**
     * Reads a pre-existing journal line by line, verifying the header first then processing the rest
     * of the journal in order to populate our field `LinkedHashMap<String, Entry> lruEntries`
     * with `Entry` objects describing the current state of our disk cache. We construct the
     * `BufferedInputStream` `InputStream in` to read from `File journalFile`, then
     * wrapped in a try block with a finally block that calls our `closeQuietly` method to close
     * `inputStream` we read the first five lines of `inputStream` into the variables `String magic`,
     * `String version`, `String appVersionString`, `String valueCountString` and
     * `String blank`. We then throw an IOException if any of these strings do not match what
     * we would write as the header to a journal file.
     *
     * We then loop forever executing our method `readJournalLine` on the string read by our
     * `readAsciiLine` from `inputStream`. `readJournalLine` processes each line and updates
     * the contents of our field `LinkedHashMap<String, Entry> lruEntries` based on the contents
     * of the journal line. We break out of this infinite loop when we catch EOFException.
     *
     * @throws IOException if an IO error of any sort occurs.
     */
    @Throws(IOException::class)
    private fun readJournal() {
        val inputStream: InputStream = BufferedInputStream(FileInputStream(journalFile), IO_BUFFER_SIZE)
        try {
            val magic = readAsciiLine(inputStream)
            val version = readAsciiLine(inputStream)
            val appVersionString = readAsciiLine(inputStream)
            val valueCountString = readAsciiLine(inputStream)
            val blank = readAsciiLine(inputStream)
            if (MAGIC != magic
                || VERSION_1 != version
                || Integer.toString(appVersion) != appVersionString
                || Integer.toString(valueCount) != valueCountString
                || "" != blank) {
                throw IOException("unexpected journal header: ["
                    + magic + ", " + version + ", " + valueCountString + ", " + blank + "]")
            }
            while (true) {
                try {
                    readJournalLine(readAsciiLine(inputStream))
                } catch (endOfJournal: EOFException) {
                    break
                }
            }
        } finally {
            closeQuietly(inputStream)
        }
    }

    /**
     * TODO: Continue here.
     * Processes a single `String line` from an old journal and updates the contents of our field
     * `LinkedHashMap<String, Entry> lruEntries` based on the contents of the journal line.
     * First we initialize `String[] parts` by splitting our parameter `String line` on
     * blanks. If there are not at least two strings in `parts` after doing this we throw
     * IOException "unexpected journal line: ". We initialize `String key` to `parts[1]`.
     *
     *
     * If `parts[0]` is equal to the string REMOVE ("REMOVE") and there are 2 strings in
     * `parts` we remove the `Entry` in `LinkedHashMap<String, Entry> lruEntries`
     * with the key `key` and return.
     *
     *
     * We initialize `Entry entry` with the `Entry` in `lruEntries` with the key
     * `key`, if this is null we set `entry` to a new instance of `Entry` for
     * `key` and put it in `lruEntries` under the key `key`.
     *
     *
     * We now handle three different cases of the value of `parts[0]`:
     *
     *  1.
     * If `parts[0]` is equal to the string CLEAN ("CLEAN") and there are `valueCount`
     * plus 2 strings in `parts`, we set the `readable` field of `entry` to true,
     * set its `currentEditor` field to null, and call its `setLengths` method to set
     * the values in the `lengths[]` array field of `entry` to the long values encoded
     * in the strings of `parts` after the first two strings (there will be `valueCount`
     * of these, 1 in our case).
     *
     *  1.
     * If `parts[0]` is equal to the string DIRTY ("DIRTY") and there are 2 strings in
     * `parts` we set the `currentEditor` field of `entry` to a new instance
     * of `Editor` constructed to edit `entry`.
     *
     *  1.
     * If `parts[0]` is equal to the string READ ("READ") and there are 2 strings in
     * `parts` we do nothing, since this has already been handled by our call to the
     * `get` method of `lruEntries`
     *
     *  1.
     * Otherwise we throw an IOException "unexpected journal line: "
     *
     *
     *
     * @param line Line from old journal to interpret
     * @throws IOException if an IO error occurs, or a incorrect journal line is passed us
     */
    @Throws(IOException::class)
    private fun readJournalLine(line: String) {
        val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size < 2) {
            throw IOException("unexpected journal line: $line")
        }
        val key = parts[1]
        if (parts[0] == REMOVE && parts.size == 2) {
            lruEntries.remove(key)
            return
        }
        var entry: Entry? = lruEntries[key]
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        }
        if (parts[0] == CLEAN && parts.size == 2 + valueCount) {
            entry!!.readable = true
            entry.currentEditor = null
            entry.setLengths(copyOfRange(parts, 2, parts.size))
        } else if (parts[0] == DIRTY && parts.size == 2) {
            entry!!.currentEditor = Editor(entry)
        } else if (parts[0] == READ && parts.size == 2) {
            // this work was already done by calling lruEntries.get()
        } else {
            throw IOException("unexpected journal line: $line")
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the cache. Dirty entries
     * are assumed to be inconsistent and will be deleted. First we call our `deleteIfExists`
     * to delete `journalFileTmp` if it already exists. Then for all the `Entry` objects
     * in `LinkedHashMap<String, Entry> lruEntries` we set `Entry entry` to the next
     * entry, and if:
     *
     *  *
     * the `currentEditor` field of `entry` is null, we add all of the values in
     * the `lengths[]` field of `entry` to size
     *
     *  *
     * if the `currentEditor` field of `entry` is not null, we set it to null, and
     * we delete from the file system (if they exist) all the files that might have been written
     * for this `Entry`. We do this by looping though all the `valueCount` (1 in
     * our case) `File` objects returned by both the methods `getCleanFile` and
     * `getDirtyFile` of `entry` passing them to `deleteIfExists` to delete
     * them if they exit. After this we remove the `Entry` from `lruEntries`.
     *
     *
     */
    @Throws(IOException::class)
    private fun processJournal() {
        deleteIfExists(journalFileTmp)
        val i = lruEntries.values.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry!!.currentEditor == null) {
                for (t in 0 until valueCount) {
                    size += entry.lengths[t]
                }
            } else {
                entry.currentEditor = null
                for (t in 0 until valueCount) {
                    deleteIfExists(entry.getCleanFile(t))
                    deleteIfExists(entry.getDirtyFile(t))
                }
                i.remove()
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the current journal if
     * it exists. If our field `Writer journalWriter` is not null, we call its `close`
     * method to close its file. Then we create `Writer writer` to be a `BufferedWriter`
     * that will use a `FileWriter` instance constructed to write to `File journalFileTmp`.
     * We then write the journal header to `writer` which consists of a line with the string
     * MAGIC ("libcore.io.DiskLruCache"), followed by a line with the string VERSION_1 ("1"), followed
     * by a line with the string value of `appVersion`, followed by a line with the string value
     * of `valueCount`, followed by a blank line.
     *
     *
     * We then loop through each of the `Entry entry` objects in the values of `lruEntries`
     * and if its `currentEditor` is:
     *
     *  *
     * not null: we write a line to `writer` consisting of the string DIRTY ("DIRTY")
     * followed by a blank, followed by the string value of the field `key` of this
     * `entry`.
     *
     *  *
     * null: we write a line to `writer` consisting of the string CLEAN ("CLEAN")
     * followed by a blank, followed by the string value of the field `key` of this
     * `entry`, followed by the string returned by the `getLengths` method of
     * this `entry` (this method concatenates the string value of all of the values
     * in the `lengths[]` field, each preceded by a blank character)
     *
     *
     * After doing this we close `writer`, rename `journalFileTmp` to `journalFile`,
     * and initialize our field `Writer journalWriter` to be a `BufferedWriter` writing
     * to a `FileWriter` that appends to `journalFile`.
     */
    @Synchronized
    @Throws(IOException::class)
    private fun rebuildJournal() {
        if (journalWriter != null) {
            journalWriter!!.close()
        }
        val writer: Writer = BufferedWriter(FileWriter(journalFileTmp), IO_BUFFER_SIZE)
        writer.write(MAGIC)
        writer.write("\n")
        writer.write(VERSION_1)
        writer.write("\n")
        writer.write(Integer.toString(appVersion))
        writer.write("\n")
        writer.write(Integer.toString(valueCount))
        writer.write("\n")
        writer.write("\n")
        for (entry in lruEntries.values) {
            if (entry!!.currentEditor != null) {
                writer.write("$DIRTY ${entry.key}\n")
            } else {
                writer.write("$CLEAN ${entry.key}${entry.getLengths()}\n")
            }
        }
        writer.close()
        journalFileTmp.renameTo(journalFile)
        journalWriter = BufferedWriter(FileWriter(journalFile, true), IO_BUFFER_SIZE)
    }

    /**
     * Returns a snapshot of the entry named `key`, or null if it doesn't exist or is not
     * currently readable. If a value is returned, it is automatically moved to the head of the LRU
     * queue by the `get` method of `lruEntries`.
     *
     *
     * First we call our method `checkNotClosed` to make sure the journal is open (it throws
     * IllegalStateException if it is not). Then we call our method `validateKey` to make
     * sure that `key` does not contain any illegal characters (if it contains " ", "\n" or
     * "\r" it throws IllegalArgumentException). We initialize `Entry entry` by retrieving the
     * `Entry` in `lruEntries` stored under `key`, and if it is null we return
     * null to the caller. If the `readable` field of `entry` is false we also return
     * null to the caller (the `Entry` has never been published).
     *
     *
     * We initialize `InputStream[] ins` to hold `valueCount` input streams, then wrapped
     * in a try block intended to catch FileNotFoundException we loop over `int i` for the
     * `valueCount` entries in `ins` opening the `FileInputStream` for each of
     * the `File` paths that `Entry.getCleanFile(i)` creates, and if one of them does
     * not exist FileNotFoundException gets caught by our catch block and we return null instead of
     * continuing.
     *
     *
     * We next increment our field `redundantOpCount`, and append a line to `journalWriter`
     * consisting of the string formed by concatenating the string READ followed by a blank, followed
     * by the string value of `key`. If our method `journalRebuildRequired` determines
     * that a journal rebuild would be profitable we schedule the callable `cleanupCallable`
     * to run in the background.
     *
     *
     * Finally we return a new instance of `Snapshot` constructed using `key` as the key
     * to the cached `Entry`, the `sequenceNumber` field of our `entry` as the
     * sequence number, and `ins` as the array of open `InputStream` objects to use to
     * read the objects cached from the disk.
     *
     * @param key key of the disk cache entry to create a snapshot for.
     * @return a `Snapshot` of the `Entry` with key `key`
     * @throws IOException if any IO errors occur.
     */
    @Synchronized
    @Throws(IOException::class)
    operator fun get(key: String): Snapshot? {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key] ?: return null
        if (!entry.readable) {
            return null
        }

        /*
         * Open all streams eagerly to guarantee that we see a single published
         * snapshot. If we opened streams lazily then the streams could come
         * from different edits.
         */
        val ins = arrayOfNulls<InputStream>(valueCount)
        try {
            for (i in 0 until valueCount) {
                ins[i] = FileInputStream(entry.getCleanFile(i))
            }
        } catch (e: FileNotFoundException) {
            // a file must have been deleted manually!
            return null
        }
        redundantOpCount++
        journalWriter!!.append("$READ $key\n")
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return Snapshot(key, entry.sequenceNumber, ins)
    }

    /**
     * Returns an editor for the entry named `key`, or null if another edit is in progress. We
     * just return the `Editor` returned by our the parameter version of this method when called
     * with our parameter `key` and ANY_SEQUENCE_NUMBER as the sequence number.
     *
     * @param key key of the cached `Entry`
     * @return an `Editor` instance to edit the `Entry` with key `key` for any
     * sequence number.
     * @throws IOException if there is an error writing to the journal
     */
    @Throws(IOException::class)
    fun edit(key: String): Editor? {
        return edit(key, ANY_SEQUENCE_NUMBER)
    }

    /**
     * Returns an editor for the entry named `key`, or null if another edit is in progress.
     * First we call our method `checkNotClosed` to make sure the journal is open (it throws
     * IllegalStateException if it is not). Then we call our method `validateKey` to make
     * sure that `key` does not contain any illegal characters (if it contains " ", "\n" or
     * "\r" it throws IllegalArgumentException). We initialize `Entry entry` by retrieving the
     * `Entry` in `lruEntries` stored under `key`.
     *
     *
     * If the parameter `expectedSequenceNumber` is not equal to ANY_SEQUENCE_NUMBER and either
     * `entry` is null, or its `sequenceNumber` field is not equal to our parameter
     * `expectedSequenceNumber` we return null (the snapshot is stale).
     *
     *
     * If `entry` is null, we set it to a new instance of `Entry` for the key `key`
     * and put it in our field `LinkedHashMap<String, Entry> lruEntries`. Otherwise we check
     * if its field `currentEditor` is not null and if so we return null to the caller (another
     * edit is in progress).
     *
     *
     * We initialize `Editor editor` with an instance for editing `entry`, then set the
     * `currentEditor` field of `entry` to `editor`. We write a line to our journal
     * consisting of the string DIRTY ("DIRTY") followed by a blank followed by the string value of
     * `key`. We then flush `journalWriter` to prevent file leaks, and return `editor`
     * to the caller.
     *
     * @param key key of the cached `Entry`
     * @param expectedSequenceNumber sequence number to expect our `Entry` to have, or
     * ANY_SEQUENCE_NUMBER if our caller does not care
     * @return an `Editor` instance to edit the `Entry` with key `key` with the
     * sequence number `expectedSequenceNumber`
     * @throws IOException if there is an error writing to the journal
     */
    @Synchronized
    @Throws(IOException::class)
    private fun edit(key: String, expectedSequenceNumber: Long): Editor? {
        checkNotClosed()
        validateKey(key)
        var entry = lruEntries[key]
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER
            && (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
            return null // snapshot is stale
        }
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        } else if (entry.currentEditor != null) {
            return null // another edit is in progress
        }
        val editor = Editor(entry)
        entry!!.currentEditor = editor

        // flush the journal before creating files to prevent file leaks
        journalWriter!!.write("$DIRTY $key\n")
        journalWriter!!.flush()
        return editor
    }

    /**
     * Returns the maximum number of bytes that this cache should use to store its data. We just
     * return the contents of our field `long maxSize`.
     *
     * @return the maximum number of bytes that this cache should use to store its data.
     */
    fun maxSize(): Long {
        return maxSize
    }

    /**
     * Returns the number of bytes currently being used to store the values in this cache. This may
     * be greater than the max size if a background deletion is pending. We just return the contents
     * of our field `long size`.
     *
     * @return the number of bytes currently being used to store the values in this cache.
     */
    @Synchronized
    fun size(): Long {
        return size
    }

    /**
     * Finalizes an edit. First we initialize `Entry entry` from the `entry` field of
     * our parameter `Editor editor`. If the `currentEditor` field of `entry` is
     * not equal to `editor` we throw IllegalStateException. If our parameter `success`
     * is true, and the `readable` field of `entry` is false (the entry is being created
     * for the first time) we loop over `int i` for the `valueCount` (1 in our case) files
     * for this entry aborting the edit and throwing IllegalStateException is any of the dirty files
     * returned by `entry.getDirtyFile(i)` does not exist.
     *
     *
     * Then we loop over `int i` for the `valueCount` (1 in our case) files for this entry
     * setting `File dirty` to each of the tmp files returned by `entry.getDirtyFile(i)`
     * and if our parameter `success` is:
     *
     *  *
     * true: if the file `dirty` exists we set `File clean` to the old file returned
     * by `entry.getCleanFile(i)` then rename `dirty` to `clean`. We set
     * `long oldLength` to the value stored in `entry.lengths` at `i` and set
     * `long newLength` to the length of `clean`. We then set `entry.lengths` at `i`
     * to `newLength` and update `size` by subtracting `oldLength` and adding
     * `newLength` from it.
     *
     *  *
     * false: We call our method `deleteIfExists` to delete `dirty` if it exists.
     *
     *
     * We then increment `redundantOpCount` and set the `currentEditor` field of `entry`
     * to null. If the `readable` field of `entry` is true or our parameter `success`
     * is true:
     *
     *  *
     * true: we set the `readable` field of `entry` to true, and write a line to
     * the journal consisting of the string CLEAN ("CLEAN") followed by a blank followed by
     * the string value of the `key` field of `entry` followed by the string
     * returned by `entry.getLengths()` (the string value of all the entries in the
     * `lengths[]` array preceded by a blank). If our parameter `success` is
     * true we set the `sequenceNumber` field of `entry` to `nextSequenceNumber`
     * (post incrementing `nextSequenceNumber` in the same statement).
     *
     *  *
     * both false: We remove the `key` `Entry` from `LinkedHashMap<String, Entry> lruEntries`
     * and write a line to the journal consisting of the string REMOVE ("REMOVE") followed by a blank followed
     * by the string value of the `key` field of `entry`.
     *
     *
     * If `size` is greater than `maxSize` or `journalRebuildRequired` determines
     * that a journal rebuild would be profitable, we submit a background job to `executorService`
     * which will run our `Callable<Void> cleanupCallable` and compress the journal.
     *
     * @param editor `Editor` whose results we are finalizing.
     * @param success if true the edit succeeded, if false it failed and we need to clean up.
     * @throws IOException if an IO error of any kind occurred.
     */
    @Synchronized
    @Throws(IOException::class)
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        check(entry.currentEditor == editor)

        // if this edit is creating the entry for the first time, every index must have a value
        if (success && !entry.readable) {
            for (i in 0 until valueCount) {
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort()
                    throw IllegalStateException("edit didn't create file $i")
                }
            }
        }
        for (i in 0 until valueCount) {
            val dirty = entry.getDirtyFile(i)
            if (success) {
                if (dirty.exists()) {
                    val clean = entry.getCleanFile(i)
                    dirty.renameTo(clean)
                    val oldLength = entry.lengths[i]
                    val newLength = clean.length()
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                }
            } else {
                deleteIfExists(dirty)
            }
        }
        redundantOpCount++
        entry.currentEditor = null
        if (entry.readable or success) {
            entry.readable = true
            journalWriter!!.write("$CLEAN ${entry.key}${entry.getLengths()}\n")
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++
            }
        } else {
            lruEntries.remove(entry.key)
            journalWriter!!.write("$REMOVE ${entry.key}\n")
        }
        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal and eliminate at least
     * 2000 ops. We define the constant REDUNDANT_OP_COMPACT_THRESHOLD to be 2000, and return true
     * if our field `redundantOpCount` is greater than or equal to REDUNDANT_OP_COMPACT_THRESHOLD
     * and `redundantOpCount` is greater than or equal to the size of our field `lruEntries`.
     *
     * @return true if a rebuild will be profitable.
     */
    private fun journalRebuildRequired(): Boolean {
        @Suppress("LocalVariableName")
        val REDUNDANT_OP_COMPACT_THRESHOLD = 2000
        return (redundantOpCount >= REDUNDANT_OP_COMPACT_THRESHOLD
            && redundantOpCount >= lruEntries.size)
    }

    /**
     * Drops the entry for `key` if it exists and can be removed. Entries actively being edited
     * cannot be removed. First we call our method `checkNotClosed` to make sure the journal
     * file is open (it throws IllegalStateException if it is null), then we call our method
     * `validateKey(key)` to validate our parameter `key` (it throws IllegalArgumentException
     * if it contains " ", "\n", or "\r"). We then initialize `Entry entry` by fetching the
     * `Entry` stored under `key` in our field `LinkedHashMap<String, Entry> lruEntries`.
     * If `entry` is null, or its field `currentEditor` is not null we return false to the
     * caller.
     *
     *
     * We loop over `i` for the `valueCount` (1 in our case) files in `entry` setting
     * `File file` to the file path returned by the `getCleanFile(i)` method of `entry`.
     * If we are not able to delete `file` we throw IOException, if we are we subtract the length
     * of the file contained in `lengths` at `i` from `size` and set `lengths` at `i` to 0.
     *
     *
     * We increment `redundantOpCount`, and append a line to the journal consisting of the string
     * REMOVE ("REMOVE") followed by a blank followed by the string value of the `key`. We then
     * remove the `key` `Entry` from `LinkedHashMap<String, Entry> lruEntries`. If
     * our method `journalRebuildRequired` determines that a journal rebuild would be profitable,
     * we submit a background job to `executorService` which will run our `cleanupCallable`
     * and compress the journal. Finally we return true to the caller.
     *
     * @param key Key of the `Entry` to be removed.
     * @return true if the `Entry` was successfully removed
     * @throws IOException if an IO error occurred.
     */
    @Synchronized
    @Throws(IOException::class)
    fun remove(key: String): Boolean {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key]
        if (entry == null || entry.currentEditor != null) {
            return false
        }
        for (i in 0 until valueCount) {
            val file = entry.getCleanFile(i)
            if (!file.delete()) {
                throw IOException("failed to delete $file")
            }
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }
        redundantOpCount++
        journalWriter!!.append("$REMOVE $key\n")
        lruEntries.remove(key)
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return true
    }

    /**
     * Returns true if this cache has been closed.
     *
     * @return true if this cache has been closed.
     */
    val isClosed: Boolean
        get() = journalWriter == null

    /**
     * Throws IllegalStateException if the journal file is not open.
     */
    private fun checkNotClosed() {
        checkNotNull(journalWriter) { "cache is closed" }
    }

    /**
     * Force buffered operations to the filesystem. First we call our method `checkNotClosed`
     * to make sure the journal file is open (it throws IllegalStateException if it is null). Then
     * we call our method `trimToSize` which removes the least recently used `Entry`
     * objects from our cache until `size` is less than or equal to `maxSize`. Finally
     * we call the `flush` method of `Writer journalWriter` to flush all the buffers
     * to disk.
     *
     * @throws IOException If an I/O error occurs
     */
    @Synchronized
    @Throws(IOException::class)
    fun flush() {
        checkNotClosed()
        trimToSize()
        journalWriter!!.flush()
    }

    /**
     * Closes this cache. Stored values will remain on the filesystem. If `journalWriter` is
     * null we return having done nothing. Then we loop through the `Entry entry` objects in
     * `lruEntries.values()` and if the `currentEditor` field of `entry` is not
     * null we call its `abort` method to abort the edit.
     *
     *
     * We call our method `trimToSize` which removes the least recently used `Entry`
     * objects from our cache until `size` is less than or equal to `maxSize`. We call
     * the `close` method of `journalWriter` to close the file, and set it to null.
     *
     * @throws IOException If an I/O error occurs
     */
    @Synchronized
    @Throws(IOException::class)
    override fun close() {
        if (journalWriter == null) {
            return  // already closed
        }
        for (entry in ArrayList(lruEntries.values)) {
            if (entry!!.currentEditor != null) {
                entry.currentEditor!!.abort()
            }
        }
        trimToSize()
        journalWriter!!.close()
        journalWriter = null
    }

    /**
     * We loop as long as `size` is greater than `maxSize` fetching key value pairs from
     * each of the entries in `lruEntries`, and calling our method `remove` on the key
     * of the key value pair to remove the `Entry` from the cache.
     *
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    private fun trimToSize() {
        while (size > maxSize) {
//            Map.Entry<String, Entry> toEvict = lruEntries.eldest();
            val (key) = lruEntries.entries.iterator().next()
            remove(key)
        }
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete all files in the cache
     * directory including files that weren't created by the cache. We call our method `close`
     * to close our cache, then we call our method `deleteContents` to recursively delete
     * everything in the cache directory `File directory`.
     *
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    fun delete() {
        close()
        deleteContents(directory)
    }

    /**
     * Throws IllegalArgumentException if our parameter `key` contains the characters " ", "\n",
     * or "\r".
     *
     * @param key cache key string
     */
    private fun validateKey(key: String) {
        require(!(key.contains(" ") || key.contains("\n") || key.contains("\r"))) { "keys must not contain spaces or newlines: \"$key\"" }
    }

    /**
     * A snapshot of the values for an entry.
     */
    inner class Snapshot
    /**
     * Our constructor. We just save our parameters in our fields of the same name.
     *
     * @param key Key of the cached `Entry` we are a snapshot of.
     * @param sequenceNumber Sequence number of this snapshot
     * @param ins `InputStream[]` array for the files contained in the `Entry`
     */(
        /**
         * Key of the cached `Entry` we are a snapshot of.
         */
        private val key: String,
        /**
         * Sequence number of this snapshot, used to detect a stale snapshot.
         */
        private val sequenceNumber: Long,
        /**
         * The `InputStream[]` array for the files contained in the `Entry`
         */
        private val ins: Array<InputStream?>) : Closeable {
        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress. We just return the Editor instance to edit the Entry
         * with our field `key` as the key, and the sequence number the
         * same as our field `sequenceNumber`.
         *
         * @return an `Editor` for this snapshot's entry
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun edit(): Editor? {
            return this@DiskLruCache.edit(key, sequenceNumber)
        }

        /**
         * Returns the unbuffered stream with the value for `index`.
         *
         * @param index index of the `InputStream` we want
         * @return input stream for file `index`
         */
        fun getInputStream(index: Int): InputStream? {
            return ins[index]
        }

        /**
         * Returns the string value for `index` by returning the string returned by our method
         * `inputStreamToString` when reading the input stream for file `index`.
         *
         * @param index index of the `InputStream` we want to read from
         * @return String containing the entire remaining contents of InputStream
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun getString(index: Int): String {
            return inputStreamToString(getInputStream(index))
        }

        /**
         * Closes quietly all of the `InputStream` objects contained in our field
         * `InputStream[] ins`
         */
        override fun close() {
            for (inputStream in ins) {
                closeQuietly(inputStream)
            }
        }
    }

    /**
     * Edits the values for an entry.
     */
    inner class Editor
    /**
     * Our constructor, just saves our parameter `Entry entry` in our field of the same
     * name.
     *
     * @param entry `Entry` we are to edit
     */(
        /**
         * `Entry` we are editing.
         */
        val entry: Entry) {
        /**
         * True if an IOException has been caught while doing IO.
         */
        private var hasErrors = false

        /**
         * Returns an unbuffered input stream to read the last committed value, or null if no value
         * has been committed. We synchronize on `DiskLruCache.this`, and if the value of the
         * `currentEditor` field of our field `Entry entry` is not this we throw
         * IllegalStateException. If the `readable` field of `entry` is false we return
         * null (no values have been committed to disk for this `Entry`). Finally we return
         * a `FileInputStream` constructed to read the file with the path returned by the
         * `getCleanFile` method of `entry` for the file index `index`.
         *
         * @param index file number whose `InputStream` we are to create
         * @return an unbuffered input stream to read from the file with index `index`
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun newInputStream(index: Int): InputStream? {
            synchronized(this@DiskLruCache) {
                check(entry.currentEditor == this)
                return if (!entry.readable) {
                    null
                } else FileInputStream(entry.getCleanFile(index))
            }
        }

        /**
         * Returns the last committed value as a string, or null if no value has been committed. We
         * create `InputStream inputStream` to read from the file with index `index`, and if that
         * is not `null` we return the string returned by our method `inputStreamToString` when
         * reading from `inputStream`. If `inputStream` is `null` we return `null` to the caller.
         *
         * @param index file number of file we are to read from
         * @return the entire contents of the file as a string
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun getString(index: Int): String? {
            val inputStream = newInputStream(index)
            return if (inputStream != null) inputStreamToString(inputStream) else null
        }

        /**
         * Returns a new unbuffered output stream to write the value at `index`. If the
         * underlying output stream encounters errors when writing to the filesystem, this edit
         * will be aborted when [.commit] is called. The returned output stream does not
         * throw IOExceptions. We synchronize on `DiskLruCache.this`, and if the value of
         * the `currentEditor` field of our field `Entry entry` is not this we throw
         * IllegalStateException. Finally we return an instance of `FaultHidingOutputStream`
         * constructed to write to the `FileOutputStream` created to write to the temporary
         * file pathname returned by the `getDirtyFile` method of `entry` for index
         * `index`.
         *
         * @param index index of the file to open for writing.
         * @return unbuffered output stream to write to the file with index `index`
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun newOutputStream(index: Int): OutputStream {
            synchronized(this@DiskLruCache) {
                check(entry.currentEditor == this)
                return FaultHidingOutputStream(FileOutputStream(entry.getDirtyFile(index)))
            }
        }

        /**
         * Sets the value at `index` to `value`. First we initialize `Writer writer`
         * to null, then wrapped in a try block whose finally block calls our method `closeQuietly`
         * to close `writer` we set `writer` to an `OutputStreamWriter` constructed
         * to write the the unbuffered output stream created to write to the path for the file with
         * index `index`, then write our parameter `String value` to it.
         *
         * @param index index of the file to write to
         * @param value string to write to the file
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        operator fun set(index: Int, value: String?) {
            var writer: Writer? = null
            try {
                writer = OutputStreamWriter(newOutputStream(index), UTF_8)
                writer.write(value)
            } finally {
                closeQuietly(writer)
            }
        }

        /**
         * Commits this edit so it is visible to readers. This releases the edit lock so another
         * edit may be started on the same key. If our flag `hasErrors` is true (an IOException
         * occurred during the edit) we call our method `completeEdit` to finalize this edit
         * by deleting our temp files (if they exist), then we remove the `Entry` whose key
         * is given in the `key` field of `entry` from our cache.
         *
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun commit() {
            if (hasErrors) {
                completeEdit(this, false)
                remove(entry.key) // the previous entry is stale
            } else {
                completeEdit(this, true)
            }
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be started on the same
         * key. We just call our method `completeEdit` to finalize this edit by deleting our
         * temp files (if they exist).
         *
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun abort() {
            completeEdit(this, false)
        }

        /**
         * A `FilterOutputStream` which catches `IOException` and just sets the flag
         * `boolean hasErrors` to true.
         */
        private inner class FaultHidingOutputStream
        /**
         * Our constructor, we just call our super's constructor.
         *
         * @param out underlying output stream
         */(out: OutputStream) : FilterOutputStream(out) {
            /**
             * Writes the specified byte to this output stream. Wrapped in a try block intended to
             * catch IOException we just call the `write` method of the underlying output
             * stream passed to our constructor. If we catch IOException we just set `hasErrors`
             * to true.
             *
             * @param oneByte the byte.
             */
            override fun write(oneByte: Int) {
                try {
                    out.write(oneByte)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            /**
             * Writes `len` bytes from the specified `byte` array starting at
             * offset `off` to this output stream. Wrapped in a try block intended to
             * catch IOException we just call the `write` method of the underlying output
             * stream passed to our constructor. If we catch IOException we just set `hasErrors`
             * to true.
             *
             * @param buffer the data.
             * @param offset the start offset in the data.
             * @param length the number of bytes to write.
             */
            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                try {
                    out.write(buffer, offset, length)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            /**
             * Closes this output stream and releases any system resources associated with the stream.
             * Wrapped in a try block intended to catch IOException we just call the `close`
             * method of the underlying output  stream passed to our constructor. If we catch IOException
             * we just set `hasErrors` to true.
             */
            override fun close() {
                try {
                    out.close()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            /**
             * Flushes this output stream and forces any buffered output bytes to be written out to
             * the stream. Wrapped in a try block intended to catch IOException we just call the
             * `flush` method of the underlying output  stream passed to our constructor. If
             * we catch IOException we just set `hasErrors` to true.
             */
            override fun flush() {
                try {
                    out.flush()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }
        }
    }

    /**
     * Object controlling a cache entry that is on the disk (or headed to disk(
     */
    inner class Entry(
        /** Key for referencing this `Entry` and creating file names  */
        val key: String) {
        /** Lengths of this entry's files.  */
        val lengths: LongArray

        /** True if this entry has ever been published  */
        var readable: Boolean = false

        /** The ongoing edit or null if this entry is not being edited.  */
        var currentEditor: Editor? = null

        /** The sequence number of the most recently committed edit to this entry.  */
        var sequenceNumber: Long = 0

        /**
         * Our constructor. First we save our parameter `String key` in our field of the same
         * name. Then we initialize our field `long[] lengths` with an array that can hold
         * `valueCount` values.
         *
         * param key Key for referencing this `Entry`
         */
        init {
            lengths = LongArray(valueCount)
        }

        /**
         * Concatenates the string value of all of the values in the `lengths[]` field, each
         * preceded by a blank character and returns that string to the caller. First we initialize
         * `StringBuilder result` with a new instance. Then we loop through all of the
         * `long size` values in `lengths[]` appending a blank to `result` followed
         * by the string value of `size`. Finally we return the string value of `result`
         * to the caller.
         *
         * @return string composed of the concatenated string values of our field `lengths[]`
         * each preceded by a blank.
         */
        fun getLengths(): String {
            val result = StringBuilder()
            for (size in lengths) {
                result.append(' ').append(size)
            }
            return result.toString()
        }

        /**
         * Set our `lengths[]` field by treating our `String[] strings` parameter as
         * decimal numbers like "10123". First we make sure that the length of the our parameter
         * `String[] strings` is equal to `valueCount`, throwing the IOException returned
         * by our method `invalidLengths(strings)` if they are unequal. Then wrapped in a try
         * block intended to catch NumberFormatException in order to throw the IOException returned
         * by our method `invalidLengths(strings)` instead, we loop over `int i` for all
         * of the strings in our parameter `String[] strings` setting `lengths` at `i` to
         * the long value parsed from `strings` at `i`.
         *
         * @param strings array of strings to convert to `long` values to set our
         * `lengths[]` array entries to.
         * @throws IOException if the length of the `strings` array is not equal to `valueCount`
         * or there is a non-decimal character in one of the strings
         */
        @Throws(IOException::class)
        fun setLengths(strings: Array<String>) {
            if (strings.size != valueCount) {
                throw invalidLengths(strings)
            }
            try {
                for (i in strings.indices) {
                    lengths[i] = strings[i].toLong()
                }
            } catch (e: NumberFormatException) {
                throw invalidLengths(strings)
            }
        }

        /**
         * Throws an IOException constructed with a detail message consisting of the string
         * "unexpected journal line: " followed by the string formed by the parameter
         * `strings`.
         *
         * @param strings array of strings that `setLengths` was called from
         * @return an IOException constructed with a detail message consisting of the string
         * "unexpected journal line: " followed by the string formed by the parameter `strings`.
         * @throws IOException always
         */
        @Throws(IOException::class)
        private fun invalidLengths(strings: Array<String>): IOException {
            throw IOException("unexpected journal line: " + Arrays.toString(strings))
        }

        /**
         * Creates and returns a `File` object created for the directory `directory` and
         * filename formed by concatenating the string `key` followed by a ".", followed by
         * the string value of our parameter `i`.
         *
         * @param i index of the file whose path we are to generate
         * @return path to the file with index `i`.
         */
        fun getCleanFile(i: Int): File {
            return File(directory, "$key.$i")
        }

        /**
         * Creates and returns a `File` object created for the directory `directory` and
         * filename formed by concatenating the string `key` followed by a ".", followed by
         * the string value of our parameter `i`, followed by the string ".tmp". This is the
         * temp file for the `File` created when calling `getCleanFile`.
         *
         * @param i index of the temp file whose path we are to generate
         * @return path to the temp file with index `i`.
         */
        fun getDirtyFile(i: Int): File {
            return File(directory, "$key.$i.tmp")
        }
    }

    companion object {
        /**
         * Name of the journal file.
         */
        const val JOURNAL_FILE: String = "journal"

        /**
         * Name of temporary file used to rebuilt the journal file.
         */
        const val JOURNAL_FILE_TMP: String = "journal.tmp"

        /**
         * Magic string written as first line of journal file, used to verify file integrity
         */
        const val MAGIC: String = "libcore.io.DiskLruCache"

        /**
         * Version number of cache, written as second line of journal file, used to verify file integrity
         */
        const val VERSION_1: String = "1"

        /**
         * Sequence number which stands for any number
         */
        const val ANY_SEQUENCE_NUMBER: Long = -1

        /**
         * Journal String for a CLEAN cache entry that has been successfully published and may be read.
         */
        private const val CLEAN = "CLEAN"

        /**
         * Journal String for a DIRTY cache entry that is actively being created or updated.
         */
        private const val DIRTY = "DIRTY"

        /**
         * Journal String for a REMOVE cache entry that have been deleted
         */
        private const val REMOVE = "REMOVE"

        /**
         * Journal String for a READ cache entry that tracks accesses for LRU.
         */
        private const val READ = "READ"

        /**
         * Character set used to read an write last committed value? (may not be used)
         */
        private val UTF_8 = Charset.forName("UTF-8")

        /**
         * Buffer size used to read and write to journal.
         */
        private const val IO_BUFFER_SIZE = 8 * 1024
        /* From java.util.Arrays */
        /**
         * Copies the specified range of the specified array into a new array. The initial index of the
         * range (`start`) must lie between zero and `original.length`, inclusive. The value
         * at `original[start]` is placed into the initial element of the copy (unless `start`
         * is equal to `original.length` or `start` is equal to `end`). Values from
         * subsequent elements in the original array are placed into subsequent elements in the copy.
         * The final index of the range (`end`), which must be greater than or equal to `start`,
         * may be greater than `original.length`, in which case null is placed in all elements of
         * the copy whose index is greater than or equal to `original.length` minus `start`.
         * The length of the returned array will be `end-start`.
         *
         *
         * The resulting array is of exactly the same class as the original array.
         *
         *
         * First we initialize `originalLength` with the length of our parameter `original`.
         * If `start` is greater than `end` we throw IllegalArgumentException. If `start`
         * is less than 0, or `start` is greater than `originalLength` we throw
         * ArrayIndexOutOfBoundsException. We initialize `resultLength` to the number of objects
         * the copy will hold: `end` minus `start`. We initialize `copyLength` to the
         * number of objects that need to be copied: the minimum of `resultLength` (the number of
         * objects the copy will hold) and `originalLength` minus `start` (the number of
         * objects in the original array that are requested to be copied). We then create a new instance
         * for `T[] result` with a component type that is the same as `original`, and whose
         * length is `resultLength`. We then use the `System.arraycopy` method to copy from
         * the source array `original`, starting at position `start` into the destination
         * array `result` starting at position 0 in the destination data, with `copyLength`
         * the number of objects copied. Finally we return `result` to the caller.
         *
         * @param original array from which a range is to be copied
         * @param start initial index of the range to be copied, inclusive
         * @param end final index of the range to be copied, exclusive. (This index may lie outside the array.)
         * @param <T> type of objects in array
         * @return a new array containing the specified range from the original array, truncated or padded
         * with nulls to obtain the required length
        </T> */
        private fun <T> copyOfRange(original: Array<T>, start: Int, end: Int): Array<T> {
            val originalLength = original.size // For exception priority compatibility.
            require(start <= end)
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            if (start < 0 || start > originalLength) {
                throw ArrayIndexOutOfBoundsException()
            }
            val resultLength = end - start
            val copyLength = Math.min(resultLength, originalLength - start)

            val result = java.lang.reflect.Array
                .newInstance(original.javaClass.componentType!!, resultLength) as Array<T>
            System.arraycopy(original, start, result, 0, copyLength)
            return result
        }

        /**
         * Returns the remainder of 'reader' as a string, closing it when done. We initialize our variable
         * `StringWriter writer`, `char[] buffer` with a char array that can hold 1024 chars,
         * and declare `int count`. Then we set `count` to the number of characters read from
         * `reader` into `buffer`, and while that is not equal to -1 (`read` returns -1
         * when the end of the stream has been reached), we call the `write` method of `writer`
         * to write `count` characters of `buffer` to it. When all of `reader` has been
         * read, we return the string representation of `writer` to the caller.
         *
         * @param reader `Reader` we are to read from (an `InputStreamReader` in our case
         * @return String containing the remaining data in our `Reader` input.
         * @throws IOException If an I/O error occurs
         */
        @Throws(IOException::class)
        fun readFully(reader: Reader): String {
            return reader.use { readerLocal ->
                val writer = StringWriter()
                val buffer = CharArray(1024)
                var count: Int
                while (readerLocal.read(buffer).also { count = it } != -1) {
                    writer.write(buffer, 0, count)
                }
                writer.toString()
            }
        }

        /**
         * Returns the ASCII characters up to but not including the next "\r\n", or "\n". First we create
         * `StringBuilder result` with an initial capacity of 80. Then we loop reading the next
         * character from `inputStream` into `int c`. If `c` is equal to -1 (end of the stream)
         * we throw EOFException, and if `c` is equal to '\n' we break out of the loop. For all
         * other characters we append the char version of `c` to `result`. After we reach a
         * '\n' character and break we set `int length` to the length of `result` and if
         * `length` is greater than 0, and the last character in `result` is an '\r' we set
         * the length of `result` to one less in order to drop the '\r'. Finally we return the
         * string version of `result` to the caller.
         *
         * @param inputStream [InputStream] to read from
         * @return string consisting of the next line not including the next "\r\n", or "\n".
         * @throws IOException if the stream is exhausted before the next newline
         * character.
         */
        @Throws(IOException::class)
        fun readAsciiLine(inputStream: InputStream): String {
            // TODO: support UTF-8 here instead
            val result = StringBuilder(80)
            while (true) {
                val c = inputStream.read()
                if (c == -1) {
                    throw EOFException()
                } else if (c == '\n'.code) {
                    break
                }
                result.append(c.toChar())
            }
            val length = result.length
            if (length > 0 && result[length - 1] == '\r') {
                result.setLength(length - 1)
            }
            return result.toString()
        }

        /**
         * Closes 'closeable', ignoring any checked exceptions. Does nothing if 'closeable' is null. We
         * make sure our parameter `Closeable closeable` is not null before calling its `close`
         * method wrapped in a try block which catches RuntimeException and rethrows it, and catches
         * all other exceptions and ignores them.
         *
         * @param closeable closable input or output stream to be closed
         */
        fun closeQuietly(closeable: Closeable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (rethrown: RuntimeException) {
                    throw rethrown
                } catch (ignored: Exception) {
                }
            }
        }

        /**
         * Recursively delete everything in `dir`. We initialize `File[] files` by reading
         * the path names in `File dir`. If `files` is null `dir` was not a directory
         * so we throw IllegalArgumentException. Otherwise we loop through all the `File file` in
         * `files` launching a recursive call to this method if `file` is a directory before
         * calling the `delete` method of `file` to delete it (if the `delete` method
         * returns false (delete failed) we throw an IOException.
         *
         * @param dir directory to recursively delete
         * @throws IOException if unable to delete our parameter `File dir`
         */
        @Throws(IOException::class)
        fun deleteContents(dir: File) {
            val files = dir.listFiles() ?: throw IllegalArgumentException("not a directory: $dir")
            for (file in files) {
                if (file.isDirectory) {
                    deleteContents(file)
                }
                if (!file.delete()) {
                    throw IOException("failed to delete file: $file")
                }
            }
        }

        /**
         * Opens the cache in `directory`, creating a cache if none exists there. If our parameter
         * `maxSize` is less than or equal to 0 we throw IllegalArgumentException, and if our
         * parameter `valueCount` is less than or equal to 0 we throw IllegalArgumentException.
         * We initialize `DiskLruCache cache` with a new instance using our parameters as the
         * arguments to the constructor.
         *
         *
         * If the file `cache.journalFile` exists, the cache already exists so we try to pick up
         * where we left off. Wrapped in a try block intended to catch IOException we call the
         * `readJournal` method of `cache` which reads the journal and after verifying the
         * header interprets each line of the journal in order to rebuild the `lruEntries` map of
         * cache files as it existed at the last run. We then call the `processJournal` method of
         * `cache` to compute the initial size and collect garbage. We then initialize the field
         * `cache.journalWriter` with a `BufferedWriter` which uses a `FileWriter`
         * constructed from `cache.journalFile` in append mode. Then we return `cache` to
         * the caller. If we catch an IOException we call the `delete` method of `cache` and
         * pretend the cache did not exist yet.
         *
         *
         * If `cache.journalFile` does not exist (or we decided to delete the cache above) we call
         * the `mkdirs` of `directory` to create the directory in the file system, set
         * `cache` to yet another new instance of `DiskLruCache` using our parameters as the
         * arguments to the constructor, call its `rebuildJournal` method to create a new journal,
         * and then return `cache` to the caller.
         *
         * @param directory a writable directory
         * @param appVersion version of the app
         * @param valueCount the number of values per cache entry. Must be positive.
         * @param maxSize the maximum number of bytes this cache should use to store
         * @throws java.io.IOException if reading or writing the cache directory fails
         */
        @Throws(IOException::class)
        fun open(directory: File, appVersion: Int, valueCount: Int, maxSize: Long): DiskLruCache {
            require(maxSize > 0) { "maxSize <= 0" }
            require(valueCount > 0) { "valueCount <= 0" }

            // prefer to pick up where we left off
            var cache = DiskLruCache(directory, appVersion, valueCount, maxSize)
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal()
                    cache.processJournal()
                    cache.journalWriter = BufferedWriter(
                        FileWriter(cache.journalFile, true),
                        IO_BUFFER_SIZE)
                    return cache
                } catch (journalIsCorrupt: IOException) {
//                System.logW("DiskLruCache " + directory + " is corrupt: "
//                        + journalIsCorrupt.getMessage() + ", removing");
                    cache.delete()
                }
            }

            // create a new empty cache
            directory.mkdirs()
            cache = DiskLruCache(directory, appVersion, valueCount, maxSize)
            cache.rebuildJournal()
            return cache
        }

        /**
         * If our parameter `File file` exists we try to delete it, and if that fails we throw
         * IOException.
         *
         * @param file `File` to delete if it exists
         * @throws IOException if delete fails
         */
        @Throws(IOException::class)
        private fun deleteIfExists(file: File) {
//        try {
//            Libcore.os.remove(file.getPath());
//        } catch (ErrnoException errnoException) {
//            if (errnoException.errno != OsConstants.ENOENT) {
//                throw errnoException.rethrowAsIOException();
//            }
//        }
            if (file.exists() && !file.delete()) {
                throw IOException()
            }
        }

        /**
         * Reads the entire remaining contents of the parameter `InputStream inputStream` into a string and
         * returns it to the caller. We create an `InputStreamReader` to read from [inputStream] using
         * the UTF_8 charset, and return the string returned by our method `readFully` when called
         * with it.
         *
         * @param inputStream [InputStream] to read from
         * @return the entire contents of `InputStream in` as a string
         * @throws IOException If an I/O error occurs.
         */
        @Throws(IOException::class)
        private fun inputStreamToString(inputStream: InputStream?): String {
            return readFully(InputStreamReader(inputStream, UTF_8))
        }
    }
}