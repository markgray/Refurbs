/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.contentprovidersample.provider

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.example.android.contentprovidersample.data.Cheese
import com.example.android.contentprovidersample.data.CheeseDao
import com.example.android.contentprovidersample.data.SampleDatabase
import androidx.core.net.toUri

/**
 * A [ContentProvider] based on a Room database.
 *
 * Note that you don't need to implement a [ContentProvider] unless you want to expose the data
 * outside your process or your application already uses a [ContentProvider].
 */
class SampleContentProvider : ContentProvider() {
    /**
     * Implement this to initialize your content provider on startup. We do nothing but return
     * `true` to the caller.
     *
     * @return `true` to indicate that the provider was successfully loaded
     */
    override fun onCreate(): Boolean {
        return true
    }

    /**
     * We implement this to handle query requests from clients. We initialize [Int] variable
     * `val code` by matching our [Uri] parameter [uri] using our [UriMatcher] field [MATCHER].
     * If the result is neither [CODE_CHEESE_DIR] or [CODE_CHEESE_ITEM] we throw an
     * [IllegalArgumentException], if it is one of these, we initialize [Context] variable
     * `val context` with the context that this provider is running in, and if that is `null` we
     * return `null` to the caller (we were called before our [onCreate] override was called). We
     * initialize [CheeseDao] variable `val cheese` with the DAO for the Cheese table, and declare
     * [Cursor] variable `val cursor`. If `code` is:
     *
     *  * [CODE_CHEESE_DIR] - we set `cursor` to the value returned by the [CheeseDao.selectAll]
     *  method of `cheese`.
     *
     *  * [CODE_CHEESE_ITEM] - we set `cursor` to the value returned by the [CheeseDao.selectById]
     *  method of `cheese` for the ID parsed from our [Uri] parameter [uri] by the method
     *  [ContentUris.parseId] (returns the long conversion of the last segment)
     *
     * We then call the [Cursor.setNotificationUri] method of `cursor` to Register with a
     * [ContentResolver] instance for our application's package to watch our content URI for changes.
     * Finally we return `cursor` to the caller.
     *
     * @param uri The URI to query. This will be the full URI sent by the client; if the client is
     * requesting a specific record, the URI will end in a record number that the implementation
     * should parse and add to a WHERE or HAVING clause, specifying that _id value.
     * @param projection The list of columns to put into the cursor. If `null` all columns are
     * included.
     * @param selection A selection criteria to apply when filtering rows. If `null` then all rows
     * are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     * from [selectionArgs], in the order that they appear in the selection. The values will be
     * bound as Strings.
     * @param sortOrder How the rows in the cursor should be sorted. If `null` then the provider is
     * free to define the sort order.
     * @return a Cursor or `null`.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val code = MATCHER.match(uri)
        return if (code == CODE_CHEESE_DIR || code == CODE_CHEESE_ITEM) {
            val context: Context = context ?: return null
            val cheese: CheeseDao = SampleDatabase.getInstance(context).cheese()
            val cursor: Cursor = if (code == CODE_CHEESE_DIR) {
                cheese.selectAll()
            } else {
                cheese.selectById(ContentUris.parseId(uri))
            }
            cursor.setNotificationUri(context.contentResolver, uri)
            cursor
        } else {
            throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * We implement this to handle requests for the MIME type of the data at the given URI. We
     * switch on the code returned when our [UriMatcher] field [MATCHER] matches our [Uri]
     * parameter [uri]:
     *
     *  * [CODE_CHEESE_DIR] - we return the string:
     *  "vnd.android.cursor.dir/com.example.android.contentprovidersample.provider.cheeses"
     *
     *  * [CODE_CHEESE_ITEM] - we return the string:
     *  "vnd.android.cursor.item/com.example.android.contentprovidersample.provider.cheeses"
     *
     *  * default - we throw an [IllegalArgumentException]
     *
     * @param uri the URI to query.
     * @return a MIME type string, or `null` if there is no type.
     */
    override fun getType(uri: Uri): String {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> "vnd.android.cursor.dir/" + AUTHORITY + "." + Cheese.TABLE_NAME
            CODE_CHEESE_ITEM -> "vnd.android.cursor.item/" + AUTHORITY + "." + Cheese.TABLE_NAME
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * We implement this to handle requests to insert a new row. We switch on the code returned when
     * our [UriMatcher] field [MATCHER] matches our [Uri] parameter [uri]:
     *
     *  * [CODE_CHEESE_DIR] - we initialize [Context] variable `val context` with the [Context] this
     *  provider is running in and if it is `null` we return `null` to the caller. If it is not
     *  `null` we get our instance of [SampleDatabase] and use it to get th [CheeseDao] for the
     *  Cheese table so we can use its [CheeseDao.insert] method to insert a [Cheese] object created
     *  from our [ContentValues] parameter [values], saving the ID returned by `insert` in our
     *  [Long] variable `val id`. We then fetch a [ContentResolver] instance for our application's
     *  package and call its [ContentResolver.notifyChange] method to Notify registered observers
     *  that a row in [uri] was updated and attempt to sync changes to the network. Finally we
     *  append `id` to the end of the path of [uri] and return it to the caller.
     *
     *  * [CODE_CHEESE_ITEM] - We throw an [IllegalArgumentException]
     *
     *  * default - We throw an [IllegalArgumentException]
     *
     * @param uri The content:// URI of the insertion request. This must not be `null`.
     * @param values A set of column_name/value pairs to add to the database.
     * This must not be `null`.
     * @return The [Uri] for the newly inserted item.
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> {
                val context: Context = context ?: return null
                val id: Long = SampleDatabase.getInstance(context).cheese()
                    .insert(Cheese.fromContentValues(values))
                context.contentResolver.notifyChange(uri, null)
                ContentUris.withAppendedId(uri, id)
            }

            CODE_CHEESE_ITEM -> throw IllegalArgumentException("Invalid URI, cannot insert with ID: $uri")
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * We implement this to handle requests to delete one or more rows. We switch on the code
     * returned when our [UriMatcher] field [MATCHER] matches our [Uri] parameter [uri]:
     *
     *  * [CODE_CHEESE_DIR] - we throw [IllegalArgumentException]
     *
     *  * [CODE_CHEESE_ITEM] - we initialize [Context] variable `val context` with the context that
     *  this provider is running in, and if that is `null` we return 0 to the caller. If it is not
     *  `null` we get our instance of [SampleDatabase] and use it to get the DAO for the Cheese
     *  table so we can use its [CheeseDao.deleteById] method to delete the [Cheese] object whose
     *  long id we parse from our [Uri] parameter [uri] using the method [ContentUris.parseId]
     *  saving the number of cheeses deleted in [Int] variable `val count`. We then fetch a
     *  [ContentResolver] instance for our application's package and call its
     *  [ContentResolver.notifyChange] method to Notify registered observers that a row in [uri]
     *  was updated and attempt to sync changes to the network. Finally we return `count` to the
     *  caller
     *
     *  * `else` - we throw [IllegalArgumentException]
     *
     * @param uri The full [Uri] to query, including a row ID (if a specific record is requested).
     * @param selection An optional restriction to apply to rows when deleting.
     * @param selectionArgs Optional arguments to replace "?" characters in [selection]
     * @return The number of rows affected.
     */
    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> {
                throw IllegalArgumentException("Invalid URI, cannot update without ID$uri")
            }
            CODE_CHEESE_ITEM -> {
                val context = context ?: return 0
                val count: Int = SampleDatabase.getInstance(context).cheese()
                    .deleteById(ContentUris.parseId(uri))
                context.contentResolver.notifyChange(uri, null)
                count
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * We implement this to handle requests to update one or more rows. We switch on the code
     * returned when our [UriMatcher] field [MATCHER] matches our [Uri] parameter [uri]:
     *
     *  * [CODE_CHEESE_DIR] - we throw [IllegalArgumentException]
     *
     *  * [CODE_CHEESE_ITEM] - we initialize [Context] variable `val context` with the context that
     *  this provider is running in, and if that is `null` we return 0 to the caller. If it is not
     *  `null` we initialize [Cheese] variable `val cheese` with the [Cheese] in our [ContentValues]
     *  parameter [values], and set its [Cheese.id] field to the long id parsed from our [Uri]
     *  parameter [uri] by the [ContentUris.parseId] method. We get our instance of [SampleDatabase]
     *  and use it to get the DAO for the Cheese table in order to use its [CheeseDao.update] method
     *  to update the entry in the database for `cheese` saving the number of cheeses updated in
     *  [Int] variable `val count` (should always be 1). We then fetch a [ContentResolver] instance
     *  for our application's package and call its [ContentResolver.notifyChange] method to Notify
     *  registered observers that a row in [uri] was updated and attempt to sync changes to the
     *  network. Finally we return `count` to the caller
     *
     *  * `else` - we throw [IllegalArgumentException]
     *
     * @param uri The URI to query. This can potentially have a record ID if this is an update
     * request for a specific record.
     * @param values A set of column_name/value pairs to update in the database. This must not be
     * `null`.
     * @param selection An optional filter to match rows to update.
     * @param selectionArgs Optional arguments to replace "?" characters in [selection]
     * @return the number of rows affected.
     */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> {
                throw IllegalArgumentException("Invalid URI, cannot update without ID$uri")
            }
            CODE_CHEESE_ITEM -> {
                val context: Context = context ?: return 0
                val cheese: Cheese = Cheese.fromContentValues(values)
                cheese.id = ContentUris.parseId(uri)
                val count: Int = SampleDatabase.getInstance(context).cheese()
                    .update(cheese)
                context.contentResolver.notifyChange(uri, null)
                count
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * We Override this to handle requests to perform a batch of operations. We initialize our
     * [Context] variable `val context` with the context this provider is running in. We initialize
     * [SampleDatabase] variable `val database` with our apps [SampleDatabase] instance and call its
     * [SampleDatabase.beginTransaction] method. Then in a `try` block which ignores exceptions we
     * initialize [Array] of  [ContentProviderResult] `val result` with the result returned by our
     * super's implementation of `applyBatch`. We then call the [SampleDatabase.setTransactionSuccessful]
     * method of `database` to mark the current transaction as successful, and return `result` to
     * the caller.
     *
     * @param operations the operations to apply
     * @return the results of the applications
     */
    @Throws(OperationApplicationException::class)
    override fun applyBatch(
        operations: ArrayList<ContentProviderOperation>
    ): Array<ContentProviderResult> {
        val context: Context? = context
        val database: SampleDatabase = SampleDatabase.getInstance(context!!)
        @Suppress("DEPRECATION")
        database.beginTransaction()
        return try {
            val result: Array<ContentProviderResult> = super.applyBatch(operations)
            @Suppress("DEPRECATION")
            database.setTransactionSuccessful()
            result
        } finally {
            @Suppress("DEPRECATION")
            database.endTransaction()
        }
    }

    /**
     * Override this to handle requests to insert a set of new rows. We switch on the code returned
     * when our [UriMatcher] field [MATCHER] matches our [Uri] parameter [uri]:
     *
     *  * [CODE_CHEESE_DIR] - We initialize our [Context] variable `val context` with the context
     *  this provider is running in, and if that is `null` we return 0 to the caller. We initialize
     *  [SampleDatabase] variable `val database` with our apps [SampleDatabase] instance, then
     *  initialize our [MutableList] of [Cheese] variable `val cheeseList` to an [ArrayList]. Then
     *  we fill it with [Cheese] objects created from each of the entries in [Array] of [ContentValues]
     *  parameter [valuesArray]. Then we retrieve the DAO of the cheese table in `database` in order
     *  to call its [CheeseDao.insertAll] method with the [Array] of [Cheese] objects created by the
     *  [toTypedArray] method of `cheeseList` and return the length of the result it returns to the
     *  caller.
     *
     *  * [CODE_CHEESE_ITEM] - we throw [IllegalArgumentException]
     *
     *  * `else` - we throw [IllegalArgumentException]
     *
     * @param uri The content:// URI of the insertion request.
     * @param valuesArray An array of sets of column_name/value pairs to add to the database.
     * This must not be `null`.
     * @return The number of values that were inserted.
     */
    override fun bulkInsert(uri: Uri, valuesArray: Array<ContentValues>): Int {
        return when (MATCHER.match(uri)) {
            CODE_CHEESE_DIR -> {
                val context: Context = context ?: return 0
                val database: SampleDatabase = SampleDatabase.getInstance(context)
                val cheeseList: MutableList<Cheese> = ArrayList()
                for (i in valuesArray.indices) {
                    cheeseList.add(Cheese.fromContentValues(valuesArray[i]))
                }
                database.cheese().insertAll(cheeseList.toTypedArray()).size
            }

            CODE_CHEESE_ITEM -> {
                throw IllegalArgumentException("Invalid URI, cannot insert with ID: $uri")
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    companion object {
        /**
         * The authority of this content provider.
         */
        const val AUTHORITY: String = "com.example.android.contentprovidersample.provider"

        /**
         * The URI for the Cheese table:
         * "content://com.example.android.contentprovidersample.provider/cheeses"
         */
        val URI_CHEESE: Uri = ("content://" + AUTHORITY + "/" + Cheese.TABLE_NAME).toUri()

        /**
         * The match code for some items in the Cheese table.
         */
        private const val CODE_CHEESE_DIR = 1

        /**
         * The match code for an item in the Cheese table.
         */
        private const val CODE_CHEESE_ITEM = 2

        /**
         * The URI matcher.
         */
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)

        init {
            MATCHER.addURI(
                /* authority = */ AUTHORITY,
                /* path = */ Cheese.TABLE_NAME,
                /* code = */ CODE_CHEESE_DIR
            )
            MATCHER.addURI(
                /* authority = */ AUTHORITY,
                /* path = */ Cheese.TABLE_NAME + "/*",
                /* code = */ CODE_CHEESE_ITEM
            )
        }
    }
}