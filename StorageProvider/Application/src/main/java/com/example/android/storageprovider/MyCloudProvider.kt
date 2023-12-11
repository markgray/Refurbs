/*
 * Copyright (C) 2013 The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog")

package com.example.android.storageprovider

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.example.android.common.logger.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.PriorityQueue

/**
 * Manages documents and exposes them to the Android system for sharing.
 */
class MyCloudProvider : DocumentsProvider() {
    /**
     * A [File] object at the root of the file hierarchy. Depending on your implementation, the root
     * does not need to be an existing file system directory. For example, a tag-based document
     * provider might return a directory containing all tags, represented as child directories.
     */
    private var mBaseDir: File? = null

    /**
     * Implement this to initialize your content provider on startup. This method is called for all
     * registered content providers on the application main thread at application launch time. It
     * must not perform lengthy operations, or application startup will be delayed.
     *
     *
     * First we log that we were called. Then we initialize our field `File mBaseDir` by calling
     * the `getFilesDir` method of the Context this provider is running in to get the absolute
     * path to the directory on the filesystem where files created with `openFileOutput` are stored.
     * Then we call our `writeDummyFilesToStorage` method to transfer the dummy content files
     * we provide from the apk to the file storage. Finally we return true to indicate the provider
     * was successfully loaded
     *
     * @return true to indicate the provider was successfully loaded
     */
    override fun onCreate(): Boolean {
        Log.v(TAG, "onCreate")
        mBaseDir = context!!.filesDir
        writeDummyFilesToStorage()
        return true
    }

    /**
     * Return all roots currently provided. First we log the fact that we were called. Then we initialize
     * `MatrixCursor result` with a new instance using our parameter `projection` for the
     * column names if it is not null or DEFAULT_ROOT_PROJECTION if it is null. If our method `isUserLoggedIn`
     * returns false (the user is not logged in) we return the empty `result` which will remove us
     * from the list of providers. Otherwise we initialize `MatrixCursor.RowBuilder row` by calling
     * the `newRow` method of `result` to add a new row to it, saving the `RowBuilder`
     * it returns for that row in `row`. We now proceed to add the following columns to row:
     *
     *  *
     * COLUMN_ROOT_ID: (Unique ID of a root) ROOT "root"
     *
     *  *
     * COLUMN_SUMMARY: (Summary for this root, which may be shown to a user) the string with
     * resource id R.string.root_summary ("cloudy with a chance of...")
     *
     *  *
     * COLUMN_FLAGS: (Flags that apply to a root) bitwise or of FLAG_SUPPORTS_CREATE (Flag
     * indicating that at least one directory under this root supports creating content),
     * FLAG_SUPPORTS_RECENTS (Flag indicating that this root can be queried to provide recently
     * modified documents), FLAG_SUPPORTS_SEARCH (Flag indicating that this root supports search)
     *
     *  *
     * COLUMN_TITLE: (Title for a root, which will be shown to a user) the string with resource
     * id R.string.app_name (StorageProvider)
     *
     *  *
     * COLUMN_DOCUMENT_ID: (Document which is a directory that represents the top directory of
     * this root) the string returned by our `getDocIdForFile` method when passed our
     * field `File mBaseDir`.
     *
     *  *
     * COLUMN_MIME_TYPES: (MIME types supported by this root) the string returned by our method
     * `getChildMimeTypes`
     *
     *  *
     * COLUMN_AVAILABLE_BYTES: (Number of bytes available in this root) the value returned by
     * the `getFreeSpace` method of `mBaseDir` (Returns the number of unallocated
     * bytes in the partition containing it)
     *
     *  *
     * COLUMN_ICON: (Icon resource ID for a root) R.drawable.ic_launcher (a cloud icon)
     *
     *
     * Finally we return `result` to the caller.
     *
     * @param projection list of [Root] columns to put into the cursor. If
     * `null` all supported columns should be included.
     * @return `MatrixCursor` containing all the columns in our parameter `projection`
     * for our root called "MyCloud".
     */
    override fun queryRoots(projection: Array<String>?): Cursor {
        Log.v(TAG, "queryRoots")

        // Create a cursor with either the requested fields, or the default projection.  This
        // cursor is returned to the Android system picker UI and used to display all roots from
        // this provider.
        val result = MatrixCursor(resolveRootProjection(projection))

        // If user is not logged in, return an empty root cursor.  This removes our provider from
        // the list entirely.
        if (!isUserLoggedIn) {
            return result
        }

        // It's possible to have multiple roots (e.g. for multiple accounts in the same app) -
        // just add multiple cursor rows.
        // Construct one row for a root called "MyCloud".
        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, ROOT)
        row.add(Root.COLUMN_SUMMARY, context!!.getString(R.string.root_summary))

        // FLAG_SUPPORTS_CREATE means at least one directory under the root supports creating
        // documents.  FLAG_SUPPORTS_RECENTS means your application's most recently used
        // documents will show up in the "Recents" category.  FLAG_SUPPORTS_SEARCH allows users
        // to search all documents the application shares.
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or
            Root.FLAG_SUPPORTS_RECENTS or
            Root.FLAG_SUPPORTS_SEARCH)

        // COLUMN_TITLE is the root title (e.g. what will be displayed to identify your provider).
        row.add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))

        // This document id must be unique within this provider and consistent across time.  The
        // system picker UI may save it and refer to it later.
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(mBaseDir))

        // The child MIME types are used to filter the roots and only present to the user roots
        // that contain the desired type somewhere in their file hierarchy.
        row.add(Root.COLUMN_MIME_TYPES, childMimeTypes)
        row.add(Root.COLUMN_AVAILABLE_BYTES, mBaseDir!!.freeSpace)
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher)
        return result
    }

    /**
     * Return recently modified documents under the requested root. First we log the fact that we were
     * called. Then we initialize `MatrixCursor result` with a new instance using our parameter
     * `projection` for the column names if it is not null or DEFAULT_DOCUMENT_PROJECTION if it
     * is null. We next initialize `File parent` with the File that our method `getFileForDocId`
     * locates when given `rootId` for the document ID representing the desired file. We create
     * `PriorityQueue<File> lastModifiedFiles` with an initial capacity of 5 and an anonymous
     * class as its `Comparator` whose `compare` override orders the files by last modified
     * time.
     *
     *
     * We initialize `LinkedList<File> pending` with a new instance, and add `parent` to
     * it. Then we loop while `pending` is not empty we initialize `File file` by removing
     * the first file in `pending` and if it is a directory we add the array of abstract path
     * names of the files in the directory to `pending` (its children). If it is a File we add
     * it to `lastModifiedFiles`.
     *
     *
     * When we have processed all the entries in `pending` we loop over `int i` for the
     * minimum of MAX_LAST_MODIFIED+1 and the size of `lastModifiedFiles` initializing `File file`
     * by removing the head of the queue `lastModifiedFiles`. We then pass `file` to our
     * method `includeFile` to add it to our `MatrixCursor result`.
     *
     *
     * Finally we return `result` to the caller.
     *
     * @param rootId the document ID representing the root document.
     * @param projection list of [ Document ] columns to put into the
     * cursor. If `null` all supported columns should be
     * included.
     * @return `Cursor` containing list of recently modified documents under the requested root.
     * @throws FileNotFoundException if a file is not found by our `getFileForDocId` method
     */
    @Throws(FileNotFoundException::class)
    override fun queryRecentDocuments(rootId: String, projection: Array<String>?): Cursor {
        Log.v(TAG, "queryRecentDocuments")

        // This example implementation walks a local file structure to find the most recently
        // modified files.  Other implementations might include making a network call to query a
        // server.

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val parent = getFileForDocId(rootId)

        // Create a queue to store the most recent documents, which orders by last modified.
        val lastModifiedFiles = PriorityQueue<File?>(5) { i, j ->
            java.lang.Long.compare(i.lastModified(), j.lastModified())
        }

        // Iterate through all files and directories in the file structure under the root.  If
        // the file is more recent than the least recently modified, add it to the queue,
        // limiting the number of results.
        val pending = LinkedList<File?>()

        // Start by adding the parent to the list of files to be processed
        pending.add(parent)

        // Do while we still have unexamined files
        while (!pending.isEmpty()) {
            // Take a file from the list of unprocessed files
            val file = pending.removeFirst()
            if (file!!.isDirectory) {
                // If it's a directory, add all its children to the unprocessed list
                Collections.addAll(pending, *file.listFiles()!!)
            } else {
                // If it's a file, add it to the ordered queue.
                lastModifiedFiles.add(file)
            }
        }

        // Add the most recent files to the cursor, not exceeding the max number of results.
        for (i in 0 until Math.min(MAX_LAST_MODIFIED + 1, lastModifiedFiles.size)) {
            val file = lastModifiedFiles.remove()
            includeFile(result, null, file)
        }
        return result
    }

    /**
     * Return documents that match the given query under the requested root. First we log the fact that
     * we were called. We initialize `MatrixCursor result` with a new instance using our parameter
     * `projection` for the column names if it is not null or DEFAULT_DOCUMENT_PROJECTION if it
     * is null. We next initialize `File parent` with the File that our method `getFileForDocId`
     * locates when given `rootId` for the document ID representing the desired file.
     *
     *
     * We initialize `LinkedList<File> pending` with a new instance, and add `parent` to
     * it. Then we loop while `pending` is not empty and the numbers of rows in the cursor `result`
     * is less than MAX_SEARCH_RESULTS, initializing `File file` with the first file in `pending`
     * and if it is a directory we add the array of abstract path names of the files in the directory to
     * `pending` (its children). If it is a File and the filename contains our search string `query`
     * we add it to `result`.
     *
     *
     * When we are done processing all the files in `pending` we return `result` to the caller.
     *
     * @param rootId the root to search under.
     * @param query string to match documents against.
     * @param projection list of [ Document ] columns to put into the
     * cursor. If `null` all supported columns should be
     * included.
     * @return `Cursor` containing the filenames that match the query
     * @throws FileNotFoundException if our `getFileForDocId` is unable to access the root
     */
    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(rootId: String, query: String, projection: Array<String>?): Cursor {
        Log.v(TAG, "querySearchDocuments")

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val parent = getFileForDocId(rootId)

        // This example implementation searches file names for the query and doesn't rank search
        // results, so we can stop as soon as we find a sufficient number of matches.  Other
        // implementations might use other data about files, rather than the file name, to
        // produce a match; it might also require a network call to query a remote server.

        // Iterate through all files in the file structure under the root until we reach the
        // desired number of matches.
        val pending = LinkedList<File?>()

        // Start by adding the parent to the list of files to be processed
        pending.add(parent)

        // Do while we still have unexamined files, and fewer than the max search results
        while (!pending.isEmpty() && result.count < MAX_SEARCH_RESULTS) {
            // Take a file from the list of unprocessed files
            val file = pending.removeFirst()
            if (file!!.isDirectory) {
                // If it's a directory, add all its children to the unprocessed list
                Collections.addAll(pending, *file.listFiles()!!)
            } else {
                // If it's a file and it matches, add it to the result cursor.
                if (file.name.lowercase(Locale.getDefault()).contains(query)) {
                    includeFile(result, null, file)
                }
            }
        }
        return result
    }

    /**
     * Open and return a thumbnail of the requested document. First we log the fact that we were
     * called. We next initialize `File file` with the File that our method `getFileForDocId`
     * locates when given `documentId` for the document ID representing the desired file. We
     * initialize `ParcelFileDescriptor pfd` by opening `file` in MODE_READ_ONLY. Finally
     * we return a new instance of `AssetFileDescriptor` constructed from `pfd` with 0 for
     * the starting offset and UNKNOWN_LENGTH as the length.
     *
     * @param documentId the document to return.
     * @param sizeHint hint of the optimal thumbnail dimensions.
     * @param signal used by the caller to signal if the request should be
     * cancelled. May be null
     * @return `AssetFileDescriptor`
     * @throws FileNotFoundException if our `getFileForDocId` is unable to find the document
     */
    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(documentId: String, sizeHint: Point,
                                       signal: CancellationSignal): AssetFileDescriptor {
        Log.v(TAG, "openDocumentThumbnail")
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    /**
     * Return metadata for the single requested document. First we log the fact that we were called.
     * We initialize `MatrixCursor result` with a new instance using our parameter
     * `projection` for the column names if it is not null or DEFAULT_DOCUMENT_PROJECTION if it
     * is null. We then call our `includeFile` method to locate the `documentId` file and
     * add the metadata for that file to `result`. Finally we return result to the caller.
     *
     * @param documentId the document to return.
     * @param projection list of [ Document ] columns to put into the
     * cursor. If `null` all supported columns should be
     * included.
     * @return `Cursor` containing the metadata for the requested document.
     * @throws FileNotFoundException if the `getFileForDocId` method called by `includeFile`
     * is unable to find the `documentId` file.
     */
    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        Log.v(TAG, "queryDocument")

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        includeFile(result, documentId, null)
        return result
    }

    /**
     * Return the children documents contained in the requested directory. This must only return
     * immediate descendants, as additional queries will be issued to recursively explore the tree.
     * First we log the fact that we were called. We initialize `MatrixCursor result` with a
     * new instance using our parameter `projection` for the column names if it is not null or
     * DEFAULT_DOCUMENT_PROJECTION if it is null. We initialize `File parent` by using the
     * `getFileForDocId` to find the directory with document id `parentDocumentId`. We
     * loop through all the `File file` in the array returned by the `listFiles` method
     * of `parent` calling our `includeFile` method for each to add it to `result`.
     * Finally we return `result` to the caller.
     *
     * @param parentDocumentId the directory to return children for.
     * @param projection list of [ Document ] columns to put into the
     * cursor. If `null` all supported columns should be
     * included.
     * @param sortOrder how to order the rows, formatted as an SQL
     * `ORDER BY` clause (excluding the ORDER BY itself).
     * Passing `null` will use the default sort order, which
     * may be unordered. This ordering is a hint that can be used to
     * prioritize how data is fetched from the network, but UI may
     * always enforce a specific ordering.
     * @return `Cursor` containing all the child files of the `parentDocumentId` directory
     * @throws FileNotFoundException if the `getFileForDocId` method is unable to find any of
     * the files.
     */
    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?,
                                     sortOrder: String): Cursor {
        Log.v(TAG, "queryChildDocuments, parentDocumentId: " +
            parentDocumentId +
            " sortOrder: " +
            sortOrder)
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val parent = getFileForDocId(parentDocumentId)
        for (file in parent!!.listFiles()!!) {
            includeFile(result, null, file)
        }
        return result
    }

    /**
     * Open and return the requested document. First we log the fact that we were called. We next
     * initialize `File file` with the File that our method `getFileForDocId` locates
     * when given `documentId` for the document ID representing the desired file. We initialize
     * `int accessMode` by using the `ParcelFileDescriptor.parseMode` to parse our parameter
     * `String mode`. We initialize `boolean isWrite` to true if `mode` contains
     * the character 'w'. We then branch on the value of `isWrite`:
     *
     *  *
     * true: Wrapped in a try block intended to catch IOException in order to throw FileNotFoundException
     * we initialize `Handler handler` with an instance which uses the Looper for the main thread
     * of the current process, then return the `ParcelFileDescriptor` that its static `open`
     * method returns when opening `file` in `accessMode` mode, using `handler` as the
     * handler to call the anonymous listener we create which overrides the `onClose` method to
     * log the fact that the `documentId` file has been closed.
     *
     *  *
     * false: we return the `ParcelFileDescriptor` that its static `open` method returns
     * when opening `file` in `accessMode` mode.
     *
     *
     *
     * @param documentId the document to return.
     * @param mode the mode to open with, such as 'r', 'w', or 'rw'.
     * @param signal used by the caller to signal if the request should be
     * cancelled. May be null.
     * @return `ParcelFileDescriptor` for the file with the document id `documentId`.
     * @throws FileNotFoundException if `ParcelFileDescriptor.open` is unable to open the file,
     * or if `getFileForDocId` is unable to find the file with document id `documentId`.
     */
    @Throws(FileNotFoundException::class)
    override fun openDocument(documentId: String, mode: String,
                              signal: CancellationSignal?): ParcelFileDescriptor {
        Log.v(TAG, "openDocument, mode: $mode")
        // It's OK to do network operations in this method to download the document, as long as you
        // periodically check the CancellationSignal.  If you have an extremely large file to
        // transfer from the network, a better solution may be pipes or sockets
        // (see ParcelFileDescriptor for helper methods).
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        val isWrite = mode.indexOf('w') != -1
        return if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            try {
                val handler = Handler(context!!.mainLooper)
                ParcelFileDescriptor.open(file, accessMode, handler
                ) { // Update the file with the cloud server.  The client is done writing.
                    Log.i(TAG, "A file with id " + documentId + " has been closed!  Time to " +
                        "update the server.")
                }
            } catch (e: IOException) {
                throw FileNotFoundException("Failed to open document with id " + documentId +
                    " and mode " + mode)
            }
        } else {
            ParcelFileDescriptor.open(file, accessMode)
        }
    }

    /**
     * Create a new document and return its newly generated [ Document.COLUMN_DOCUMENT_ID ]. You
     * must allocate a new [ Document.COLUMN_DOCUMENT_ID ] to represent the document, which must
     * not change once returned. First we log the fact that we were called. We initialize `File parent`
     * by using the `getFileForDocId` to find the directory with document id `documentId`.
     * We initialize `File file` with a new instance using the pathname of `parent` and the
     * child pathname `displayName`. Then wrapped in a try block intended to catch IOException in
     * order to throw FileNotFoundException we call the `createNewFile` method of `file` to
     * create a new, empty file named by this abstract pathname, set its write permission, and set its
     * read permission. Finally we return the document ID for `file` generated by our method
     * `getDocIdForFile`.
     *
     * @param documentId the parent directory to create the new document
     * under.
     * @param mimeType the concrete MIME type associated with the new document.
     * If the MIME type is not supported, the provider must throw.
     * @param displayName the display name of the new document. The provider may
     * alter this name to meet any internal constraints, such as
     * avoiding conflicting names.
     * @return document id for the newly created file.
     * @throws FileNotFoundException if `getFileForDocId` is unable to find the file with document
     * id `documentId` or an IOException occurs when trying to create the document.
     */
    @Throws(FileNotFoundException::class)
    override fun createDocument(documentId: String, mimeType: String, displayName: String): String {
        Log.v(TAG, "createDocument")
        val parent = getFileForDocId(documentId)
        val file = File(parent!!.path, displayName)
        try {
            file.createNewFile()
            file.setWritable(true)
            file.setReadable(true)
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with name " +
                displayName + " and documentId " + documentId)
        }
        return getDocIdForFile(file)
    }

    /**
     * Delete the requested document. First we log the fact that we were called. We next initialize
     * `File file` with the File that our method `getFileForDocId` locates when given
     * `documentId` for the document ID representing the desired file. If the `delete`
     * method of `file` returns true (the file was successfully deleted) we log the fact that
     * we deleted the file, otherwise we throw FileNotFoundException.
     *
     * @param documentId the document to delete.
     * @throws FileNotFoundException if `delete` method returns false.
     */
    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        Log.v(TAG, "deleteDocument")
        val file = getFileForDocId(documentId)
        if (file!!.delete()) {
            Log.i(TAG, "Deleted file with id $documentId")
        } else {
            throw FileNotFoundException("Failed to delete document with id $documentId")
        }
    }

    /**
     * Return concrete MIME type of the requested document. We initialize `File file` with the
     * File that our method `getFileForDocId` locates when given `documentId` for the
     * document ID representing the desired file. We then return the value returned by our method
     * `getTypeForFile` for that file.
     *
     * @param documentId the document whose mimetype we are to return
     * @return concrete MIME type of the requested document
     * @throws FileNotFoundException if `getFileForDocId` cannot locate the document
     */
    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        val file = getFileForDocId(documentId)
        return getTypeForFile(file)
    }// Flatten the list into a string and insert newlines between the MIME type strings.

    /**
     * Gets a string of unique MIME data types a directory supports, separated by newlines. This
     * should not change. We initialize `Set<String> mimeTypes` with a new instance of
     * `HashSet`, and add the strings "image/ *", "text/ *", and
     * "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to it. We then
     * initialize `StringBuilder mimeTypesString` with a new instance then loop for all the
     * `String mimeType` in `mimeTypes` appending `mimeType` followed by a newline
     * character to `mimeTypesString`. When done looping we return the string version of
     * `mimeTypesString` to the caller.
     *
     * @return a string of the unique MIME data types the parent directory supports
     */
    private val childMimeTypes: String
        get() {
            val mimeTypes: MutableSet<String> = HashSet()
            mimeTypes.add("image/*")
            mimeTypes.add("text/*")
            mimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document")

            // Flatten the list into a string and insert newlines between the MIME type strings.
            val mimeTypesString = StringBuilder()
            for (mimeType in mimeTypes) {
                mimeTypesString.append(mimeType).append("\n")
            }
            return mimeTypesString.toString()
        }

    /**
     * Get the document ID given a File. The document id must be consistent across time. Other
     * applications may save the ID and use it to reference documents later.
     *
     *
     * This implementation is specific to this demo. It assumes only one root and is built
     * directly from the file structure. However, it is possible for a document to be a child of
     * multiple directories (for example "android" and "images"), in which case the file must have
     * the same consistent, unique document ID in both cases.
     *
     *
     * We initialize `String path` with the absolute path of our parameter `File file`,
     * and initialize `String rootPath` with the string form of the abstract pathname of our
     * field `File mBaseDir`. If `rootPath` is equal to `path` we set `path`
     * to the empty string, else if `rootPath` ends with the "/" character we set `path`
     * to the substring of `path` which follows the end of `rootPath`, if it does not end
     * with "/" we add 1 to the length of `rootPath` to skip the "/" in `path` and set
     * `path` to the substring of `path` which follows that "/".
     *
     *
     * Finally we return the string formed by concatenating the string "root" followed by the ":"
     * character followed by `path`.
     *
     * @param file the File whose document ID you want
     * @return the corresponding document ID
     */
    private fun getDocIdForFile(file: File?): String {
        var path = file!!.absolutePath

        // Start at first char of path under root
        val rootPath = mBaseDir!!.path
        path = if (rootPath == path) {
            ""
        } else if (rootPath.endsWith("/")) {
            path.substring(rootPath.length)
        } else {
            path.substring(rootPath.length + 1)
        }
        return "root:$path"
    }

    /**
     * Add a representation of a file to a cursor. If our parameter `docId` is null we set it
     * to the document id returned by our method `getDocIdForFile` when passed our parameter
     * `File file`, otherwise we set `file` to the file that our method `getFileForDocId`
     * finds when passed the document id `docId`.
     *
     *
     * We initialize `int flags` to 0. If `file` is a directory, then if we can write to
     * that directory we add the FLAG_DIR_SUPPORTS_CREATE flag to `flags`. If it is not a directory
     * and we can write to it we add the flags FLAG_SUPPORTS_WRITE and FLAG_SUPPORTS_DELETE to it.
     *
     *
     * We initialize `String displayName` with the name of the file or directory `file`,
     * then initialize `String mimeType` with the mimetype that our method `getTypeForFile`
     * guesses for `file`. If `mimeType` starts with the string "image/" we set the
     * FLAG_SUPPORTS_THUMBNAIL flag of `flags`.
     *
     *
     * WE initialize `MatrixCursor.RowBuilder row` with a new row added to `result`. We
     * add to it `docId` in the column COLUMN_DOCUMENT_ID, `displayName` in the column
     * COLUMN_DISPLAY_NAME, the length of `file` in column COLUMN_SIZE, `mimeType` in
     * column COLUMN_MIME_TYPE, the time that `file` was last modified in column COLUMN_LAST_MODIFIED,
     * `flags` in column COLUMN_FLAGS, and the resource id R.drawable.ic_launcher in column
     * COLUMN_ICON.
     *
     * @param result the cursor to modify
     * @param docId  the document ID representing the desired file (may be null if given file)
     * @param file   the File object representing the desired file (may be null if given docID)
     * @throws java.io.FileNotFoundException if file is not found
     */
    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        var docIdLocal = docId
        var fileLocal = file
        if (docIdLocal == null) {
            docIdLocal = getDocIdForFile(fileLocal)
        } else {
            fileLocal = getFileForDocId(docIdLocal)
        }
        var flags = 0
        if (fileLocal!!.isDirectory) {
            // Request the folder to lay out as a grid rather than a list. This also allows a larger
            // thumbnail to be displayed for each image.
            //            flags |= Document.FLAG_DIR_PREFERS_GRID;

            // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
            if (fileLocal.isDirectory && fileLocal.canWrite()) {
                @Suppress("KotlinConstantConditions")
                flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
            }
        } else if (fileLocal.canWrite()) {
            // If the file is writable set FLAG_SUPPORTS_WRITE and
            // FLAG_SUPPORTS_DELETE
            @Suppress("KotlinConstantConditions")
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }
        val displayName = fileLocal.name
        val mimeType = getTypeForFile(fileLocal)
        if (mimeType.startsWith("image/")) {
            // Allow the image to be represented by a thumbnail rather than an icon
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
        }
        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docIdLocal)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_SIZE, fileLocal.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, fileLocal.lastModified())
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)

        // Add a custom icon
        row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_launcher)
    }

    /**
     * Translate your custom URI scheme into a File object. We initialize `File target` to our
     * field `File mBaseDir`. If our parameter `docId` is equal to ROOT ("root") we return
     * `target` to the caller. We initialize `int splitIndex` by finding the index of the
     * first ':' character in `docId` (starting from 1). If this is less then 0 we throw
     * FileNotFoundException. Otherwise we set `String path` to the substring that follows the
     * ':' character. We then set `target` to a new file using `target` as the base directory
     * and `path` as the child pathname. If `target` does not exist we throw FileNotFoundException,
     * otherwise we return `target` to the caller.
     *
     * @param docId the document ID representing the desired file
     * @return a File represented by the given document ID
     * @throws java.io.FileNotFoundException if file is not found
     */
    @Throws(FileNotFoundException::class)
    private fun getFileForDocId(docId: String): File? {
        var target = mBaseDir
        if (docId == ROOT) {
            return target
        }
        val splitIndex = docId.indexOf(':', 1)
        return if (splitIndex < 0) {
            throw FileNotFoundException("Missing root for $docId")
        } else {
            val path = docId.substring(splitIndex + 1)
            target = File(target, path)
            if (!target.exists()) {
                throw FileNotFoundException("Missing file for $docId at $target")
            }
            target
        }
    }

    /**
     * Preload sample files packaged in the apk into the internal storage directory.  This is a
     * dummy function specific to this demo.  The MyCloud mock cloud service doesn't actually
     * have a backend, so it simulates by reading content from the device's internal storage.
     *
     *
     * If the length of the array of strings naming the files and directories in `mBaseDir` is
     * greater than 0 we return having done nothing (we have already been run before now). Otherwise
     * we initialize `int[] imageResIds` with the resource ID array that our method
     * `getResourceIdArray` constructs from the resource array R.array.image_res_ids
     * (references for five jpg files in /raw) then loop for all the `int resId` in
     * `imageResIds` calling our method `writeFileToInternalStorage` for each `resId`
     * using ".jpeg" as the extension.
     *
     *
     * We next initialize `int[] textResIds` with the resource ID array that our method
     * `getResourceIdArray` constructs from the resource array R.array.text_res_ids
     * (references to two text files in /raw) then loop for all the `int resId` in
     * `imageResIds` calling our method `writeFileToInternalStorage` for each `resId`
     * using ".txt" as the extension.
     *
     *
     * We next initialize `int[] docxResIds` with the resource ID array that our method
     * `getResourceIdArray` constructs from the resource array R.array.docx_res_ids
     * (reference to one docx file) then loop for all the `int resId` in `docxResIds`
     * calling our method `writeFileToInternalStorage` for each `resId` using ".docx" as
     * the extension.
     */
    private fun writeDummyFilesToStorage() {
        if (mBaseDir!!.list()!!.isNotEmpty()) {
            return
        }
        val imageResIds = getResourceIdArray(R.array.image_res_ids)
        for (resId in imageResIds) {
            writeFileToInternalStorage(resId, ".jpeg")
        }
        val textResIds = getResourceIdArray(R.array.text_res_ids)
        for (resId in textResIds) {
            writeFileToInternalStorage(resId, ".txt")
        }
        val docxResIds = getResourceIdArray(R.array.docx_res_ids)
        for (resId in docxResIds) {
            writeFileToInternalStorage(resId, ".docx")
        }
    }

    /**
     * Write a file to internal storage. Used to set up our dummy "cloud server". First we initialize
     * `InputStream ins` with a data stream for reading the raw resource with resource ID
     * `resId`. Then we initialize `ByteArrayOutputStream outputStream` with a new instance,
     * declare `int size` and allocate 1024 bytes for `byte[] buffer`. Then wrapped in a
     * try block intended to catch IOException in order to print its stack trace we loop while the
     * `size` read from `ins` into `buffer` is greater than or equal to zero writing
     * the bytes read to `outputStream`. When done reading we close `ins` then set
     * `buffer` to the contents of `outputStream`. We create `String filename` by
     * appending our parameter `extension` to the entry name of the resource with resource ID
     * `resId`. We then open `filename` for output with its mode MODE_PRIVATE to initialize
     * `FileOutputStream fos`, write the contents of `buffer` to it then close it.
     *
     * @param resId     the resource ID of the file to write to internal storage
     * @param extension the file extension (ex. .png, .mp3)
     */
    private fun writeFileToInternalStorage(resId: Int, extension: String) {
        val ins = context!!.resources.openRawResource(resId)
        val outputStream = ByteArrayOutputStream()
        var size: Int
        var buffer = ByteArray(1024)
        try {
            while (ins.read(buffer, 0, 1024).also { size = it } >= 0) {
                outputStream.write(buffer, 0, size)
            }
            ins.close()
            buffer = outputStream.toByteArray()
            val filename = context!!.resources.getResourceEntryName(resId) + extension
            val fos = context!!.openFileOutput(filename, Context.MODE_PRIVATE)
            fos.write(buffer)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Returns an array containing resource ids read from the xml resource array whose resource id
     * is specified by our parameter `arrayResId`. We initialize `TypedArray ar` by
     * reading the array of array values with resource ID `arrayResId`, initialize `len`
     * to the length of `ar`, and allocate `len` ints for `int[] resIds`. We then
     * loop `len` times over `i` setting `resIds[ i ]` to the value of `ar`
     * at index `i` (defaulting to 0). When done we recycle `ar` and return `resIds`
     * to the caller.
     *
     * @param arrayResId resource ID of an xml resource array containing references to raw files.
     * @return array of resource ids
     */
    private fun getResourceIdArray(arrayResId: Int): IntArray {
        val ar = context!!.resources.obtainTypedArray(arrayResId)
        val len = ar.length()
        val resIds = IntArray(len)
        for (i in 0 until len) {
            resIds[i] = ar.getResourceId(i, 0)
        }
        ar.recycle()
        return resIds
    }

    /**
     * Dummy function to determine whether the user is logged in. We initialize our variable
     * `SharedPreferences sharedPreferences` with the `SharedPreferences` instance for
     * the name with resource id R.string.app_name ("StorageProvider"). Then we return the boolean
     * value stored under the key with resource id R.string.key_logged_in ("logged_in") defaulting to
     * false.
     *
     * @return true if the user is "logged in".
     */
    private val isUserLoggedIn: Boolean
        get() {
            val sharedPreferences = context!!.getSharedPreferences(context!!.getString(R.string.app_name),
                Context.MODE_PRIVATE)
            return sharedPreferences.getBoolean(context!!.getString(R.string.key_logged_in), false)
        }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "MyCloudProvider"

        /**
         * Use these as the default columns to return information about a root if no specific
         * columns are requested in a query.
         */
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        /**
         * Use these as the default columns to return information about a document if no specific
         * columns are requested in a query.
         */
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )

        /**
         * Maximum number of unexamined files to return. No official policy on how many to return,
         * but make sure you do limit the number of recent and search results.
         */
        private const val MAX_SEARCH_RESULTS = 20

        /**
         * Maximum number of recently modified files to return.
         */
        private const val MAX_LAST_MODIFIED = 5

        /**
         * Unique ID of our root.
         */
        private const val ROOT = "root"

        /**
         * Decides whether to use the DEFAULT_ROOT_PROJECTION instead of the `projection` passed us.
         *
         * @param projection the requested root column projection
         * @return either the requested root column projection, or the default projection if the
         * requested projection is null.
         */
        private fun resolveRootProjection(projection: Array<String>?): Array<String> {
            return projection ?: DEFAULT_ROOT_PROJECTION
        }

        /**
         * Decides whether to use the DEFAULT_DOCUMENT_PROJECTION instead of the `projection` passed us.
         *
         * @param projection the requested document column projection
         * @return either the requested document column projection, or the default projection if the
         * requested projection is null.
         */
        private fun resolveDocumentProjection(projection: Array<String>?): Array<String> {
            return projection ?: DEFAULT_DOCUMENT_PROJECTION
        }

        /**
         * Get a file's MIME type. If the file is a directory we return MIME_TYPE_DIR, otherwise we return
         * the mimetype guessed by our method `getTypeForName` for the name of the file.
         *
         * @param file the File object whose type we want
         * @return the MIME type of the file
         */
        private fun getTypeForFile(file: File?): String {
            return if (file!!.isDirectory) {
                DocumentsContract.Document.MIME_TYPE_DIR
            } else {
                getTypeForName(file.name)
            }
        }

        /**
         * Get the MIME data type of a document, given its filename. We initialize `int lastDot` by
         * finding the index of the last '.' character in our parameter `String name`. If `lastDot`
         * is greater than or equal to 0 (a '.' was found) we initialize `String extension` to the
         * substring in `name` that follows the '.'. We then initialize `String mime` to the
         * value returned by the `getMimeTypeFromExtension` method of `MimeTypeMap` for the
         * `extension`. If this is not null we return it to the caller. If we were unable to guess
         * the mimetype from the extension we return "application/octet-stream" to the caller.
         *
         * @param name the filename of the document
         * @return the MIME data type of a document
         */
        private fun getTypeForName(name: String): String {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1)
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) {
                    return mime
                }
            }
            return "application/octet-stream"
        }
    }
}