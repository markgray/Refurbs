/*
 * Copyright (C) 2007 The Android Open Source Project
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
@file:Suppress(
    "RedundantNullableReturnType",
    "ReplaceNotNullAssertionWithElvisReturn",
    "JoinDeclarationAndAssignment",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.notepad

import android.content.ClipDescription
import android.content.ContentProvider
import android.content.ContentProvider.PipeDataWriter
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.UnsupportedEncodingException

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
class NotePadProvider : ContentProvider(), PipeDataWriter<Cursor> {
    /**
     * A test package can call this to get a handle to the database underlying NotePadProvider,
     * so it can insert test data into the database. The test case class is responsible for
     * instantiating the provider in a test context; `android.test.ProviderTestCase2` does
     * this during the call to setUp()
     */
    var openHelperForTest: DatabaseHelper? = null
        private set

    /**
     * This class helps open, create, and upgrade the database file. Set to package visibility
     * for testing purposes. We just call our super's 4 argument constructor, using [DATABASE_NAME]
     * ("note_pad.db") as the database name, `null` to use the default cursor factory, and
     * [DATABASE_VERSION] (2) as the version number of the database.
     *
     * @param context the [Context] to use for locating paths to the the database
     */
    class DatabaseHelper(
        context: Context?
    ) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        /**
         * Called when the database is created for the first time. Creates the underlying database
         * with table name and column names taken from the [NotePad] class. We call the
         * [SQLiteDatabase.execSQL] method of our [SQLiteDatabase] parameter [db] to have it
         * execute the SQL statement:
         * CREATE TABLE notes (_id INTEGER PRIMARY KEY,title TEXT,note TEXT,created INTEGER,modified INTEGER);
         *
         * This creates a table with the name [NotePad.Notes.TABLE_NAME] ("notes"), with
         * [BaseColumns._ID] ("_id") as an INTEGER column to be used as the "PRIMARY KEY",
         * [NotePad.Notes.COLUMN_NAME_TITLE] ("title") as a TEXT column,
         * [NotePad.Notes.COLUMN_NAME_NOTE] ("note") as a TEXT column,
         * [NotePad.Notes.COLUMN_NAME_CREATE_DATE] ("created") as an INTEGER column, and
         * [NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE] ("modified") as an INTEGER column.
         *
         * @param db The database.
         */
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                    + ");"
            )
        }

        /**
         * Called when the database needs to be upgraded. Demonstrates that the provider must consider
         * what happens when the underlying data store is changed. In this sample, the database is
         * upgraded by destroying the existing data. A real application should upgrade the database
         * in place. We log what we are doing, then call the [SQLiteDatabase.execSQL] of our
         * [SQLiteDatabase] parameter [db] to have it execute the command "DROP TABLE IF EXISTS notes"
         * which removes the table "notes" from the database schema if it exists there. Then we call our
         * [onCreate] method to create the table used by the new version.
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

            // Logs that the database is being upgraded
            Log.w(
                TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data"
            )

            // Kills the table and existing data
            db.execSQL("DROP TABLE IF EXISTS notes")

            // Recreates the database with a new version
            onCreate(db)
        }
    }

    /**
     * We implement this to initialize our content provider on startup. Initializes the provider by
     * creating a new [DatabaseHelper]. [onCreate] is called automatically when Android creates the
     * provider in response to a resolver request from a client. This method is called for all
     * registered content providers on the  application main thread at application launch time.
     * It must not perform lengthy operations, or application startup will be delayed.
     *
     * We initialize our [DatabaseHelper] field [openHelperForTest] with a new instance and return
     * `true` to the caller to indicate that the provider was successfully loaded (any failures will
     * be reported by a thrown exception in the framework).
     *
     * @return `true` if the provider was successfully loaded, `false` otherwise
     */
    override fun onCreate(): Boolean {

        // Creates a new helper object. Note that the database itself isn't opened until
        // something tries to access it, and it's only created if it doesn't already exist.
        openHelperForTest = DatabaseHelper(context)

        // Assumes that any failures will be reported by a thrown exception.
        return true
    }

    /**
     * We implement this to handle query requests from clients that they issue by calling the
     * [ContentResolver.query] method of [ContentResolver] with a URI that we are responsible for.
     * Queries the database and returns a [Cursor] containing the results.
     *
     * First we initialize [SQLiteQueryBuilder] variable `val qb` with a new instance, and set the
     * list of tables for it to query to [NotePad.Notes.TABLE_NAME] ("notes"). Then we switch on
     * the value returned by the [UriMatcher.match] method of [UriMatcher] field [sUriMatcher] when
     * matching its patterns against our [Uri] parameter [uri]:
     *
     *  * NOTES: (URIs terminated with "notes") We call the [SQLiteQueryBuilder.setProjectionMap]
     *  method of `qb` to set the projection map for the query to [HashMap] of [String] to [String]
     *  field [sNotesProjectionMap] (Which just maps all the column name strings to themselves).
     *
     *  * [NOTE_ID]: (URIs terminated with "notes" plus a wild card integer) We call the
     *  [SQLiteQueryBuilder.setProjectionMap] method of `qb` to set the projection map for the query
     *  to [HashMap] of [String] to [String] field [sNotesProjectionMap] (Which just maps all the
     *  column name strings to themselves), then we call the [SQLiteQueryBuilder.appendWhere] method
     *  of `qb` to append a WHERE clause to the query consisting of the string formed by concatenating
     *  the name of the ID column [BaseColumns._ID] ("_id") to the string "=" followed by the note
     *  ID parsed from our [Uri] parameter [uri] by the method [Uri.getPathSegments] (it returns a
     *  list of path segments minus any "/" and the one we want is in position
     *  [NotePad.Notes.NOTE_ID_PATH_POSITION] (1).
     *
     *  * default: If the URI doesn't match any of the known patterns, throw an [IllegalArgumentException].
     *
     * We declare [String] variable `val orderBy` and if our [String] parameter [sortOrder] is empty
     * (or `null`) we set `orderBy` to [NotePad.Notes.DEFAULT_SORT_ORDER] ("modified DESC", descending
     * on the "modified" column) otherwise we set `orderBy` to [sortOrder]. We then initialize
     * [SQLiteDatabase] variable `val db` to the readable [SQLiteDatabase] returned by the
     * [DatabaseHelper.getReadableDatabase] method of [DatabaseHelper] field [openHelperForTest],
     * and initialize [Cursor] variable `val c` to the value returned by the [SQLiteQueryBuilder.query]
     * method of [SQLiteQueryBuilder] `qb` querying [SQLiteDatabase] `db` for the columns
     * in our [Array] of [String] parameter [projection], the row selection specified by our [String]
     * parameter [selection], the selection arguments for any "?" characters in [selection] specified
     * by our [Array] of [String] parameter [selectionArgs], `null` for row grouping (no grouping of
     * rows), with `null` for the "having" filter so that the row groups are not filtered, and
     * [String] `orderBy` for the sort order. Then we call the [Cursor.setNotificationUri] method of
     * [Cursor] `c` to have it watch the URI in [Uri] parameter [uri] in case the source data changes.
     * Finally we return `c` to the caller.
     *
     * @param uri The URI to query. This will be the full URI sent by the client; if the client is
     * requesting a specific record, the URI will end in a record number that the implementation
     * should parse and add to a WHERE or HAVING clause, specifying that _id value.
     * @param projection The list of columns to put into the cursor. If `null` all columns
     * are included.
     * @param selection A selection criteria to apply when filtering rows. If `null` then all rows
     * are included.
     * @param selectionArgs You may include "?" characters in [selection], which will be replaced by
     * the values from [selectionArgs], in the order that they appear in the selection. The values
     * will be bound as Strings.
     * @param sortOrder How the rows in the cursor should be sorted. If `null` then the provider is
     * free to define the sort order.
     * @return A [Cursor] containing the results of the query. The [Cursor] exists but is empty if
     * the query returns no results or an exception occurs.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {

        // Constructs a new query builder and sets its table name
        val qb = SQLiteQueryBuilder()
        qb.tables = NotePad.Notes.TABLE_NAME
        when (sUriMatcher!!.match(uri)) {
            NOTES -> qb.projectionMap = sNotesProjectionMap
            NOTE_ID -> {
                qb.projectionMap = sNotesProjectionMap
                qb.appendWhere(
                    BaseColumns._ID +  // the name of the ID column
                        "=" +  // the position of the note ID itself in the incoming URI
                        uri.pathSegments[NotePad.Notes.NOTE_ID_PATH_POSITION]
                )
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        val orderBy: String?
        // If no sort order is specified, uses the default
        orderBy = if (TextUtils.isEmpty(sortOrder)) {
            NotePad.Notes.DEFAULT_SORT_ORDER
        } else {
            // otherwise, uses the incoming sort order
            sortOrder
        }

        // Opens the database object in "read" mode, since no writes need to be done.
        val db: SQLiteDatabase = openHelperForTest!!.readableDatabase

        /*
         * Performs the query. If no problems occur trying to read the database, then a Cursor
         * object is returned; otherwise, the cursor variable contains null. If no records were
         * selected, then the Cursor object is empty, and Cursor.getCount() returns 0.
         */
        val c = qb.query(
            db,  // The database to query
            projection,  // The columns to return from the query
            selection,  // The columns for the where clause
            selectionArgs,  // The values for the where clause
            null,  // don't group the rows
            null,  // don't filter by row groups
            orderBy // The sort order
        )

        // Tells the Cursor what URI to watch, so it knows when its source data changes
        c.setNotificationUri(context!!.contentResolver, uri)
        return c
    }

    /**
     * This is called when a client calls [ContentResolver.getType]. Returns the
     * MIME data type of the URI given in [Uri] parameter [uri]. We `when` switch on the value
     * returned by the [UriMatcher.match] method of [UriMatcher] field [sUriMatcher] when matching
     * its patterns against our [Uri] parameter [uri]:
     *
     *  * [NOTES]: (URIs terminated with "notes") we return [NotePad.Notes.CONTENT_TYPE]
     * ("vnd.android.cursor.dir/vnd.google.note") to the caller.
     *
     *  * [NOTE_ID]: (URIs terminated with "notes" plus a wild card integer) we return
     *  [NotePad.Notes.CONTENT_ITEM_TYPE] ("vnd.android.cursor.item/vnd.google.note") to
     *  the caller.
     *
     *  * else: If the URI doesn't match any of the known patterns, throw an [IllegalArgumentException].
     *
     * @param uri The URI whose MIME type is desired.
     * @return The MIME type of the URI.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    override fun getType(uri: Uri): String? {
        /*
         * Chooses the MIME type based on the incoming URI pattern
         */
        return when (sUriMatcher!!.match(uri)) {
            NOTES -> NotePad.Notes.CONTENT_TYPE
            NOTE_ID -> NotePad.Notes.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
    }

    /**
     * Returns the types of available data streams. URIs to specific notes are supported. The
     * application can convert such a note to a plain text stream. We when switch on the value
     * returned by the [UriMatcher.match] method of [UriMatcher] field [sUriMatcher] when matching
     * its patterns against our [Uri] parameter [uri]:
     *
     *  * [NOTES]: If the pattern is for notes or live folders, return `null`. Data streams are not
     *  supported for this type of URI.
     *
     *  * [NOTE_ID]: If the pattern is for note IDs and the MIME filter is text/plain, then return
     *  text/plain, which we do by returning the values returned by the [ClipDescription.filterMimeTypes]
     *  method of [ClipDescription] field [NOTE_STREAM_TYPES] for the filter in [String] parameter
     *  [mimeTypeFilter] (since [NOTE_STREAM_TYPES] contains only MIMETYPE_TEXT_PLAIN ("text/plain")
     *  it will return that or `null` if the filter does not match it).
     *
     *  * `else`: We throw [IllegalArgumentException]
     *
     * @param uri the URI to analyze
     * @param mimeTypeFilter The MIME type to check for. This method only returns a data stream
     * type for MIME types that match the filter. Currently, only text/plain MIME types match.
     * @return a data stream MIME type. Currently, only text/plan is returned.
     * @throws IllegalArgumentException if the URI pattern doesn't match any supported patterns.
     */
    override fun getStreamTypes(uri: Uri, mimeTypeFilter: String): Array<String>? {
        /*
         *  Chooses the data stream type based on the incoming URI pattern.
         */
        return when (sUriMatcher!!.match(uri)) {
            NOTES -> null
            NOTE_ID -> NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter)
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
    }

    /**
     * Returns a stream of data for each supported stream type. This method does a query on the
     * incoming URI, then uses [openPipeHelper] to start another thread in which to convert
     * the data into a stream. First we initialize [Array] of [String] variable `val mimeTypes`
     * with the array of supported mime types that our [getStreamTypes] method returns that match
     * our [Uri] parameter [uri] and [String] parameter [mimeTypeFilter]. If this is `null` then
     * we return the read only handle to the file that is returned by our super's implementation
     * of `openTypedAssetFile`. If it is not `null` we initialize [Cursor] variable `val c` to the
     * value returned by the [query] method when reading the note pointed to by our [Uri] parameter
     * [uri] for the columns given in [READ_NOTE_PROJECTION] (ID, title, and contents), with `null`
     * for selection (all records), `null` for selection arguments (no need since no selection), and
     * `null` for the sort order (use the default sort order: modification date, descending). If `c`
     * is `null` (the query fails) or its [Cursor.moveToFirst] method returns false (the cursor is
     * empty) we close the cursor if it is not `null`, and throw [FileNotFoundException]. If
     * everything has worked we initialize [ParcelFileDescriptor] variable `val fd` with the
     * instance returned by the [openPipeHelper] method when it creates a data pipe and background
     * thread that will allow the read end of the pipe (`fd`) to be used to stream data back to the
     * caller. Finally we return an [AssetFileDescriptor] constructed from `fd` to our caller.
     *
     * @param uri The URI pattern that points to the data stream
     * @param mimeTypeFilter A String containing a MIME type. This method tries to get a stream of
     * data with this MIME type.
     * @param opts Additional options supplied by the caller. Can be interpreted as desired by the
     * content provider.
     * @return AssetFileDescriptor A handle to the file.
     * @throws FileNotFoundException if there is no file associated with the incoming URI.
     */
    @Throws(FileNotFoundException::class)
    override fun openTypedAssetFile(
        uri: Uri,
        mimeTypeFilter: String,
        opts: Bundle?
    ): AssetFileDescriptor? {

        // Checks to see if the MIME type filter matches a supported MIME type.
        val mimeTypes: Array<String>? = getStreamTypes(uri, mimeTypeFilter)

        // If the MIME type is supported
        if (mimeTypes != null) {

            // Retrieves the note for this URI. Uses the query method defined for this provider,
            // rather than using the database query method.
            val c = query(
                uri,  // The URI of a note
                READ_NOTE_PROJECTION,  // Gets a projection containing the note's ID, title, and contents
                null,  // No WHERE clause, get all matching records
                null,  // Since there is no WHERE clause, no selection criteria
                null // Use the default sort order: modification date, descending
            )


            // If the query fails or the cursor is empty, stop
            if (c == null || !c.moveToFirst()) {

                // If the cursor is empty, simply close the cursor and return
                c?.close()
                throw FileNotFoundException("Unable to query $uri")
            }

            // Start a new thread that pipes the stream data back to the caller.
            val fd: ParcelFileDescriptor = openPipeHelper(
                uri,  // The URI whose data is to be written.
                mimeTypes[0],  // The desired type of data to be written.
                opts,  // Options supplied by caller.
                c,  // Our cursor
                this // Interface implementing the function that will actually stream the data.
            )
            return AssetFileDescriptor(
                fd,  // The underlying file descriptor
                0,  // The location within the file that the asset starts.
                AssetFileDescriptor.UNKNOWN_LENGTH // The number of bytes of the asset, UNKNOWN_LENGTH
            )
        }

        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts)
    }

    /**
     * Called from a background thread to stream data out to a pipe. Note that the pipe is blocking,
     * so this thread can block on writes for an arbitrary amount of time if the client is slow at
     * reading. Performs the actual work of converting the data in the cursor passed it to a stream
     * of data for the client to read.
     *
     * We initialize [FileOutputStream] variable `val fout` with an instance constructed from the
     * [FileDescriptor] associated with our [ParcelFileDescriptor] parameter [output]. We initialize
     * [PrintWriter] variable `var pw` to `null`. Then wrapped in a try block intended to catch and
     * log [UnsupportedEncodingException] we set `pw` to an instance constructed from an
     * [OutputStreamWriter] that is constructed to write to `fout` using the character set "UTF-8".
     * We then write a line to `pw` that contains the string in [Cursor] parameter [c] at column
     * index [READ_NOTE_TITLE_INDEX] (2, the notes title), then print a line containing only the
     * empty string followed by a line that contains the string in [c] at column index
     * [READ_NOTE_NOTE_INDEX] (1, the note's content column). In the finally block for the try we
     * close [c], and if `pw` is not `null` we flush it before wrapped in a try block intended to
     * catch and log [IOException] we close `fout`.
     *
     * @param output The pipe where data should be written. This will be closed for you upon
     * returning from this function.
     * @param uri The URI whose data is to be written.
     * @param mimeType The desired type of data to be written.
     * @param opts Options supplied by caller.
     * @param c Your own custom arguments, in our case a [Cursor].
     */
    override fun writeDataToPipe(
        output: ParcelFileDescriptor,
        uri: Uri,
        mimeType: String,
        opts: Bundle?,
        c: Cursor?
    ) {
        // We currently only support conversion-to-text from a single note entry,
        // so no need for cursor data type checking here.
        val fout = FileOutputStream(output.fileDescriptor)
        var pw: PrintWriter? = null
        try {
            pw = PrintWriter(OutputStreamWriter(fout, "UTF-8"))
            pw.println(c!!.getString(READ_NOTE_TITLE_INDEX))
            pw.println("")
            pw.println(c.getString(READ_NOTE_NOTE_INDEX))
        } catch (e: UnsupportedEncodingException) {
            Log.w(TAG, "Oops", e)
        } finally {
            c!!.close()
            pw?.flush()
            try {
                fout.close()
            } catch (_: IOException) {
                Log.w(TAG, "IOException closing FileOutputStream")
            }
        }
    }

    /**
     * This is called when a client calls [ContentResolver.insert] with a [Uri] pointing to where
     * its [ContentValues] should be inserted. Inserts a new row into the database. This method sets
     * up default values for any columns that are not included in the incoming map. If rows were
     * inserted, then listeners are notified of the change.
     *
     * If [UriMatcher] field [sUriMatcher] does not match our [Uri] parameter [uri] to be a [NOTES]
     * URI we throw IllegalArgumentException. If it does match we declare [ContentValues] variable
     * `val values` and if our [ContentValues] parameter is not `null` we set `values` to an
     * instance whose values are copied from [initialValues], if it is `null` we set `values` to a
     * new instance.
     *
     * We initialize [Long] variable `val now` to the current system time in milliseconds. If the
     * `values` map does not already contain the creation date (under the key
     * [NotePad.Notes.COLUMN_NAME_CREATE_DATE]) we add `now` to `values` under the key
     * [NotePad.Notes.COLUMN_NAME_CREATE_DATE]. If the `values` map does not already contain the
     * modification date (under the key [NotePad.Notes.COLUMN_NAME_CREATE_DATE]) we add `now` to
     * `values` under the key [NotePad.Notes.COLUMN_NAME_CREATE_DATE]. If the `values` map does not
     * already contain the note title (under the key [NotePad.Notes.COLUMN_NAME_TITLE]) we add the
     * string with the resource id [android.R.string.untitled] (&lt;Untitled&gt;) to `values` under
     * the key [NotePad.Notes.COLUMN_NAME_TITLE]. If the `values` map does not already contain the
     * note text (under the key [NotePad.Notes.COLUMN_NAME_NOTE]) we add the empty string ("") to
     * `values` under the key [NotePad.Notes.COLUMN_NAME_NOTE].
     *
     * We initialize [SQLiteDatabase] variable `val db` by opening [DatabaseHelper] field
     * [openHelperForTest] in write mode. We then set [Long] variable `val rowId` to the row id
     * returned by the [SQLiteDatabase.insert] method of `db` when inserting `values` into its table
     * [NotePad.Notes.TABLE_NAME] ("notes"). The nullable column name [NotePad.Notes.COLUMN_NAME_NOTE]
     * ("note") is included to get around the fact that SQLite won't insert the row if no columns are
     * contained in `values` (a hack). If `rowId` is greater than 0 we initialize [Uri] variable
     * `val noteUri` to the [Uri] produced by appending the id `rowId` to
     * [NotePad.Notes.CONTENT_ID_URI_BASE] ("content://com.google.provider.NotePad/notes/"). We then
     * retrieve the [Context] this provider is running in, fetch from it a [ContentResolver] instance
     * for our application's package and call its [ContentResolver.notifyChange] method to notify
     * registered observers that the row with URI `noteUri` has been updated. Finally we return
     * `noteUri` to the caller. If on the other hand `rowId` is not greater than 0 we throw
     * [SQLException].
     *
     * @param uri The content:// URI of the insertion request. This must not be `null`.
     * @param initialValues A set of column_name/value pairs to add to the database.
     * This must not be `null`.
     * @return The [Uri] for the newly inserted row.
     * @throws SQLException if the insertion fails.
     */
    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {

        // Validates the incoming URI. Only the full provider URI is allowed for inserts.
        require(sUriMatcher!!.match(uri) == NOTES) { "Unknown URI $uri" }

        // A map to hold the new record's values.
        val values: ContentValues

        // If the incoming values map is not null, uses it for the new values.
        values = initialValues?.let { ContentValues(it) } ?: // Otherwise, create a new value map
            ContentValues()

        // Gets the current system time in milliseconds
        val now: Long = System.currentTimeMillis()

        // If the values map doesn't contain the creation date, sets the value to the current time.
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now)
        }

        // If the values map doesn't contain the modification date, sets the value to the current
        // time.
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now)
        }

        // If the values map doesn't contain a title, sets the value to the default title.
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
            val r = Resources.getSystem()
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled))
        }

        // If the values map doesn't contain note text, sets the value to an empty string.
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "")
        }

        // Opens the database object in "write" mode.
        val db: SQLiteDatabase = openHelperForTest!!.writableDatabase

        // Performs the insert and returns the ID of the new note.
        val rowId = db.insert(
            NotePad.Notes.TABLE_NAME,  // The table to insert into.
            NotePad.Notes.COLUMN_NAME_NOTE,  // A hack, SQLite sets this column value to null if values is empty.
            values // A map of column names, and the values to insert into the columns.
        )

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID appended to it.
            val noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId)

            // Notifies observers registered against this provider that the data changed.
            context!!.contentResolver.notifyChange(noteUri, null)
            return noteUri
        }
        throw SQLException("Failed to insert row into $uri")
    }

    /**
     * This is called when a client calls [ContentResolver.delete] to delete records from the
     * database. If the incoming URI matches the note ID URI pattern, this method deletes the one
     * record specified by the ID in the URI. Otherwise, it deletes a set of records. The record or
     * records must also match the input selection criteria specified by [String] parameter [where]
     * and [Array] of [String] parameter [whereArgs]. If rows were deleted, then listeners are
     * notified of the change.
     *
     * First we initialize [SQLiteDatabase] variable `val db` to the writeable database returned by
     * the [DatabaseHelper.getWritableDatabase] method of [openHelperForTest]. We declare [String]
     * variable `var finalWhere`, and [Int] variable `val count`. Then we `when` switch on the value
     * returned by the [UriMatcher.match] method of [UriMatcher] field [sUriMatcher] when matching
     * its patterns against our [Uri] parameter [uri]:
     *
     *  * [NOTES]: (the incoming pattern matches the general pattern for notes, so we do a delete
     *  based on the incoming [where] columns and [whereArgs] arguments) We save the number of rows
     *  affected in `count` from a call to the [SQLiteDatabase.delete] method of `db` when passed
     *  the table name [NotePad.Notes.TABLE_NAME] ("notes"), our [String] parameter [where] as the
     *  `WHERE` clause, and our [Array] of [String] parameter [whereArgs] as the where arguments.
     *
     *  * [NOTE_ID]: (the incoming URI matches a single note ID, do the delete based on the incoming
     *  data, but modify the where clause to restrict it to the particular note ID) We set
     *  `finalWhere` to the string formed by concatenating the ID column name [BaseColumns._ID]
     *  ("_id") followed by the string " = " (test for equality) followed by the note id number
     *  extracted from our [Uri] parameter [uri] from the [NotePad.Notes.NOTE_ID_PATH_POSITION]
     *  position in the path segments that its [Uri.getPathSegments] method (kotlin `pathSegments`
     *  property) parses from it. If our [String] parameter [where] is not `null` we append the
     *  string " AND " followed by [where] to `finalWhere`. Now we save the number of rows affected
     *  in `count` from a call to the [SQLiteDatabase.delete] method of `db` when passed the table
     *  name [NotePad.Notes.TABLE_NAME] ("notes"), [String] variable `finalWhere` as the WHERE
     *  clause, and our [Array] of [String] parameter [whereArgs] as the where arguments.
     *
     *  * `else`: (the incoming pattern is invalid) We throw [IllegalArgumentException]
     *
     * We retrieve the [Context] this provider is running in and use it to fetch a [ContentResolver]
     * instance for our application's package whose [ContentResolver.notifyChange] method we then
     * call to notify registered observers that a row was updated in [uri]. Finally we return `count`
     * (the number of rows deleted) to the caller.
     *
     * @param uri The full URI to query, including a row ID (if a specific record is requested).
     * @param whereArgs An optional restriction to apply to rows when deleting.
     * @return The number of rows affected.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {

        // Opens the database object in "write" mode.
        val db: SQLiteDatabase = openHelperForTest!!.writableDatabase
        var finalWhere: String
        val count: Int
        when (sUriMatcher!!.match(uri)) {
            NOTES -> count = db.delete(
                NotePad.Notes.TABLE_NAME,  // The database table name
                where,  // The incoming where clause column names
                whereArgs // The incoming where clause values
            )

            NOTE_ID -> {
                /*
                 * Starts a final WHERE clause by restricting it to the
                 * desired note ID.
                 */
                finalWhere = BaseColumns._ID +  // The ID column name
                    " = " +  // test for equality
                    uri.pathSegments[NotePad.Notes.NOTE_ID_PATH_POSITION]

                // If there were additional selection criteria, append them to the final
                // WHERE clause
                if (where != null) {
                    finalWhere = "$finalWhere AND $where"
                }

                // Performs the delete.
                count = db.delete(
                    NotePad.Notes.TABLE_NAME,  // The database table name.
                    finalWhere,  // The final WHERE clause
                    whereArgs // The incoming where clause values.
                )
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

        /* Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        context!!.contentResolver.notifyChange(uri, null)

        // Returns the number of rows deleted.
        return count
    }

    /**
     * This is called when a client calls [ContentResolver.update], it updates records in the
     * database. The column names specified by the keys in the [ContentValues] parmaeter [values]
     * map are updated with new data specified by the values in the map. If the incoming [Uri]
     * parameter [uri] matches the note ID URI pattern, then the method updates the one record
     * specified by the ID in the URI; otherwise, it updates a set of records. The record or records
     * must match the input selection criteria specified by [String] parameter [where] and [Array]
     * of [String] parameter [whereArgs]. If rows were updated, then listeners are notified of the
     * change.
     * TODO: Continue here.
     * First we initialize [SQLiteDatabase] variable `val db` to the writeable database returned by
     * the [DatabaseHelper.getWritableDatabase] method (kotlin `writableDatabase` property) of
     * [DatabaseHelper] field [openHelperForTest]. We declare [Int] variable `val count` and
     * [String] variable `var finalWhere`. Then we `when` switch on the value returned by the
     * [UriMatcher.match] method of [UriMatcher] field [sUriMatcher] when matching its patterns
     * against our [Uri] parameter [uri]:
     *
     *  * [NOTES]: (the incoming URI matches the general notes pattern) We set `count` to the number
     *  of rows in `db` that are updated when we call its [SQLiteDatabase.update] method to update
     *  the database table named [NotePad.Notes.TABLE_NAME] ("notes") with the map of column names
     *  and new values contained in our [ContentValues] parameter [values] using our [String]
     *  parameter [where]  for the where clause column names, and [Array] of [String] parameter
     *  [whereArgs] for the where clause column values to select on.
     *
     *  * [NOTE_ID]: (the incoming URI matches a single note ID) we initialize [String] variable
     *  `val noteId` with the note number parsed from our [Uri] parameter [uri] using its
     *  [Uri.getPathSegments] method (kotlin `pathSegments` property): the note number id is in the
     *  [NotePad.Notes.NOTE_ID_PATH_POSITION] position of the list of strings returned. Then we set
     *  `finalWhere` to the string formed by concatenating the ID column name [BaseColumns._ID]
     *  ("_id") followed by the test for equality (" = "), followed by the incoming note ID in
     *  `noteId`. If our [String] parameter [where] is not `null` (there were additional selection
     *  criteria) we set `finalWhere` to the string formed by concatenating `finalWhere` followed
     *  by the string " AND ", followed by `where`. We set `count` to the number of rows in `db`
     *  that are updated when we call its [SQLiteDatabase.update] method to update the database
     *  table named [NotePad.Notes.TABLE_NAME] ("notes") with the map of column names and new values
     *  contained in our [ContentValues] parameter [values] using `finalWhere` for the where clause
     *  column names, and [Array] of [String] parameter [whereArgs] for the where clause column
     *  values to select on.
     *
     *  * `else`: (the incoming pattern is invalid) We throw [IllegalArgumentException].
     *
     * We retrieve the [Context] this provider is running in and use it to fetch a [ContentResolver]
     * instance for our application's package whose [ContentResolver.notifyChange] method we then
     * call to notify registered observers that a row was updated in [uri]. Finally we return
     * `count` (the number of rows updated) to the caller.
     *
     * @param uri The URI pattern to match and update.
     * @param values A map of column names (keys) and new values (values).
     * @param where An SQL "WHERE" clause that selects records based on their column values. If this
     * is `null`, then all records that match the URI pattern are selected.
     * @param whereArgs An array of selection criteria. If the [where] param contains value
     * placeholders ("?" characters), then each placeholder is replaced by the corresponding
     * element in the array.
     * @return The number of rows updated.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        where: String?,
        whereArgs: Array<String>?
    ): Int {

        // Opens the database object in "write" mode.
        val db: SQLiteDatabase = openHelperForTest!!.writableDatabase
        val count: Int
        var finalWhere: String
        when (sUriMatcher!!.match(uri)) {
            NOTES ->
                // Does the update and returns the number of rows updated.
                count = db.update(
                    NotePad.Notes.TABLE_NAME,  // The database table name.
                    values,  // A map of column names and new values to use.
                    where,  // The where clause column names.
                    whereArgs // The where clause column values to select on.
                )

            NOTE_ID -> {
                // From the incoming URI, get the note ID
                val noteId: String = uri.pathSegments[NotePad.Notes.NOTE_ID_PATH_POSITION]

                /*
                 * Starts creating the final WHERE clause by restricting it to the incoming
                 * note ID.
                 */
                finalWhere = BaseColumns._ID +  // The ID column name
                    " = " +  // test for equality
                    noteId // the incoming note ID

                // If there were additional selection criteria, append them to the final WHERE
                // clause
                if (where != null) {
                    finalWhere = "$finalWhere AND $where"
                }


                // Does the update and returns the number of rows updated.
                count = db.update(
                    NotePad.Notes.TABLE_NAME,  // The database table name.
                    values,  // A map of column names and new values to use.
                    finalWhere,  // The final WHERE clause to use
                    whereArgs // The where clause column values to select on, or
                    // null if the values are all in the where argument.
                )
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

        /* Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        context!!.contentResolver.notifyChange(uri, null)

        // Returns the number of rows updated.
        return count
    }

    companion object {
        /**
         * Used for debugging and logging
         */
        private const val TAG = "NotePadProvider"

        /**
         * The database that the provider uses as its underlying data store
         */
        private const val DATABASE_NAME = "note_pad.db"

        /**
         * The database version
         */
        private const val DATABASE_VERSION = 2

        /**
         * A projection map used to select columns from the database, initialized in a static block
         */
        private var sNotesProjectionMap: HashMap<String, String>? = null

        /**
         * Standard projection for the interesting columns of a normal note.
         */
        private val READ_NOTE_PROJECTION = arrayOf(
            BaseColumns._ID,  // Projection position 0, the note's id
            NotePad.Notes.COLUMN_NAME_NOTE,  // Projection position 1, the note's content
            NotePad.Notes.COLUMN_NAME_TITLE
        )

        /**
         * Index of the note's content column COLUMN_NAME_NOTE
         */
        private const val READ_NOTE_NOTE_INDEX = 1

        /**
         * Index of the note's title column COLUMN_NAME_TITLE
         */
        private const val READ_NOTE_TITLE_INDEX = 2

        // Constants used by the Uri matcher to choose an action
        // based on the pattern of the incoming URI

        /**
         * The incoming URI matches the Notes URI pattern
         */
        private const val NOTES = 1

        /**
         * The incoming URI matches the Note ID URI pattern
         */
        private const val NOTE_ID = 2

        /**
         * A UriMatcher instance, initialized in a static block
         */
        private var sUriMatcher: UriMatcher? = null

        init {
            //Creates and initializes the URI matcher
            // Create a new instance
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

            // Add a pattern that routes URIs terminated with "notes" to a NOTES operation
            sUriMatcher!!.addURI(NotePad.AUTHORITY, "notes", NOTES)

            // Add a pattern that routes URIs terminated with "notes" plus an integer (# is a wild card
            // for an integer) to a note ID operation
            sUriMatcher!!.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID)

            // Creates and initializes a projection map that returns all columns
            // Creates a new projection map instance. The map returns a column name
            // given a string. The two are usually equal.
            sNotesProjectionMap = HashMap()

            // Maps the string "_ID" to the column name "_ID"
            sNotesProjectionMap!![BaseColumns._ID] = BaseColumns._ID

            // Maps "title" to "title"
            sNotesProjectionMap!![NotePad.Notes.COLUMN_NAME_TITLE] = NotePad.Notes.COLUMN_NAME_TITLE

            // Maps "note" to "note"
            sNotesProjectionMap!![NotePad.Notes.COLUMN_NAME_NOTE] = NotePad.Notes.COLUMN_NAME_NOTE

            // Maps "created" to "created"
            sNotesProjectionMap!![NotePad.Notes.COLUMN_NAME_CREATE_DATE] =
                NotePad.Notes.COLUMN_NAME_CREATE_DATE

            // Maps "modified" to "modified"
            sNotesProjectionMap!![NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE] =
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        }

        /**
         * This describes the MIME types that are supported for opening a note
         * URI as a stream.
         */
        var NOTE_STREAM_TYPES: ClipDescription = ClipDescription(
            /* label = */ null,
            /* mimeTypes = */ arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        )
    }
}