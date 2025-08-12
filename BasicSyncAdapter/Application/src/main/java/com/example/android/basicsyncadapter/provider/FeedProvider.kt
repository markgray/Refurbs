/*
 * Copyright 2013 The Android Open Source Project
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
@file:Suppress("DEPRECATION", "RedundantNullableReturnType", "ReplaceNotNullAssertionWithElvisReturn", "JoinDeclarationAndAssignment", "MemberVisibilityCanBePrivate")

package com.example.android.basicsyncadapter.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.BaseColumns
import com.example.android.common.db.SelectionBuilder
import androidx.core.net.toUri

/**
 * [ContentProvider] class specified as a provider in AndroidManifest.xml, with android:authorities
 * defined to be "com.example.android.basicsyncadapter"
 */
class FeedProvider : ContentProvider() {
    /**
     * SQLite backend instance.
     */
    var mDatabaseHelper: FeedDatabase? = null

    /**
     * Implement this to initialize your content provider on startup. We initialize our [FeedDatabase]
     * field [mDatabaseHelper] with a new instance, and return `true` to the caller to signify that
     * we have been successfully loaded.
     *
     * @return `true` if the provider was successfully loaded, `false` otherwise
     */
    override fun onCreate(): Boolean {
        mDatabaseHelper = FeedDatabase(context)
        return true
    }

    /**
     * Determine the mime type for entries returned by a given URI. First we initialize [Int]
     * variable `val match` by using [UriMatcher] field [sUriMatcher] to match our [Uri] parameter
     * [uri]. Then we `when` switch on `match`:
     *
     *  * [ROUTE_ENTRIES] - we return [FeedContract.Entry.CONTENT_TYPE] to the caller
     *  ("vnd.android.cursor.dir/vnd.basicsyncadapter.entries")
     *
     *  * [ROUTE_ENTRIES_ID] - we return [FeedContract.Entry.CONTENT_ITEM_TYPE] to the caller
     *  ("vnd.android.cursor.item/vnd.basicsyncadapter.entry")
     *
     *  * default - we throw [UnsupportedOperationException]
     *
     * @param uri the [Uri] to query.
     * @return a MIME type string, or `null` if there is no type.
     */
    override fun getType(uri: Uri): String? {
        val match = sUriMatcher.match(uri)
        return when (match) {
            ROUTE_ENTRIES -> FeedContract.Entry.CONTENT_TYPE
            ROUTE_ENTRIES_ID -> FeedContract.Entry.CONTENT_ITEM_TYPE
            else -> throw UnsupportedOperationException("Unknown uri: $uri")
        }
    }

    /**
     * Perform a database query by URI. First we initialize [SQLiteDatabase] variable `val db` by
     * using the [FeedDatabase.getReadableDatabase] method of [FeedDatabase] field [mDatabaseHelper]
     * to create or open our database. We initialize [SelectionBuilder] variable `val builder` with
     * a new instance, and initialize our "null varargs kludge" variable `val spreadFooler` to our
     * [Array] of [String] parameter [selectionArgs] if it is not `null` or to an empty [Array] of
     * [String] if it is `null`. We then set [Int] varible `val uriMatch` to the code returned when
     * we use the [UriMatcher.match] method of our [sUriMatcher] field to match our [Uri] parameter
     * [uri] and we then `when` switch on `uriMatch`:
     *
     *  * [ROUTE_ENTRIES_ID]: Returns a single entry, by ID. We initialize [String] variabe `val id`
     *  with the last path segment of [uri], then append a selection clause to `builder` for the
     *  unique ID for a row equal to "?", which is filled in by the selection argument `id`.
     *  We call the [SelectionBuilder.table] method of `builder` to set the Table name to use for
     *  the SQL FROM statement to [FeedContract.Entry.TABLE_NAME] ("entry"), and append a
     *  [SelectionBuilder.where] to the `builder` returned to Append our parameter [selection] with
     *  the "varargs spread" of `spreadFooler` as the selection clause. We then create [Cursor]
     *  variable `val c` using the [SelectionBuilder.query] method of `builder` for the database
     *  `db`, projection [projection], and sort order [sortOrder]. We initialize [Context] variable
     *  `val ctx` with the context this provider is running in, and register `c` with the content
     *  resolver to watch our content [Uri] parameter [uri] for changes (listener attached to this
     *  resolver will be notified). Finally we return `c` to the caller.
     *
     *  * [ROUTE_ENTRIES]: Return all known entries. We call the [SelectionBuilder.table] method of
     *  `builder` to set the Table name to use for  the SQL FROM statement to
     *  [FeedContract.Entry.TABLE_NAME] ("entry"), and append a [SelectionBuilder.where] to the
     *  `builder` returned to Append our parameter [selection] with the "varargs spread" of
     *  `spreadFooler` as the selection clause. We then create [Cursor] variable `val c` using the
     *  [SelectionBuilder.query] method of `builder` for the database `db`, projection [projection],
     *  and sort order [sortOrder]. We initialize [Context] variable `val ctx` with the context this
     *  provider is running in, and register `c` with the content resolver to watch our content
     *  [Uri] parameter [uri] for changes (listener attached to this resolver will be notified).
     *  Finally we return `c` to the caller
     *
     *  * default: We throw [UnsupportedOperationException].
     *
     *
     *
     * @param uri The [Uri] to query. This will be the full URI sent by the client; if the client
     * is requesting a specific record, the URI will end in a record number that the implementation
     * should parse and add to a WHERE or HAVING clause, specifying that _id value.
     * @param projection The list of columns to put into the cursor. If `null` all columns
     * are included.
     * @param selection A selection criteria to apply when filtering rows. If `null` then all rows
     * are included.
     * @param selectionArgs You may include ?s in [selection], which will be replaced by the values
     * from [selectionArgs], in order that they appear in the selection. The values will be bound as
     * [String]'s.
     * @param sortOrder How the rows in the cursor should be sorted. If `null` then the provider is
     * free to define the sort order.
     * @return a [Cursor] or `null`.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {

        val db: SQLiteDatabase = mDatabaseHelper!!.readableDatabase
        val builder = SelectionBuilder()
        val spreadFooler: Array<String> = selectionArgs ?: arrayOf()
        val uriMatch: Int = sUriMatcher.match(uri)
        return when (uriMatch) {
            ROUTE_ENTRIES_ID -> {
                // Return a single entry, by ID.
                val id: String? = uri.lastPathSegment
                builder.where(BaseColumns._ID + "=?", id)
                // Return all known entries.
                builder.table(FeedContract.Entry.TABLE_NAME)
                    .where(selection, *spreadFooler)
                val c: Cursor = builder.query(db, projection, sortOrder)
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                val ctx: Context = context!!
                c.setNotificationUri(ctx.contentResolver, uri)
                c
            }

            ROUTE_ENTRIES -> {
                builder.table(FeedContract.Entry.TABLE_NAME)
                    .where(selection, *spreadFooler)
                val c = builder.query(db, projection, sortOrder)
                val ctx = context!!
                c.setNotificationUri(ctx.contentResolver, uri)
                c
            }

            else -> throw UnsupportedOperationException("Unknown uri: $uri")
        }
    }

    /**
     * Insert a new entry into the database. First we initialize [SQLiteDatabase] variable `val db`
     * by using the [FeedDatabase.getWritableDatabase] method of [FeedDatabase] field [mDatabaseHelper]
     * to create or open our database. We set [Int] variable `val match` to the code returned when
     * we use the [UriMatcher.match] method of our [sUriMatcher] field to match our [Uri] parameter
     * [uri]. We declare [Uri] variable `var result` and we then `when` switch on `match`:
     *
     *  * [ROUTE_ENTRIES]: We set [Long] variable `val id` to the result returned by the
     *  [SQLiteDatabase.insertOrThrow] method of `db` when it inserts our [ContentValues]
     *  parameter [values] into the table `TABLE_NAME` ("entries"). We then set `result` to
     *  the [Uri] created by appending a "/" and the string value of `id` to TABLE_NAME.
     *
     *  * [ROUTE_ENTRIES_ID]: We throw [UnsupportedOperationException]
     *
     *  * default: We throw [UnsupportedOperationException]
     *
     * We initialize [Context] variable `val ctx` with the context this provider is running in, use
     * it to get an instance of [ContentResolver] in order to have it send a broadcast to registered
     * [ContentObserver]'s, to refresh the UI. Finally we return `result` to the caller.
     *
     * @param uri The `content://` [Uri] of the insertion request. This must not be `null`.
     * @param values A set of column_name/value pairs to add to the database. This must not be `null`.
     * @return The [Uri] for the newly inserted item.
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db: SQLiteDatabase = mDatabaseHelper!!.writableDatabase!!
        val match = sUriMatcher.match(uri)
        val result: Uri
        result = when (match) {
            ROUTE_ENTRIES -> {
                val id = db.insertOrThrow(FeedContract.Entry.TABLE_NAME, null, values)
                (FeedContract.Entry.CONTENT_URI.toString() + "/" + id).toUri()
            }

            ROUTE_ENTRIES_ID -> throw UnsupportedOperationException("Insert not supported on URI: $uri")
            else -> throw UnsupportedOperationException("Unknown uri: $uri")
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        val ctx: Context = context!!
        ctx.contentResolver.notifyChange(uri, null, false)
        return result
    }

    /**
     * Delete an entry in the database by [Uri]. First we initialize [SQLiteDatabase] variable
     * `val db` by using the [FeedDatabase.getWritableDatabase] method of [FeedDatabase] field
     * [mDatabaseHelper] to create or open our database. We initialize [SelectionBuilder] variable
     * `val builder` with a new instance, then set [Int] variable `val match` to the code returned
     * when we use the [UriMatcher.match] method of our [sUriMatcher] field to match our [Uri]
     * parameter [uri], we initialize our [Array] of [String] variable `val spreadFooler` to our
     * parameter [selectionArgs] if it is not `null` or to an empty [Array] if it is, we declare
     * [Int] variable `val count`,  and then switch on `match`:
     *
     *  * [ROUTE_ENTRIES]: We set `count` to the value returned by chaining to `builder` the
     *  commands to set the table to `TABLE_NAME`, set the selection to our parameter [selection]
     *  with the vararg selection arguments given by our variable `spreadFooler`, then calling the
     *  `delete` method of this `builder` with the [SQLiteDatabase] argument `db`.
     *
     *  * ROUTE_ENTRIES_ID: We initialize [String] variable `val id` with the last path segment of
     *  [uri]. We set `count` to the value returned by chaining to `builder` the commands to set the
     *  table to `TABLE_NAME`, the selection to the string formed by concatenating the "_id" column
     *  to the string "=?", with the selection arguments given by `id`, adding an additional `where`
     *  clause of our parameter [selection] with the varargs selection arguments provided by our
     *  variable `spreadFooler`, then calling the `delete` method of `builder` with the [SQLiteDatabase]
     *  argument `db`.
     *
     *  * default: We throw [UnsupportedOperationException]
     *
     * We initialize [Context] variable `val ctx` with the context this provider is running in, use
     * it to get an instance of [ContentResolver] in order to have it send a broadcast to registered
     * [ContentObserver]'s to refresh the UI. Finally we return `count` to the caller.
     *
     * @param uri The full [Uri] to query, including a row ID (if a specific record is requested).
     * @param selection An optional restriction to apply to rows when deleting.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     * from selectionArgs, in order that they appear in the selection. The values will be bound as
     * [String]'s.
     * @return The number of rows affected.
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db: SQLiteDatabase = mDatabaseHelper!!.writableDatabase
        val builder = SelectionBuilder()
        val match: Int = sUriMatcher.match(uri)
        val spreadFooler: Array<String> = selectionArgs ?: arrayOf()
        val count: Int
        count = when (match) {
            ROUTE_ENTRIES -> builder.table(FeedContract.Entry.TABLE_NAME)
                .where(selection, *spreadFooler)
                .delete(db)

            ROUTE_ENTRIES_ID -> {
                val id = uri.lastPathSegment
                builder.table(FeedContract.Entry.TABLE_NAME)
                    .where(BaseColumns._ID + "=?", id)
                    .where(selection, *spreadFooler)
                    .delete(db)
            }

            else -> throw UnsupportedOperationException("Unknown uri: $uri")
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        val ctx: Context = context!!
        ctx.contentResolver.notifyChange(uri, null, false)
        return count
    }

    /**
     * Update an entry in the database by [Uri]. First we initialize [SQLiteDatabase] variable
     * `val db` by using the [FeedDatabase.getWritableDatabase] method of [FeedDatabase] field
     * [mDatabaseHelper] to create or open our database. We initialize [SelectionBuilder] variable
     * `val builder` with a new instance, then set [Int] variable `val match` to the code returned
     * when we use the [UriMatcher.match] method of [sUriMatcher] to match our [Uri] parameter [uri]
     * `Uri `, we initialize our [Array] of [String] variable `val spreadFooler` to our parameter
     * [selectionArgs] if it is not `null` or to an empty [Array] if it is, we declare [Int] variable
     * `val count` and then `when` switch on `match`:
     *
     *  * [ROUTE_ENTRIES]: We set `count` to the value returned by chaining to `builder` the commands
     *  to set the table to `TABLE_NAME`, set the selection to our parameter [selection] with the
     *  varargs selection arguments given by our variable `spreadFooler`, then calling the `update`
     *  method of this `builder` with the [SQLiteDatabase] argument `db` and [ContentValues] argument
     *  `values`.
     *
     *  * [ROUTE_ENTRIES_ID]: We initialize [String] variable `val id` with the last path segment of
     *  [Uri] parameter [uri]. We set `count` to the value returned by chaining to `builder` the
     *  commands to set the table to `TABLE_NAME`, the selection to the string formed by concatenating
     *  the "_id" column to the string "=?", with the selection arguments given by `id`, adding our
     *  parameter [selection] with the varargs selection arguments our variable `spreadFooler`
     *  to the selection clause, then calling the `update` method of `builder` with the [SQLiteDatabase]
     *  argument `db` and [ContentValues] argument `values`.
     *
     *  * default: We throw [UnsupportedOperationException]
     *
     * We initialize [Context] variable `val ctx` with the context this provider is running in, use
     * it to get an instance of [ContentResolver] in order to have it send a broadcast to registered
     * [ContentObserver]'s to refresh the UI. Finally we return `count` to the caller.
     *
     * @param uri The URI to query. This can potentially have a record ID if this is an update request
     * for a specific record.
     * @param values A set of column_name/value pairs to update in the database. This must not be `null`.
     * @param selection An optional filter to match rows to update.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     * from selectionArgs, in order that they appear in the selection. The values will be bound as
     * [String]'s.
     * @return the number of rows affected.
     */
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val db: SQLiteDatabase = mDatabaseHelper!!.writableDatabase
        val builder = SelectionBuilder()
        val match: Int = sUriMatcher.match(uri)
        val spreadFooler: Array<String> = selectionArgs ?: arrayOf()
        val count: Int
        count = when (match) {
            ROUTE_ENTRIES -> builder.table(FeedContract.Entry.TABLE_NAME)
                .where(selection, *spreadFooler)
                .update(db, values)

            ROUTE_ENTRIES_ID -> {
                val id = uri.lastPathSegment
                builder.table(FeedContract.Entry.TABLE_NAME)
                    .where(BaseColumns._ID + "=?", id)
                    .where(selection, *spreadFooler)
                    .update(db, values)
            }

            else -> throw UnsupportedOperationException("Unknown uri: $uri")
        }
        val ctx = context!!
        ctx.contentResolver.notifyChange(uri, null, false)
        return count
    }

    /**
     * SQLite backend for [FeedProvider]. Provides access to an disk-backed, SQLite data-store which
     * is utilized by [FeedProvider]. This database should never be accessed by other parts of the
     * application directly.
     */
    class FeedDatabase
    /**
     * Our constructor. We call our super's constructor with [DATABASE_NAME] as the name of the
     * database file, and [DATABASE_VERSION] as the version.
     *
     * @param context Context the provider is running in.
     */
    (context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        /**
         * Called when the database is created for the first time. This is where the
         * creation of tables and the initial population of the tables should happen.
         *
         * We call the [SQLiteDatabase.execSQL] method of our parameter [db] to execute the single
         * SQL statement [SQL_CREATE_ENTRIES] which creates the "entry" table.
         *
         * @param db The database.
         */
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(SQL_CREATE_ENTRIES)
        }

        /**
         * Called when the database needs to be upgraded. We call the [SQLiteDatabase.execSQL]
         * method of our parameter [db] to execute the single SQL statement [SQL_DELETE_ENTRIES]
         * which deletes the "entry" table (SQL: "DROP TABLE IF EXISTS "), then call our [onCreate]
         * method to create the "entry" table.
         *
         * @param db The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES)
            onCreate(db)
        }

        companion object {
            /**
             * Schema version.
             */
            const val DATABASE_VERSION: Int = 1

            /**
             * Filename for SQLite file.
             */
            const val DATABASE_NAME: String = "feed.db"

            // Constants used to form SQL statements
            private const val TYPE_TEXT = " TEXT"
            private const val TYPE_INTEGER = " INTEGER"
            private const val COMMA_SEP = ","

            /**
             * SQL statement to create "entry" table.
             */
            private const val SQL_CREATE_ENTRIES = "CREATE TABLE " + FeedContract.Entry.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY," +
                FeedContract.Entry.COLUMN_NAME_ENTRY_ID + TYPE_TEXT + COMMA_SEP +
                FeedContract.Entry.COLUMN_NAME_TITLE + TYPE_TEXT + COMMA_SEP +
                FeedContract.Entry.COLUMN_NAME_LINK + TYPE_TEXT + COMMA_SEP +
                FeedContract.Entry.COLUMN_NAME_PUBLISHED + TYPE_INTEGER + ")"

            /**
             * SQL statement to drop "entry" table.
             */
            private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + FeedContract.Entry.TABLE_NAME
        }
    }

    companion object {
        /**
         * Content authority for this provider.
         */
        private const val AUTHORITY = FeedContract.CONTENT_AUTHORITY
        // The constants below represent individual URI routes, as IDs. Every URI pattern recognized
        // by this ContentProvider is defined using sUriMatcher.addURI(), and associated with one of
        // these IDs. When a incoming URI is run through sUriMatcher, it will be tested against the
        // defined URI patterns, and the corresponding route ID will be returned.

        /**
         * URI ID for route: /entries
         */
        const val ROUTE_ENTRIES: Int = 1

        /**
         * URI ID for route: /entries/{ID}
         */
        const val ROUTE_ENTRIES_ID: Int = 2

        /**
         * UriMatcher, used to decode incoming URIs.
         */
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        // Initialize UriMatcher sUriMatcher by adding Uri's to match the paths "entries"
        // (code ROUTE_ENTRIES) and "entries/*" (code ROUTE_ENTRIES_ID)
        init {
            sUriMatcher.addURI(AUTHORITY, "entries", ROUTE_ENTRIES)
            sUriMatcher.addURI(AUTHORITY, "entries/*", ROUTE_ENTRIES_ID)
        }
    }
}