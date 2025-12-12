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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn") // Returning would just hide the bug

package com.example.android.actionbarcompat.shareactionprovider.content

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.database.Cursor
import android.net.Uri
import android.text.TextUtils
import java.io.FileNotFoundException
import java.io.IOException

/**
 * A simple [ContentProvider] which can serve files from this application's assets. The majority of
 * functionality is in [openAssetFile].
 */
class AssetProvider : ContentProvider() {
    /**
     * We implement this to initialize our content provider on startup. We just return `true`.
     *
     * @return `true` to indicate this provider was successfully loaded.
     */
    override fun onCreate(): Boolean {
        return true
    }

    /**
     * Implement this to handle requests to delete one or more rows. We do nothing, and return 0.
     *
     * @param uri The full [Uri] to query, including a row ID (if a specific record is requested).
     * @param selection An optional restriction to apply to rows when deleting.
     * @param selectionArgs an optional array of strings to replace "?" entries in `selection`
     * @return The number of rows affected.
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // Do not support delete requests.
        return 0
    }

    /**
     * Implement this to handle requests for the MIME type of the data at the
     * given URI. We do nothing, and return null.
     *
     * @param uri the [Uri] to query.
     * @return a MIME type string, or `null` if there is no type.
     */
    override fun getType(uri: Uri): String? {
        // Do not support returning the data type
        return null
    }

    /**
     * Implement this to handle requests to insert a new row. We do nothing and return `null`.
     *
     * @param uri The content:// [Uri] of the insertion request. This must not be `null`.
     * @param values A set of column_name/value pairs to add to the database.
     * This must not be `null`.
     * @return The [Uri] for the newly inserted item, we just return `null`.
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // Do not support insert requests.
        return null
    }

    /**
     * Implement this to handle query requests from clients. We do nothing and return `null`.
     *
     * @param uri The [Uri] to query. This will be the full [Uri] sent by the client;
     * if the client is requesting a specific record, the [Uri] will end in a record number
     * that the implementation should parse and add to a WHERE or HAVING clause, specifying
     * that _id value.
     * @param projection The list of columns to put into the [Cursor]. If
     * `null` all columns are included.
     * @param selection A selection criteria to apply when filtering rows.
     * If `null` then all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     * the values from [selectionArgs], in order that they appear in the selection.
     * The values will be bound as [String]'s.
     * @param sortOrder How the rows in the [Cursor] should be sorted.
     * If `null` then the provider is free to define the sort order.
     * @return a [Cursor] or `null`, we return `null`
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        // Do not support query requests.
        return null
    }

    /**
     * Implement this to handle requests to update one or more rows. We do nothing and return 0.
     *
     * @param uri The [Uri] to query. This can potentially have a record ID if this
     * is an update request for a specific record.
     * @param values A set of column_name/value pairs to update in the database.
     * This must not be `null`.
     * @param selection An optional filter to match rows to update.
     * @param selectionArgs An optional array of strings to replace "?" in `selection`
     * @return the number of rows affected.
     */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        // Do not support update requests.
        return 0
    }

    /**
     * This is like [openFile], but can be implemented by providers that need to be able to
     * return sub-sections of files, often assets inside of their .apk. We initialize our [String]
     * variable `val assetName` with the last segment in the path of our [Uri] parameter  [uri], then
     * if this is the empty string we throw [FileNotFoundException]. Otherwise, wrapped in a `try`
     * block intended to catch [IOException] we initialize our [AssetManager] variable  `val am` with
     * an instance for our application's package, and use it to open a file descriptor for `assetName`
     * which we return to the caller.
     *
     * @param uri The [Uri] whose file is to be opened.
     * @param mode Access mode for the file.  May be "r" for read-only access,
     * "w" for write-only access (erasing whatever data is currently in
     * the file), "wa" for write-only access to append to any existing data,
     * "rw" for read and write access on any existing data, and "rwt" for read
     * and write access that truncates any existing file.
     * @return Returns a new [AssetFileDescriptor] which you can use to access
     * the file.
     * @throws FileNotFoundException if the file is not found
     */
    @Throws(FileNotFoundException::class)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        // The asset file name should be the last path segment
        val assetName = uri.lastPathSegment

        // If the given asset name is empty, throw an exception
        if (TextUtils.isEmpty(assetName)) {
            throw FileNotFoundException()
        }
        return try {
            // Try and return a file descriptor for the given asset name
            val am: AssetManager = context!!.assets
            am.openFd(assetName!!)
        } catch (e: IOException) {
            e.printStackTrace()
            super.openAssetFile(uri, mode)
        }
    }

    companion object {
        /**
         * android:authorities value for this [ContentProvider]
         */
        var CONTENT_URI: String = "com.example.android.actionbarcompat.shareactionprovider"
    }
}