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

package com.example.android.displayingbitmaps.util;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 ******************************************************************************
 * Taken from the JB source code, can be found in:
 * libcore/luni/src/main/java/libcore/io/DiskLruCache.java
 * or direct link:
 * https://android.googlesource.com/platform/libcore/+/android-4.1.1_r1/luni/src/main/java/libcore/io/DiskLruCache.java
 ******************************************************************************
 * <p>
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Values are byte
 * sequences, accessible as streams or files. Each value must be between {@code
 * 0} and {@code Integer.MAX_VALUE} bytes in length.
 * <p>
 * The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 * <p>
 * This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 * <p>
 * Clients call {@link #edit} to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then {@link #edit} will return null.
 * <ul>
 *     <li>When an entry is being <strong>created</strong> it is necessary to
 *         supply a full set of values; the empty value should be used as a
 *         placeholder if necessary.
 *     <li>When an entry is being <strong>edited</strong>, it is not necessary
 *         to supply data for every value; values default to their previous
 *         value.
 * </ul>
 * Every {@link #edit} call must be matched by a call to {@link Editor#commit}
 * or {@link Editor#abort}. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 * <p>
 * Clients call {@link #get} to read a snapshot of an entry. The read will
 * observe the value at the time that {@link #get} was called. Updates and
 * removals after the call do not impact ongoing reads.
 * <p>
 * This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching {@code IOException} and
 * responding appropriately.
 */
@SuppressWarnings({"WeakerAccess"})
public final class DiskLruCache implements Closeable {
    /**
     * Name of the journal file.
     */
    static final String JOURNAL_FILE = "journal";
    /**
     * Name of temporary file used to rebuilt the journal file.
     */
    static final String JOURNAL_FILE_TMP = "journal.tmp";
    /**
     * Magic string written as first line of journal file, used to verify file integrity
     */
    static final String MAGIC = "libcore.io.DiskLruCache";
    /**
     * Version number of cache, written as second line of journal file, used to verify file integrity
     */
    static final String VERSION_1 = "1";
    /**
     * Sequence number which stands for any number
     */
    static final long ANY_SEQUENCE_NUMBER = -1;
    /**
     * Journal String for a CLEAN cache entry that has been successfully published and may be read.
     */
    private static final String CLEAN = "CLEAN";
    /**
     * Journal String for a DIRTY cache entry that is actively being created or updated.
     */
    private static final String DIRTY = "DIRTY";
    /**
     * Journal String for a REMOVE cache entry that have been deleted
     */
    private static final String REMOVE = "REMOVE";
    /**
     * Journal String for a READ cache entry that tracks accesses for LRU.
     */
    private static final String READ = "READ";

    /**
     * Character set used to read an write last committed value? (may not be used)
     */
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    /**
     * Buffer size used to read and write to journal.
     */
    private static final int IO_BUFFER_SIZE = 8 * 1024;

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
     * Directory used for writing our files to, set by our constructor.
     */
    private final File directory;
    /**
     * File we write our journal to (and read back when restarting) named: JOURNAL_FILE = "journal"
     */
    private final File journalFile;
    /**
     * Temporary file to use when compacting journal. named: JOURNAL_FILE_TMP = "journal.tmp"
     */
    private final File journalFileTmp;
    /**
     * Version of app using us, set by our constructor and written to journal to verify on restart.
     */
    private final int appVersion;
    /**
     * Maximum size of all the files in cache, set in constructor.
     */
    private final long maxSize;
    /**
     * The number of values per cache entry (always 1 in our case)
     */
    private final int valueCount;
    /**
     * Number of bytes currently being used to store the values in this cache. This may be greater
     * than the max size if a background deletion is pending.
     */
    private long size = 0;
    /**
     * {@code BufferedWriter} being used to write to journal.
     */
    private Writer journalWriter;
    /**
     * Hash of {@code Entry} objects pointing to disk cache keyed by a string generated by the
     * client. It is a linked hash map whose order of iteration is the order in which its entries
     * were last accessed, from least-recently accessed to most-recently (access-order).
     * This kind of map is well-suited to building LRU caches.
     */
    private final LinkedHashMap<String, Entry> lruEntries
            = new LinkedHashMap<>(0, 0.75f, true);
    /**
     * Count of number of operations committed to journal, used to determine whether a journal
     * rebuild will be profitable.
     */
    private int redundantOpCount;

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private long nextSequenceNumber = 0;

    /* From java.util.Arrays */

    /**
     * Copies the specified range of the specified array into a new array. The initial index of the
     * range ({@code start}) must lie between zero and {@code original.length}, inclusive. The value
     * at {@code original[start]} is placed into the initial element of the copy (unless {@code start}
     * is equal to {@code original.length} or {@code start} is equal to {@code end}). Values from
     * subsequent elements in the original array are placed into subsequent elements in the copy.
     * The final index of the range ({@code end}), which must be greater than or equal to {@code start},
     * may be greater than {@code original.length}, in which case null is placed in all elements of
     * the copy whose index is greater than or equal to {@code original.length} minus {@code start}.
     * The length of the returned array will be {@code end-start}.
     * <p>
     * The resulting array is of exactly the same class as the original array.
     * <p>
     * First we initialize {@code originalLength} with the length of our parameter {@code original}.
     * If {@code start} is greater than {@code end} we throw IllegalArgumentException. If {@code start}
     * is less than 0, or {@code start} is greater than {@code originalLength} we throw
     * ArrayIndexOutOfBoundsException. We initialize {@code resultLength} to the number of objects
     * the copy will hold: {@code end} minus {@code start}. We initialize {@code copyLength} to the
     * number of objects that need to be copied: the minimum of {@code resultLength} (the number of
     * objects the copy will hold) and {@code originalLength} minus {@code start} (the number of
     * objects in the original array that are requested to be copied). We then create a new instance
     * for {@code T[] result} with a component type that is the same as {@code original}, and whose
     * length is {@code resultLength}. We then use the {@code System.arraycopy} method to copy from
     * the source array {@code original}, starting at position {@code start} into the destination
     * array {@code result} starting at position 0 in the destination data, with {@code copyLength}
     * the number of objects copied. Finally we return {@code result} to the caller.
     *
     * @param original array from which a range is to be copied
     * @param start initial index of the range to be copied, inclusive
     * @param end final index of the range to be copied, exclusive. (This index may lie outside the array.)
     * @param <T> type of objects in array
     * @return a new array containing the specified range from the original array, truncated or padded
     * with nulls to obtain the required length
     */
    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <T> T[] copyOfRange(T[] original, int start, int end) {
        final int originalLength = original.length; // For exception priority compatibility.
        if (start > end) {
            throw new IllegalArgumentException();
        }
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        final int resultLength = end - start;
        final int copyLength = Math.min(resultLength, originalLength - start);
        final T[] result = (T[]) Array
                .newInstance(original.getClass().getComponentType(), resultLength);
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    /**
     * Returns the remainder of 'reader' as a string, closing it when done. We initialize our variable
     * {@code StringWriter writer}, {@code char[] buffer} with a char array that can hold 1024 chars,
     * and declare {@code int count}. Then we set {@code count} to the number of characters read from
     * {@code reader} into {@code buffer}, and while that is not equal to -1 ({@code read} returns -1
     * when the end of the stream has been reached), we call the {@code write} method of {@code writer}
     * to write {@code count} characters of {@code buffer} to it. When all of {@code reader} has been
     * read, we return the string representation of {@code writer} to the caller.
     *
     * @param reader {@code Reader} we are to read from (an {@code InputStreamReader} in our case
     * @return String containing the remaining data in our {@code Reader} input.
     * @throws IOException If an I/O error occurs
     */
    public static String readFully(Reader reader) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Returns the ASCII characters up to but not including the next "\r\n", or "\n". First we create
     * {@code StringBuilder result} with an initial capacity of 80. Then we loop reading the next
     * character from {@code in} into {@code int c}. If {@code c} is equal to -1 (end of the stream)
     * we throw EOFException, and if {@code c} is equal to '\n' we break out of the loop. For all
     * other characters we append the char version of {@code c} to {@code result}. After we reach a
     * '\n' character and break we set {@code int length} to the length of {@code result} and if
     * {@code length} is greater than 0, and the last character in {@code result} is an '\r' we set
     * the length of {@code result} to one less in order to drop the '\r'. Finally we return the
     * string version of {@code result} to the caller.
     *
     * @param in {@code InputStream} to read from
     * @return string consisting of the next line not including the next "\r\n", or "\n".
     * @throws IOException if the stream is exhausted before the next newline
     * character.
     */
    public static String readAsciiLine(InputStream in) throws IOException {
        // TODO: support UTF-8 here instead

        StringBuilder result = new StringBuilder(80);
        while (true) {
            int c = in.read();
            if (c == -1) {
                throw new EOFException();
            } else if (c == '\n') {
                break;
            }

            result.append((char) c);
        }
        int length = result.length();
        if (length > 0 && result.charAt(length - 1) == '\r') {
            result.setLength(length - 1);
        }
        return result.toString();
    }

    /**
     * Closes 'closeable', ignoring any checked exceptions. Does nothing if 'closeable' is null. We
     * make sure our parameter {@code Closeable closeable} is not null before calling its {@code close}
     * method wrapped in a try block which catches RuntimeException and rethrows it, and catches
     * all other exceptions and ignores them.
     *
     * @param closeable closable input or output stream to be closed
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Recursively delete everything in {@code dir}. We initialize {@code File[] files} by reading
     * the path names in {@code File dir}. If {@code files} is null {@code dir} was not a directory
     * so we throw IllegalArgumentException. Otherwise we loop through all the {@code File file} in
     * {@code files} launching a recursive call to this method if {@code file} is a directory before
     * calling the {@code delete} method of {@code file} to delete it (if the {@code delete} method
     * returns false (delete failed) we throw an IOException.
     *
     * @param dir directory to recursively delete
     * @throws IOException if unable to delete our parameter {@code File dir}
     */
    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("not a directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    /** This cache uses a single background thread to evict entries. */
    private final ExecutorService executorService = new ThreadPoolExecutor(0, 1,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    /**
     * Background thread used to evict entries.
     */
    private final Callable<Void> cleanupCallable = new Callable<Void>() {
        /**
         * {@code Callable} used to cleanup our cache. Synchronized on this, if {@code journalWriter}
         * is null we return null (it is already closed). Otherwise we call our method {@code trimToSize}
         * which removes cache entries until {@code size} (the number of bytes used) is less than
         * {@code maxSize} (the maximum number of bytes to use for our cache). Then if our method
         * {@code journalRebuildRequired} returns true (it determines that a rebuild of the journal
         * would be profitable) we call our method {@code rebuildJournal} to rebuild the journal, and
         * set {@code redundantOpCount} to 0. Once outside of the synchronized block we return null
         * to the caller.
         *
         * @return null
         * @throws Exception all exceptions thrown by methods we call.
         */
        @Override
        public Void call() throws Exception {
            synchronized (DiskLruCache.this) {
                if (journalWriter == null) {
                    return null; // closed
                }
                trimToSize();
                if (journalRebuildRequired()) {
                    rebuildJournal();
                    redundantOpCount = 0;
                }
            }
            return null;
        }
    };

    /**
     * Our constructor. We store our parameter {@code File directory} in our field {@code directory},
     * {@code int appVersion} in our field {@code appVersion}, initialize our field {@code File journalFile}
     * with a new file in {@code directory} with the name JOURNAL_FILE, initialize our field
     * {@code File journalFileTmp} with a new file in {@code directory} with the name JOURNAL_FILE_TMP,
     * store our parameter {@code int valueCount} in our field {@code valueCount}, and store our
     * parameter {@code int maxSize} in our field {@code maxSize}.
     *
     * @param directory a writable directory
     * @param appVersion version of the app
     * @param valueCount the number of values per cache entry. Must be positive.
     * @param maxSize the maximum number of bytes this cache should use to store
     */
    private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        this.directory = directory;
        this.appVersion = appVersion;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
    }

    /**
     * Opens the cache in {@code directory}, creating a cache if none exists there. If our parameter
     * {@code maxSize} is less than or equal to 0 we throw IllegalArgumentException, and if our
     * parameter {@code valueCount} is less than or equal to 0 we throw IllegalArgumentException.
     * We initialize {@code DiskLruCache cache} with a new instance using our parameters as the
     * arguments to the constructor.
     * <p>
     * If the file {@code cache.journalFile} exists, the cache already exists so we try to pick up
     * where we left off. Wrapped in a try block intended to catch IOException we call the
     * {@code readJournal} method of {@code cache} which reads the journal and after verifying the
     * header interprets each line of the journal in order to rebuild the {@code lruEntries} map of
     * cache files as it existed at the last run. We then call the {@code processJournal} method of
     * {@code cache} to compute the initial size and collect garbage. We then initialize the field
     * {@code cache.journalWriter} with a {@code BufferedWriter} which uses a {@code FileWriter}
     * constructed from {@code cache.journalFile} in append mode. Then we return {@code cache} to
     * the caller. If we catch an IOException we call the {@code delete} method of {@code cache} and
     * pretend the cache did not exist yet.
     * <p>
     * If {@code cache.journalFile} does not exist (or we decided to delete the cache above) we call
     * the {@code mkdirs} of {@code directory} to create the directory in the file system, set
     * {@code cache} to yet another new instance of {@code DiskLruCache} using our parameters as the
     * arguments to the constructor, call its {@code rebuildJournal} method to create a new journal,
     * and then return {@code cache} to the caller.
     *
     * @param directory a writable directory
     * @param appVersion version of the app
     * @param valueCount the number of values per cache entry. Must be positive.
     * @param maxSize the maximum number of bytes this cache should use to store
     * @throws java.io.IOException if reading or writing the cache directory fails
     */
    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
            throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount <= 0");
        }

        // prefer to pick up where we left off
        DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        if (cache.journalFile.exists()) {
            try {
                cache.readJournal();
                cache.processJournal();
                cache.journalWriter = new BufferedWriter(
                        new FileWriter(cache.journalFile, true),
                        IO_BUFFER_SIZE);
                return cache;
            } catch (IOException journalIsCorrupt) {
//                System.logW("DiskLruCache " + directory + " is corrupt: "
//                        + journalIsCorrupt.getMessage() + ", removing");
                cache.delete();
            }
        }

        // create a new empty cache
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
        cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        cache.rebuildJournal();
        return cache;
    }

    /**
     * Reads a pre-existing journal line by line, verifying the header first then processing the rest
     * of the journal in order to populate our field {@code LinkedHashMap<String, Entry> lruEntries}
     * with {@code Entry} objects describing the current state of our disk cache. We construct the
     * {@code BufferedInputStream} {@code InputStream in} to read from {@code File journalFile}, then
     * wrapped in a try block with a finally block that calls our {@code closeQuietly} method to close
     * {@code in} we read the first five lines of {@code in} into the variables {@code String magic},
     * {@code String version}, {@code String appVersionString}, {@code String valueCountString} and
     * {@code String blank}. We then throw an IOException if any of these strings do not match what
     * we would write as the header to a journal file.
     * <p>
     * We then loop forever executing our method {@code readJournalLine} on the string read by our
     * {@code readAsciiLine} from {@code in}. {@code readJournalLine} processes each line and updates
     * the contents of our field {@code LinkedHashMap<String, Entry> lruEntries} based on the contents
     * of the journal line. We break out of this infinite loop when we catch EOFException.
     *
     * @throws IOException if an IO error of any sort occurs.
     */
    private void readJournal() throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(journalFile), IO_BUFFER_SIZE);
        try {
            String magic = readAsciiLine(in);
            String version = readAsciiLine(in);
            String appVersionString = readAsciiLine(in);
            String valueCountString = readAsciiLine(in);
            String blank = readAsciiLine(in);
            if (!MAGIC.equals(magic)
                    || !VERSION_1.equals(version)
                    || !Integer.toString(appVersion).equals(appVersionString)
                    || !Integer.toString(valueCount).equals(valueCountString)
                    || !"".equals(blank)) {
                throw new IOException("unexpected journal header: ["
                        + magic + ", " + version + ", " + valueCountString + ", " + blank + "]");
            }

            while (true) {
                try {
                    readJournalLine(readAsciiLine(in));
                } catch (EOFException endOfJournal) {
                    break;
                }
            }
        } finally {
            closeQuietly(in);
        }
    }

    /**
     * Processes a single {@code String line} from an old journal and updates the contents of our field
     * {@code LinkedHashMap<String, Entry> lruEntries} based on the contents of the journal line.
     * First we initialize {@code String[] parts} by splitting our parameter {@code String line} on
     * blanks. If there are not at least two strings in {@code parts} after doing this we throw
     * IOException "unexpected journal line: ". We initialize {@code String key} to {@code parts[1]}.
     * <p>
     * If {@code parts[0]} is equal to the string REMOVE ("REMOVE") and there are 2 strings in
     * {@code parts} we remove the {@code Entry} in {@code LinkedHashMap<String, Entry> lruEntries}
     * with the key {@code key} and return.
     * <p>
     * We initialize {@code Entry entry} with the {@code Entry} in {@code lruEntries} with the key
     * {@code key}, if this is null we set {@code entry} to a new instance of {@code Entry} for
     * {@code key} and put it in {@code lruEntries} under the key {@code key}.
     * <p>
     * We now handle three different cases of the value of {@code parts[0]}:
     * <ol>
     *     <li>
     *         If {@code parts[0]} is equal to the string CLEAN ("CLEAN") and there are {@code valueCount}
     *         plus 2 strings in {@code parts}, we set the {@code readable} field of {@code entry} to true,
     *         set its {@code currentEditor} field to null, and call its {@code setLengths} method to set
     *         the values in the {@code lengths[]} array field of {@code entry} to the long values encoded
     *         in the strings of {@code parts} after the first two strings (there will be {@code valueCount}
     *         of these, 1 in our case).
     *     </li>
     *     <li>
     *         If {@code parts[0]} is equal to the string DIRTY ("DIRTY") and there are 2 strings in
     *         {@code parts} we set the {@code currentEditor} field of {@code entry} to a new instance
     *         of {@code Editor} constructed to edit {@code entry}.
     *     </li>
     *     <li>
     *         If {@code parts[0]} is equal to the string READ ("READ") and there are 2 strings in
     *         {@code parts} we do nothing, since this has already been handled by our call to the
     *         {@code get} method of {@code lruEntries}
     *     </li>
     *     <li>
     *         Otherwise we throw an IOException "unexpected journal line: "
     *     </li>
     * </ol>
     *
     * @param line Line from old journal to interpret
     * @throws IOException if an IO error occurs, or a incorrect journal line is passed us
     */
    private void readJournalLine(String line) throws IOException {
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            throw new IOException("unexpected journal line: " + line);
        }

        String key = parts[1];
        if (parts[0].equals(REMOVE) && parts.length == 2) {
            lruEntries.remove(key);
            return;
        }

        Entry entry = lruEntries.get(key);
        if (entry == null) {
            entry = new Entry(key);
            lruEntries.put(key, entry);
        }

        if (parts[0].equals(CLEAN) && parts.length == 2 + valueCount) {
            entry.readable = true;
            entry.currentEditor = null;
            entry.setLengths(copyOfRange(parts, 2, parts.length));
        } else if (parts[0].equals(DIRTY) && parts.length == 2) {
            entry.currentEditor = new Editor(entry);
        } else //noinspection StatementWithEmptyBody
            if (parts[0].equals(READ) && parts.length == 2) {
            // this work was already done by calling lruEntries.get()
        } else {
            throw new IOException("unexpected journal line: " + line);
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the cache. Dirty entries
     * are assumed to be inconsistent and will be deleted. First we call our {@code deleteIfExists}
     * to delete {@code journalFileTmp} if it already exists. Then for all the {@code Entry} objects
     * in {@code LinkedHashMap<String, Entry> lruEntries} we set {@code Entry entry} to the next
     * entry, and if:
     * <ul>
     *     <li>
     *         the {@code currentEditor} field of {@code entry} is null, we add all of the values in
     *         the {@code lengths[]} field of {@code entry} to size
     *     </li>
     *     <li>
     *         if the {@code currentEditor} field of {@code entry} is not null, we set it to null, and
     *         we delete from the file system (if they exist) all the files that might have been written
     *         for this {@code Entry}. We do this by looping though all the {@code valueCount} (1 in
     *         our case) {@code File} objects returned by both the methods {@code getCleanFile} and
     *         {@code getDirtyFile} of {@code entry} passing them to {@code deleteIfExists} to delete
     *         them if they exit. After this we remove the {@code Entry} from {@code lruEntries}.
     *     </li>
     * </ul>
     */
    private void processJournal() throws IOException {
        deleteIfExists(journalFileTmp);
        for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
            Entry entry = i.next();
            if (entry.currentEditor == null) {
                for (int t = 0; t < valueCount; t++) {
                    size += entry.lengths[t];
                }
            } else {
                entry.currentEditor = null;
                for (int t = 0; t < valueCount; t++) {
                    deleteIfExists(entry.getCleanFile(t));
                    deleteIfExists(entry.getDirtyFile(t));
                }
                i.remove();
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the current journal if
     * it exists. If our field {@code Writer journalWriter} is not null, we call its {@code close}
     * method to close its file. Then we create {@code Writer writer} to be a {@code BufferedWriter}
     * that will use a {@code FileWriter} instance constructed to write to {@code File journalFileTmp}.
     * We then write the journal header to {@code writer} which consists of a line with the string
     * MAGIC ("libcore.io.DiskLruCache"), followed by a line with the string VERSION_1 ("1"), followed
     * by a line with the string value of {@code appVersion}, followed by a line with the string value
     * of {@code valueCount}, followed by a blank line.
     * <p>
     * We then loop through each of the {@code Entry entry} objects in the values of {@code lruEntries}
     * and if its {@code currentEditor} is:
     * <ul>
     *     <li>
     *         not null: we write a line to {@code writer} consisting of the string DIRTY ("DIRTY")
     *         followed by a blank, followed by the string value of the field {@code key} of this
     *         {@code entry}.
     *     </li>
     *     <li>
     *         null: we write a line to {@code writer} consisting of the string CLEAN ("CLEAN")
     *         followed by a blank, followed by the string value of the field {@code key} of this
     *         {@code entry}, followed by the string returned by the {@code getLengths} method of
     *         this {@code entry} (this method concatenates the string value of all of the values
     *         in the {@code lengths[]} field, each preceded by a blank character)
     *     </li>
     * </ul>
     * After doing this we close {@code writer}, rename {@code journalFileTmp} to {@code journalFile},
     * and initialize our field {@code Writer journalWriter} to be a {@code BufferedWriter} writing
     * to a {@code FileWriter} that appends to {@code journalFile}.
     */
    private synchronized void rebuildJournal() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
        }

        Writer writer = new BufferedWriter(new FileWriter(journalFileTmp), IO_BUFFER_SIZE);
        writer.write(MAGIC);
        writer.write("\n");
        writer.write(VERSION_1);
        writer.write("\n");
        writer.write(Integer.toString(appVersion));
        writer.write("\n");
        writer.write(Integer.toString(valueCount));
        writer.write("\n");
        writer.write("\n");

        for (Entry entry : lruEntries.values()) {
            if (entry.currentEditor != null) {
                writer.write(DIRTY + ' ' + entry.key + '\n');
            } else {
                writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
            }
        }

        writer.close();
        //noinspection ResultOfMethodCallIgnored
        journalFileTmp.renameTo(journalFile);
        journalWriter = new BufferedWriter(new FileWriter(journalFile, true), IO_BUFFER_SIZE);
    }

    /**
     * If our parameter {@code File file} exists we try to delete it, and if that fails we throw
     * IOException.
     *
     * @param file {@code File} to delete if it exists
     * @throws IOException if delete fails
     */
    private static void deleteIfExists(File file) throws IOException {
//        try {
//            Libcore.os.remove(file.getPath());
//        } catch (ErrnoException errnoException) {
//            if (errnoException.errno != OsConstants.ENOENT) {
//                throw errnoException.rethrowAsIOException();
//            }
//        }
        if (file.exists() && !file.delete()) {
            throw new IOException();
        }
    }

    /**
     * Returns a snapshot of the entry named {@code key}, or null if it doesn't exist or is not
     * currently readable. If a value is returned, it is automatically moved to the head of the LRU
     * queue by the {@code get} method of {@code lruEntries}.
     * <p>
     * First we call our method {@code checkNotClosed} to make sure the journal is open (it throws
     * IllegalStateException if it is not). Then we call our method {@code validateKey} to make
     * sure that {@code key} does not contain any illegal characters (if it contains " ", "\n" or
     * "\r" it throws IllegalArgumentException). We initialize {@code Entry entry} by retrieving the
     * {@code Entry} in {@code lruEntries} stored under {@code key}, and if it is null we return
     * null to the caller. If the {@code readable} field of {@code entry} is false we also return
     * null to the caller (the {@code Entry} has never been published).
     * <p>
     * We initialize {@code InputStream[] ins} to hold {@code valueCount} input streams, then wrapped
     * in a try block intended to catch FileNotFoundException we loop over {@code int i} for the
     * {@code valueCount} entries in {@code ins} opening the {@code FileInputStream} for each of
     * the {@code File} paths that {@code Entry.getCleanFile(i)} creates, and if one of them does
     * not exist FileNotFoundException gets caught by our catch block and we return null instead of
     * continuing.
     * <p>
     * We next increment our field {@code redundantOpCount}, and append a line to {@code journalWriter}
     * consisting of the string formed by concatenating the string READ followed by a blank, followed
     * by the string value of {@code key}. If our method {@code journalRebuildRequired} determines
     * that a journal rebuild would be profitable we schedule the callable {@code cleanupCallable}
     * to run in the background.
     * <p>
     * Finally we return a new instance of {@code Snapshot} constructed using {@code key} as the key
     * to the cached {@code Entry}, the {@code sequenceNumber} field of our {@code entry} as the
     * sequence number, and {@code ins} as the array of open {@code InputStream} objects to use to
     * read the objects cached from the disk.
     *
     * @param key key of the disk cache entry to create a snapshot for.
     * @return a {@code Snapshot} of the {@code Entry} with key {@code key}
     * @throws IOException if any IO errors occur.
     */
    public synchronized Snapshot get(String key) throws IOException {
        checkNotClosed();
        validateKey(key);
        Entry entry = lruEntries.get(key);
        if (entry == null) {
            return null;
        }

        if (!entry.readable) {
            return null;
        }

        /*
         * Open all streams eagerly to guarantee that we see a single published
         * snapshot. If we opened streams lazily then the streams could come
         * from different edits.
         */
        InputStream[] ins = new InputStream[valueCount];
        try {
            for (int i = 0; i < valueCount; i++) {
                ins[i] = new FileInputStream(entry.getCleanFile(i));
            }
        } catch (FileNotFoundException e) {
            // a file must have been deleted manually!
            return null;
        }

        redundantOpCount++;
        //noinspection StringConcatenationInsideStringBufferAppend
        journalWriter.append(READ + ' ' + key + '\n');
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }

        return new Snapshot(key, entry.sequenceNumber, ins);
    }

    /**
     * Returns an editor for the entry named {@code key}, or null if another edit is in progress. We
     * just return the {@code Editor} returned by our the parameter version of this method when called
     * with our parameter {@code key} and ANY_SEQUENCE_NUMBER as the sequence number.
     *
     * @param key key of the cached {@code Entry}
     * @return an {@code Editor} instance to edit the {@code Entry} with key {@code key} for any
     * sequence number.
     * @throws IOException if there is an error writing to the journal
     */
    public Editor edit(String key) throws IOException {
        return edit(key, ANY_SEQUENCE_NUMBER);
    }

    /**
     * Returns an editor for the entry named {@code key}, or null if another edit is in progress.
     * First we call our method {@code checkNotClosed} to make sure the journal is open (it throws
     * IllegalStateException if it is not). Then we call our method {@code validateKey} to make
     * sure that {@code key} does not contain any illegal characters (if it contains " ", "\n" or
     * "\r" it throws IllegalArgumentException). We initialize {@code Entry entry} by retrieving the
     * {@code Entry} in {@code lruEntries} stored under {@code key}.
     * <p>
     * If the parameter {@code expectedSequenceNumber} is not equal to ANY_SEQUENCE_NUMBER and either
     * {@code entry} is null, or its {@code sequenceNumber} field is not equal to our parameter
     * {@code expectedSequenceNumber} we return null (the snapshot is stale).
     * <p>
     * If {@code entry} is null, we set it to a new instance of {@code Entry} for the key {@code key}
     * and put it in our field {@code LinkedHashMap<String, Entry> lruEntries}. Otherwise we check
     * if its field {@code currentEditor} is not null and if so we return null to the caller (another
     * edit is in progress).
     * <p>
     * We initialize {@code Editor editor} with an instance for editing {@code entry}, then set the
     * {@code currentEditor} field of {@code entry} to {@code editor}. We write a line to our journal
     * consisting of the string DIRTY ("DIRTY") followed by a blank followed by the string value of
     * {@code key}. We then flush {@code journalWriter} to prevent file leaks, and return {@code editor}
     * to the caller.
     *
     * @param key key of the cached {@code Entry}
     * @param expectedSequenceNumber sequence number to expect our {@code Entry} to have, or
     * ANY_SEQUENCE_NUMBER if our caller does not care
     * @return an {@code Editor} instance to edit the {@code Entry} with key {@code key} with the
     * sequence number {@code expectedSequenceNumber}
     * @throws IOException if there is an error writing to the journal
     */
    private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
        checkNotClosed();
        validateKey(key);
        Entry entry = lruEntries.get(key);
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER
                && (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
            return null; // snapshot is stale
        }
        if (entry == null) {
            entry = new Entry(key);
            lruEntries.put(key, entry);
        } else if (entry.currentEditor != null) {
            return null; // another edit is in progress
        }

        Editor editor = new Editor(entry);
        entry.currentEditor = editor;

        // flush the journal before creating files to prevent file leaks
        journalWriter.write(DIRTY + ' ' + key + '\n');
        journalWriter.flush();
        return editor;
    }

    /**
     * Returns the directory where this cache stores its data. We just return the value in our field
     * {@code File directory} to the caller.
     *
     * @return the directory where this cache stores its data.
     */
    @SuppressWarnings("unused")
    public File getDirectory() {
        return directory;
    }

    /**
     * Returns the maximum number of bytes that this cache should use to store its data. We just
     * return the contents of our field {@code long maxSize}.
     *
     * @return the maximum number of bytes that this cache should use to store its data.
     */
    @SuppressWarnings("unused")
    public long maxSize() {
        return maxSize;
    }

    /**
     * Returns the number of bytes currently being used to store the values in this cache. This may
     * be greater than the max size if a background deletion is pending. We just return the contents
     * of our field {@code long size}.
     *
     * @return the number of bytes currently being used to store the values in this cache.
     */
    public synchronized long size() {
        return size;
    }

    /**
     * Finalizes an edit. First we initialize {@code Entry entry} from the {@code entry} field of
     * our parameter {@code Editor editor}. If the {@code currentEditor} field of {@code entry} is
     * not equal to {@code editor} we throw IllegalStateException. If our parameter {@code success}
     * is true, and the {@code readable} field of {@code entry} is false (the entry is being created
     * for the first time) we loop over {@code int i} for the {@code valueCount} (1 in our case) files
     * for this entry aborting the edit and throwing IllegalStateException is any of the dirty files
     * returned by {@code entry.getDirtyFile(i)} does not exist.
     * <p>
     * Then we loop over {@code int i} for the {@code valueCount} (1 in our case) files for this entry
     * setting {@code File dirty} to each of the tmp files returned by {@code entry.getDirtyFile(i)}
     * and if our parameter {@code success} is:
     * <ul>
     *     <li>
     *         true: if the file {@code dirty} exists we set {@code File clean} to the old file returned
     *         by {@code entry.getCleanFile(i)} then rename {@code dirty} to {@code clean}. We set
     *         {@code long oldLength} to the value stored in {@code entry.lengths[i]} and set
     *         {@code long newLength} to the length of {@code clean}. We then set {@code entry.lengths[i]}
     *         to {@code newLength} and update {@code size} by subtracting {@code oldLength} and adding
     *         {@code newLength} from it.
     *     </li>
     *     <li>
     *         false: We call our method {@code deleteIfExists} to delete {@code dirty} if it exists.
     *     </li>
     * </ul>
     * We then increment {@code redundantOpCount} and set the {@code currentEditor} field of {@code entry}
     * to null. If the {@code readable} field of {@code entry} is true or our parameter {@code success}
     * is true:
     * <ul>
     *     <li>
     *         true: we set the {@code readable} field of {@code entry} to true, and write a line to
     *         the journal consisting of the string CLEAN ("CLEAN") followed by a blank followed by
     *         the string value of the {@code key} field of {@code entry} followed by the string
     *         returned by {@code entry.getLengths()} (the string value of all the entries in the
     *         {@code lengths[]} array preceded by a blank). If our parameter {@code success} is
     *         true we set the {@code sequenceNumber} field of {@code entry} to {@code nextSequenceNumber}
     *         (post incrementing {@code nextSequenceNumber} in the same statement).
     *     </li>
     *     <li>
     *         both false: We remove the {@code key} {@code Entry} from {@code LinkedHashMap<String, Entry> lruEntries}
     *         and write a line to the journal consisting of the string REMOVE ("REMOVE") followed by a blank followed
     *         by the string value of the {@code key} field of {@code entry}.
     *     </li>
     * </ul>
     * If {@code size} is greater than {@code maxSize} or {@code journalRebuildRequired} determines
     * that a journal rebuild would be profitable, we submit a background job to {@code executorService}
     * which will run our {@code Callable<Void> cleanupCallable} and compress the journal.
     *
     * @param editor {@code Editor} whose results we are finalizing.
     * @param success if true the edit succeeded, if false it failed and we need to clean up.
     * @throws IOException if an IO error of any kind occurred.
     */
    private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (entry.currentEditor != editor) {
            throw new IllegalStateException();
        }

        // if this edit is creating the entry for the first time, every index must have a value
        if (success && !entry.readable) {
            for (int i = 0; i < valueCount; i++) {
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort();
                    throw new IllegalStateException("edit didn't create file " + i);
                }
            }
        }

        for (int i = 0; i < valueCount; i++) {
            File dirty = entry.getDirtyFile(i);
            if (success) {
                if (dirty.exists()) {
                    File clean = entry.getCleanFile(i);
                    //noinspection ResultOfMethodCallIgnored
                    dirty.renameTo(clean);
                    long oldLength = entry.lengths[i];
                    long newLength = clean.length();
                    entry.lengths[i] = newLength;
                    size = size - oldLength + newLength;
                }
            } else {
                deleteIfExists(dirty);
            }
        }

        redundantOpCount++;
        entry.currentEditor = null;
        if (entry.readable | success) {
            entry.readable = true;
            journalWriter.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++;
            }
        } else {
            lruEntries.remove(entry.key);
            journalWriter.write(REMOVE + ' ' + entry.key + '\n');
        }

        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal and eliminate at least
     * 2000 ops. We define the constant REDUNDANT_OP_COMPACT_THRESHOLD to be 2000, and return true
     * if our field {@code redundantOpCount} is greater than or equal to REDUNDANT_OP_COMPACT_THRESHOLD
     * and {@code redundantOpCount} is greater than or equal to the size of our field {@code lruEntries}.
     *
     * @return true if a rebuild will be profitable.
     */
    private boolean journalRebuildRequired() {
        final int REDUNDANT_OP_COMPACT_THRESHOLD = 2000;
        return redundantOpCount >= REDUNDANT_OP_COMPACT_THRESHOLD
                && redundantOpCount >= lruEntries.size();
    }

    /**
     * Drops the entry for {@code key} if it exists and can be removed. Entries actively being edited
     * cannot be removed. First we call our method {@code checkNotClosed} to make sure the journal
     * file is open (it throws IllegalStateException if it is null), then we call our method
     * {@code validateKey(key)} to validate our parameter {@code key} (it throws IllegalArgumentException
     * if it contains " ", "\n", or "\r"). We then initialize {@code Entry entry} by fetching the
     * {@code Entry} stored under {@code key} in our field {@code LinkedHashMap<String, Entry> lruEntries}.
     * If {@code entry} is null, or its field {@code currentEditor} is not null we return false to the
     * caller.
     * <p>
     * We loop over {@code i} for the {@code valueCount} (1 in our case) files in {@code entry} setting
     * {@code File file} to the file path returned by the {@code getCleanFile(i)} method of {@code entry}.
     * If we are not able to delete {@code file} we throw IOException, if we are we subtract the length
     * of the file contained in {@code lengths[i]} from {@code size} and set {@code lengths[i]} to 0.
     * <p>
     * We increment {@code redundantOpCount}, and append a line to the journal consisting of the string
     * REMOVE ("REMOVE") followed by a blank followed by the string value of the {@code key}. We then
     * remove the {@code key} {@code Entry} from {@code LinkedHashMap<String, Entry> lruEntries}. If
     * our method {@code journalRebuildRequired} determines that a journal rebuild would be profitable,
     * we submit a background job to {@code executorService} which will run our {@code cleanupCallable}
     * and compress the journal. Finally we return true to the caller.
     *
     * @param key Key of the {@code Entry} to be removed.
     * @return true if the {@code Entry} was successfully removed
     * @throws IOException if an IO error occurred.
     */
    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean remove(String key) throws IOException {
        checkNotClosed();
        validateKey(key);
        Entry entry = lruEntries.get(key);
        if (entry == null || entry.currentEditor != null) {
            return false;
        }

        for (int i = 0; i < valueCount; i++) {
            File file = entry.getCleanFile(i);
            if (!file.delete()) {
                throw new IOException("failed to delete " + file);
            }
            size -= entry.lengths[i];
            entry.lengths[i] = 0;
        }

        redundantOpCount++;
        //noinspection StringConcatenationInsideStringBufferAppend
        journalWriter.append(REMOVE + ' ' + key + '\n');
        lruEntries.remove(key);

        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }

        return true;
    }

    /**
     * Returns true if this cache has been closed.
     *
     * @return true if this cache has been closed.
     */
    public boolean isClosed() {
        return journalWriter == null;
    }

    /**
     * Throws IllegalStateException if the journal file is not open.
     */
    private void checkNotClosed() {
        if (journalWriter == null) {
            throw new IllegalStateException("cache is closed");
        }
    }

    /**
     * Force buffered operations to the filesystem. First we call our method {@code checkNotClosed}
     * to make sure the journal file is open (it throws IllegalStateException if it is null). Then
     * we call our method {@code trimToSize} which removes the least recently used {@code Entry}
     * objects from our cache until {@code size} is less than or equal to {@code maxSize}. Finally
     * we call the {@code flush} method of {@code Writer journalWriter} to flush all the buffers
     * to disk.
     *
     * @throws IOException If an I/O error occurs
     */
    public synchronized void flush() throws IOException {
        checkNotClosed();
        trimToSize();
        journalWriter.flush();
    }

    /**
     * Closes this cache. Stored values will remain on the filesystem. If {@code journalWriter} is
     * null we return having done nothing. Then we loop through the {@code Entry entry} objects in
     * {@code lruEntries.values()} and if the {@code currentEditor} field of {@code entry} is not
     * null we call its {@code abort} method to abort the edit.
     * <p>
     * We call our method {@code trimToSize} which removes the least recently used {@code Entry}
     * objects from our cache until {@code size} is less than or equal to {@code maxSize}. We call
     * the {@code close} method of {@code journalWriter} to close the file, and set it to null.
     *
     * @throws IOException If an I/O error occurs
     */
    public synchronized void close() throws IOException {
        if (journalWriter == null) {
            return; // already closed
        }
        for (Entry entry : new ArrayList<>(lruEntries.values())) {
            if (entry.currentEditor != null) {
                entry.currentEditor.abort();
            }
        }
        trimToSize();
        journalWriter.close();
        journalWriter = null;
    }

    /**
     * We loop as long as {@code size} is greater than {@code maxSize} fetching key value pairs from
     * each of the entries in {@code lruEntries}, and calling our method {@code remove} on the key
     * of the key value pair to remove the {@code Entry} from the cache.
     *
     * @throws IOException If an I/O error occurs
     */
    private void trimToSize() throws IOException {
        while (size > maxSize) {
//            Map.Entry<String, Entry> toEvict = lruEntries.eldest();
            final Map.Entry<String, Entry> toEvict = lruEntries.entrySet().iterator().next();
            remove(toEvict.getKey());
        }
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete all files in the cache
     * directory including files that weren't created by the cache. We call our method {@code close}
     * to close our cache, then we call our method {@code deleteContents} to recursively delete
     * everything in the cache directory {@code File directory}.
     *
     * @throws IOException If an I/O error occurs
     */
    public void delete() throws IOException {
        close();
        deleteContents(directory);
    }

    /**
     * Throws IllegalArgumentException if our parameter {@code key} contains the characters " ", "\n",
     * or "\r".
     *
     * @param key cache key string
     */
    private void validateKey(String key) {
        if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
            throw new IllegalArgumentException(
                    "keys must not contain spaces or newlines: \"" + key + "\"");
        }
    }

    /**
     * Reads the entire remaining contents of the parameter {@code InputStream in} into a string and
     * returns it to the caller. We create an {@code InputStreamReader} to read from {@code in} using
     * the UTF_8 charset, and return the string returned by our method {@code readFully} when called
     * with it.
     *
     * @param in {@code InputStream} to read from
     * @return the entire contents of {@code InputStream in} as a string
     * @throws IOException If an I/O error occurs.
     */
    private static String inputStreamToString(InputStream in) throws IOException {
        return readFully(new InputStreamReader(in, UTF_8));
    }

    /**
     * A snapshot of the values for an entry.
     */
    public final class Snapshot implements Closeable {
        /**
         * Key of the cached {@code Entry} we are a snapshot of.
         */
        private final String key;
        /**
         * Sequence number of this snapshot, used to detect a stale snapshot.
         */
        private final long sequenceNumber;
        /**
         * The {@code InputStream[]} array for the files contained in the {@code Entry}
         */
        private final InputStream[] ins;

        /**
         * Our constructor. We just save our parameters in our fields of the same name.
         *
         * @param key Key of the cached {@code Entry} we are a snapshot of.
         * @param sequenceNumber Sequence number of this snapshot
         * @param ins {@code InputStream[]} array for the files contained in the {@code Entry}
         */
        private Snapshot(String key, long sequenceNumber, InputStream[] ins) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.ins = ins;
        }

        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress. We just return the Editor instance to edit the Entry
         * with our field {@code key} as the key, and the sequence number the
         * same as our field {@code sequenceNumber}.
         *
         * @return an {@code Editor} for this snapshot's entry
         * @throws IOException If an I/O error occurs
         */
        @SuppressWarnings("unused")
        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(key, sequenceNumber);
        }

        /**
         * Returns the unbuffered stream with the value for {@code index}.
         *
         * @param index index of the {@code InputStream} we want
         * @return input stream for file {@code index}
         */
        public InputStream getInputStream(int index) {
            return ins[index];
        }

        /**
         * Returns the string value for {@code index} by returning the string returned by our method
         * {@code inputStreamToString} when reading the input stream for file {@code index}.
         *
         * @param index index of the {@code InputStream} we want to read from
         * @return String containing the entire remaining contents of InputStream
         * @throws IOException If an I/O error occurs
         */
        @SuppressWarnings("unused")
        public String getString(int index) throws IOException {
            return inputStreamToString(getInputStream(index));
        }

        /**
         * Closes quietly all of the {@code InputStream} objects contained in our field
         * {@code InputStream[] ins}
         */
        @Override
        public void close() {
            for (InputStream in : ins) {
                closeQuietly(in);
            }
        }
    }

    /**
     * Edits the values for an entry.
     */
    public final class Editor {
        /**
         * {@code Entry} we are editing.
         */
        private final Entry entry;
        /**
         * True if an IOException has been caught while doing IO.
         */
        private boolean hasErrors;

        /**
         * Our constructor, just saves our parameter {@code Entry entry} in our field of the same
         * name.
         *
         * @param entry {@code Entry} we are to edit
         */
        private Editor(Entry entry) {
            this.entry = entry;
        }

        /**
         * Returns an unbuffered input stream to read the last committed value, or null if no value
         * has been committed. We synchronize on {@code DiskLruCache.this}, and if the value of the
         * {@code currentEditor} field of our field {@code Entry entry} is not this we throw
         * IllegalStateException. If the {@code readable} field of {@code entry} is false we return
         * null (no values have been committed to disk for this {@code Entry}). Finally we return
         * a {@code FileInputStream} constructed to read the file with the path returned by the
         * {@code getCleanFile} method of {@code entry} for the file index {@code index}.
         *
         * @param index file number whose {@code InputStream} we are to create
         * @return an unbuffered input stream to read from the file with index {@code index}
         * @throws IOException If an I/O error occurs
         */
        public InputStream newInputStream(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    return null;
                }
                return new FileInputStream(entry.getCleanFile(index));
            }
        }

        /**
         * Returns the last committed value as a string, or null if no value has been committed. We
         * create {@code InputStream in} to read from the file with index {@code index}, and if that
         * is not null we return the string returned by our method {@code inputStreamToString} when
         * reading from {@code in}. If {@code in} is null we return null to the caller.
         *
         * @param index file number of file we are to read from
         * @return the entire contents of the file as a string
         * @throws IOException If an I/O error occurs
         */
        @SuppressWarnings("unused")
        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            return in != null ? inputStreamToString(in) : null;
        }

        /**
         * Returns a new unbuffered output stream to write the value at {@code index}. If the
         * underlying output stream encounters errors when writing to the filesystem, this edit
         * will be aborted when {@link #commit} is called. The returned output stream does not
         * throw IOExceptions. We synchronize on {@code DiskLruCache.this}, and if the value of
         * the {@code currentEditor} field of our field {@code Entry entry} is not this we throw
         * IllegalStateException. Finally we return an instance of {@code FaultHidingOutputStream}
         * constructed to write to the {@code FileOutputStream} created to write to the temporary
         * file pathname returned by the {@code getDirtyFile} method of {@code entry} for index
         * {@code index}.
         *
         * @param index index of the file to open for writing.
         * @return unbuffered output stream to write to the file with index {@code index}
         * @throws IOException If an I/O error occurs
         */
        public OutputStream newOutputStream(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                return new FaultHidingOutputStream(new FileOutputStream(entry.getDirtyFile(index)));
            }
        }

        /**
         * Sets the value at {@code index} to {@code value}. First we initialize {@code Writer writer}
         * to null, then wrapped in a try block whose finally block calls our method {@code closeQuietly}
         * to close {@code writer} we set {@code writer} to an {@code OutputStreamWriter} constructed
         * to write the the unbuffered output stream created to write to the path for the file with
         * index {@code index}, then write our parameter {@code String value} to it.
         *
         * @param index index of the file to write to
         * @param value string to write to the file
         * @throws IOException If an I/O error occurs
         */
        public void set(int index, String value) throws IOException {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(newOutputStream(index), UTF_8);
                writer.write(value);
            } finally {
                closeQuietly(writer);
            }
        }

        /**
         * Commits this edit so it is visible to readers. This releases the edit lock so another
         * edit may be started on the same key. If our flag {@code hasErrors} is true (an IOException
         * occurred during the edit) we call our method {@code completeEdit} to finalize this edit
         * by deleting our temp files (if they exist), then we remove the {@code Entry} whose key
         * is given in the {@code key} field of {@code entry} from our cache.
         *
         * @throws IOException If an I/O error occurs
         */
        public void commit() throws IOException {
            if (hasErrors) {
                completeEdit(this, false);
                remove(entry.key); // the previous entry is stale
            } else {
                completeEdit(this, true);
            }
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be started on the same
         * key. We just call our method {@code completeEdit} to finalize this edit by deleting our
         * temp files (if they exist).
         *
         * @throws IOException If an I/O error occurs
         */
        public void abort() throws IOException {
            completeEdit(this, false);
        }

        /**
         * A {@code FilterOutputStream} which catches {@code IOException} and just sets the flag
         * {@code boolean hasErrors} to true.
         */
        private class FaultHidingOutputStream extends FilterOutputStream {
            /**
             * Our constructor, we just call our super's constructor.
             *
             * @param out underlying output stream
             */
            private FaultHidingOutputStream(OutputStream out) {
                super(out);
            }

            /**
             * Writes the specified byte to this output stream. Wrapped in a try block intended to
             * catch IOException we just call the {@code write} method of the underlying output
             * stream passed to our constructor. If we catch IOException we just set {@code hasErrors}
             * to true.
             *
             * @param oneByte the byte.
             */
            @Override
            public void write(int oneByte) {
                try {
                    out.write(oneByte);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            /**
             * Writes <code>len</code> bytes from the specified <code>byte</code> array starting at
             * offset <code>off</code> to this output stream. Wrapped in a try block intended to
             * catch IOException we just call the {@code write} method of the underlying output
             * stream passed to our constructor. If we catch IOException we just set {@code hasErrors}
             * to true.
             *
             * @param buffer the data.
             * @param offset the start offset in the data.
             * @param length the number of bytes to write.
             */
            @Override
            public void write(@NonNull byte[] buffer, int offset, int length) {
                try {
                    out.write(buffer, offset, length);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            /**
             * Closes this output stream and releases any system resources associated with the stream.
             * Wrapped in a try block intended to catch IOException we just call the {@code close}
             * method of the underlying output  stream passed to our constructor. If we catch IOException
             * we just set {@code hasErrors} to true.
             */
            @Override
            public void close() {
                try {
                    out.close();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            /**
             * Flushes this output stream and forces any buffered output bytes to be written out to
             * the stream. Wrapped in a try block intended to catch IOException we just call the
             * {@code flush} method of the underlying output  stream passed to our constructor. If
             * we catch IOException we just set {@code hasErrors} to true.
             */
            @Override
            public void flush() {
                try {
                    out.flush();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
        }
    }

    /**
     * Object controlling a cache entry that is on the disk (or headed to disk(
     */
    private final class Entry {
        /** Key for referencing this {@code Entry} and creating file names */
        private final String key;

        /** Lengths of this entry's files. */
        private final long[] lengths;

        /** True if this entry has ever been published */
        private boolean readable;

        /** The ongoing edit or null if this entry is not being edited. */
        private Editor currentEditor;

        /** The sequence number of the most recently committed edit to this entry. */
        private long sequenceNumber;

        /**
         * Our constructor. First we save our parameter {@code String key} in our field of the same
         * name. Then we initialize our field {@code long[] lengths} with an array that can hold
         * {@code valueCount} values.
         *
         * @param key Key for referencing this {@code Entry}
         */
        private Entry(String key) {
            this.key = key;
            this.lengths = new long[valueCount];
        }

        /**
         * Concatenates the string value of all of the values in the {@code lengths[]} field, each
         * preceded by a blank character and returns that string to the caller. First we initialize
         * {@code StringBuilder result} with a new instance. Then we loop through all of the
         * {@code long size} values in {@code lengths[]} appending a blank to {@code result} followed
         * by the string value of {@code size}. Finally we return the string value of {@code result}
         * to the caller.
         *
         * @return string composed of the concatenated string values of our field {@code lengths[]}
         * each preceded by a blank.
         */
        public String getLengths() {
            StringBuilder result = new StringBuilder();
            for (long size : lengths) {
                result.append(' ').append(size);
            }
            return result.toString();
        }

        /**
         * Set our {@code lengths[]} field by treating our {@code String[] strings} parameter as
         * decimal numbers like "10123". First we make sure that the length of the our parameter
         * {@code String[] strings} is equal to {@code valueCount}, throwing the IOException returned
         * by our method {@code invalidLengths(strings)} if they are unequal. Then wrapped in a try
         * block intended to catch NumberFormatException in order to throw the IOException returned
         * by our method {@code invalidLengths(strings)} instead, we loop over {@code int i} for all
         * of the strings in our parameter {@code String[] strings} setting {@code lengths[i]} to
         * the long value parsed from {@code strings[i]}.
         *
         * @param strings array of strings to convert to {@code long} values to set our
         *                {@code lengths[]} array entries to.
         * @throws IOException if the length of the {@code strings} array is not equal to {@code valueCount}
         * or there is a non-decimal character in one of the strings
         */
        private void setLengths(String[] strings) throws IOException {
            if (strings.length != valueCount) {
                throw invalidLengths(strings);
            }

            try {
                for (int i = 0; i < strings.length; i++) {
                    lengths[i] = Long.parseLong(strings[i]);
                }
            } catch (NumberFormatException e) {
                throw invalidLengths(strings);
            }
        }

        /**
         * Throws an IOException constructed with a detail message consisting of the string
         * "unexpected journal line: " followed by the string formed by the parameter
         * {@code strings}.
         *
         * @param strings array of strings that {@code setLengths} was called from
         * @return an IOException constructed with a detail message consisting of the string
         * "unexpected journal line: " followed by the string formed by the parameter {@code strings}.
         * @throws IOException always
         */
        private IOException invalidLengths(String[] strings) throws IOException {
            throw new IOException("unexpected journal line: " + Arrays.toString(strings));
        }

        /**
         * Creates and returns a {@code File} object created for the directory {@code directory} and
         * filename formed by concatenating the string {@code key} followed by a ".", followed by
         * the string value of our parameter {@code i}.
         *
         * @param i index of the file whose path we are to generate
         * @return path to the file with index {@code i}.
         */
        public File getCleanFile(int i) {
            return new File(directory, key + "." + i);
        }

        /**
         * Creates and returns a {@code File} object created for the directory {@code directory} and
         * filename formed by concatenating the string {@code key} followed by a ".", followed by
         * the string value of our parameter {@code i}, followed by the string ".tmp". This is the
         * temp file for the {@code File} created when calling {@code getCleanFile}.
         *
         * @param i index of the temp file whose path we are to generate
         * @return path to the temp file with index {@code i}.
         */
        public File getDirtyFile(int i) {
            return new File(directory, key + "." + i + ".tmp");
        }
    }
}
