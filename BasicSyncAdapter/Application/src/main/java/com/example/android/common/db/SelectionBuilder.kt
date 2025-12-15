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
/*
 * Modifications:
 * -Imported from A.O.S.P. frameworks/base/core/java/com/android/internal/content
 * -Changed package name
 */
@file:Suppress("unused", "SENSELESS_COMPARISON", "MemberVisibilityCanBePrivate")

package com.example.android.common.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.util.Log
import java.util.Arrays

/**
 * Helper for building selection clauses for [SQLiteDatabase]. This class provides a convenient
 * frontend for working with SQL. Instead of composing statements manually using string
 * concatenation, method calls are used to construct the statement one clause at a time. These
 * methods can be chained together. If multiple `where`() statements are provided, they're
 * combined using `AND`.
 *
 * Example:
 *
 *    val builder = SelectionBuilder()
 *    val c: Cursor = builder.table(FeedContract.Entry.TABLE_NAME)
 *                .where(FeedContract.Entry._ID + "=?", id)
 *                .query(db, projection, sortOrder)
 *
 * In this example, the table name and filters (`WHERE` clauses) are both explicitly
 * specified via method call. [SelectionBuilder] takes care of issuing a "query" command to the
 * database, and returns the resulting [Cursor] object.
 *
 * Inner `JOIN`s can be accomplished using the `mapToTable`() function. The `map`() function
 * can be used to create new columns based on arbitrary (SQL-based) criteria. In advanced usage,
 * entire sub-queries can be passed into the map() function.
 *
 * Advanced example:
 *
 *    // String SESSIONS_JOIN_BLOCKS_ROOMS = "sessions "
 *    //        + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
 *    //        + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";
 *
 *    // String SubQuery.BLOCK_NUM_STARRED_SESSIONS =
 *    //       "(SELECT COUNT(1) FROM "
 *    //        + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
 *    //        + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1)";
 *
 *    String SubQuery.BLOCK_SESSIONS_COUNT =
 *    Cursor c = builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
 *         .map(Blocks.NUM_STARRED_SESSIONS, SubQuery.BLOCK_NUM_STARRED_SESSIONS)
 *         .mapToTable(Sessions._ID, Tables.SESSIONS)
 *         .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
 *         .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
 *         .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
 *         .where(Qualified.SESSIONS_BLOCK_ID + "=?", blockId);
 *
 * In this example, we have two different types of `JOIN`s: a left outer join using a modified table
 * name (since this class doesn't directly support these), and an inner join using the mapToTable()
 * function. The map() function is used to insert a count based on specific criteria, executed as a
 * sub-query.
 *
 * This class is *not* thread safe.
 */
class SelectionBuilder {
    /**
     * Table name to use for sql query, update, and delete
     */
    private var mTable: String? = null

    /**
     * Translation map from column names to aliases (column->table.column (inner join)
     * and column->toClause "AS" column (adds new column) UNUSED by us.
     */
    private val mProjectionMap: MutableMap<String, String> = HashMap()

    /**
     * [StringBuilder] we use to build the SQL selection (WHERE clause minus the WHERE)
     */
    private val mSelection = StringBuilder()

    /**
     * List of arguments to use to replace "?" in our SQL selection `mSelection`
     */
    private val mSelectionArgs = ArrayList<String>()

    /**
     * Reset any internal state, allowing this builder to be recycled. Calling this method is more
     * efficient than creating a new [SelectionBuilder] object.
     *
     * We set our [String] field [mTable] (the table name used for sql query) to `null`, set the
     * length of the character sequence in [StringBuilder] field [mSelection] (our SQL selection
     * clause builder) to 0, and clear [ArrayList] of [String] field [mSelectionArgs] (our list of
     * selection arguments). Finally we return `this` to the caller.
     *
     * @return Fluent interface (A fluent interface is normally implemented by using method chaining
     * to implement method cascading (in languages that do not natively support cascading), concretely
     * by having each method return this).
     */
    fun reset(): SelectionBuilder {
        mTable = null
        mSelection.setLength(0)
        mSelectionArgs.clear()
        return this
    }

    /**
     * Append the given selection clause to the internal state. Each clause is surrounded with
     * parenthesis and combined using `AND`. In the most basic usage, simply provide a selection
     * in SQL `WHERE` statement format.
     *
     * Example:
     *
     *     .where("blog_posts.category = 'PROGRAMMING')
     *
     * User input should never be directly supplied as part of the selection statement. Instead,
     * use positional parameters in your selection statement, then pass the user input in via the
     * `selectionArgs` parameter. This prevents SQL escape characters in user input from causing
     * unwanted side effects. (Failure to follow this convention may have security implications.)
     * Positional parameters are specified using the '?' character.
     *
     * Example:
     *
     *     .where("blog_posts.title contains ?", userSearchString)
     *
     * First if our [String] parameter [selection] is the empty string we check to see if
     * we have one or more [String] entries in our `vararg` parameter [selectionArgs] and
     * throw [IllegalArgumentException] if we do, and if there are none we just return `this`
     * to the caller.
     *
     * Then if the length of the current value of [StringBuilder] field [mSelection] is greater
     * than 0 we append the string " AND " to it. We then append the string "(", followed by
     * our [selection] parameter followed by the string ")" to [mSelection]. Then if our parameter
     * [selectionArgs] is not null we add all of [selectionArgs] to our [ArrayList] of [String]
     * field [mSelectionArgs]. Finally we return this to the caller.
     *
     * @param selection     SQL where statement
     * @param selectionArgs Values to substitute for positional parameters ('?' characters in
     * [selection] statement. Will be automatically escaped.
     * @return Fluent interface (ie. `this`)
     */
    fun where(selection: String?, vararg selectionArgs: String?): SelectionBuilder {
        if (TextUtils.isEmpty(selection)) {
            require(!(selectionArgs != null && selectionArgs.isNotEmpty())) {
                "Valid selection required when including arguments="
            }

            // Shortcut when clause is empty
            return this
        }
        if (mSelection.isNotEmpty()) {
            mSelection.append(" AND ")
        }
        mSelection.append("(").append(selection).append(")")
        if (selectionArgs != null) {
            for (string: String? in selectionArgs) {
                mSelectionArgs.add(string!!)
            }
        }
        return this
    }

    /**
     * Table name to use for SQL `FROM` statement. This method may only be called once. If multiple
     * tables are required, concatenate them in SQL-format (typically comma-separated). If you need
     * to do advanced `JOIN`s, they can also be specified here. See also: [mapToTable].
     *
     * We just save our [String] parameter [table] in our field [mTable] and return `this`
     * to the caller.
     *
     * @param table Table name
     * @return Fluent interface (ie. `this`)
     */
    fun table(table: String?): SelectionBuilder {
        mTable = table
        return this
    }

    /**
     * Verify that a table name has been supplied using [table]. If our [mTable] field is `null`
     * we throw [IllegalStateException] "Table not specified"
     *
     * @throws IllegalStateException if table not set
     */
    private fun assertTable() {
        checkNotNull(mTable) { "Table not specified" }
    }

    /**
     * Perform an inner join. Map columns from a secondary table onto the current result set.
     * References to the column specified in our [String] parameter [column] will be replaced
     * with `table.column` in the SQL `SELECT` clause. We just store under the key [column] the
     * string formed by concatenating `table`, ".", and `column` in our [MutableMap] of [String]
     * to [String] field [mProjectionMap] then return `this` to the caller.
     *
     * @param column Column name to join on. Must be the same in both tables.
     * @param table  Secondary table to join.
     * @return Fluent interface (ie. `this`)
     */
    fun mapToTable(column: String, table: String): SelectionBuilder {
        mProjectionMap[column] = "$table.$column"
        return this
    }

    /**
     * Create a new column based on custom criteria (such as aggregate functions). This adds a new
     * column to the result set, based upon custom criteria in SQL format. This is equivalent to
     * the SQL statement: `SELECT toClause AS fromColumn`. This method is useful for executing SQL
     * sub-queries.
     *
     * @param fromColumn Name of column for mapping
     * @param toClause   SQL string representing data to be mapped
     * @return Fluent interface (ie. `this`)
     */
    fun map(fromColumn: String, toClause: String): SelectionBuilder {
        mProjectionMap[fromColumn] = "$toClause AS $fromColumn"
        return this
    }

    /**
     * Return selection string based on current internal state. We just return the string version of
     * our [StringBuilder] field [mSelection].
     *
     * @return Current selection as a SQL statement
     * @see [selectionArgs]
     */
    val selection: String
        get() = mSelection.toString()

    /**
     * Return selection arguments based on current internal state. Converts our [ArrayList] of
     * [String] field [mSelectionArgs] to an array of strings and returns it.
     *
     * @return our list field [mSelectionArgs] converted to an array.
     */
    val selectionArgs: Array<String>
        get() = mSelectionArgs.toTypedArray()

    /**
     * Process user-supplied projection (column list). In cases where a column is mapped to another
     * data source (either another table, or an SQL sub-query), the column name will be replaced
     * with a more specific, SQL-compatible representation. Assumes that incoming columns are
     * non-`null`. See also: [map], [mapToTable].
     *
     * We loop over all the strings in our [Array] of [String] parameter [columns] first retrieving
     * the alias for that column contained in our map field [mProjectionMap] (if any) to initialize
     * [String] variable `val target`, then if `target` is not `null` we set the current column
     * [String] in [columns] to `target`.
     *
     * @param columns User supplied projection (column list).
     */
    private fun mapColumns(columns: Array<String>) {
        for (i in columns.indices) {
            val target = mProjectionMap[columns[i]]
            if (target != null) {
                columns[i] = target
            }
        }
    }

    /**
     * Return a description of this builder's state. Does NOT output SQL.
     *
     * @return Human-readable internal state
     */
    override fun toString(): String {
        return ("SelectionBuilder[table=" + mTable + ", selection=" + selection
            + ", selectionArgs=" + selectionArgs.contentToString() + "]")
    }

    /**
     * Execute query (SQL `SELECT`) against specified database. Using a null projection
     * (column list) is not supported. We just return the [Cursor] returned by our 6 argument
     * `query` method.
     *
     * @param db      Database to query.
     * @param columns Database projection (column list) to return, must be non-`null`.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause (excluding the
     * ORDER BY itself). Passing `null` will use the default sort order, which may be
     * unordered.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    fun query(db: SQLiteDatabase, columns: Array<String>?, orderBy: String?): Cursor {
        return query(
            db = db,
            columns = columns,
            groupBy = null,
            having = null,
            orderBy = orderBy,
            limit = null
        )
    }

    /**
     * Execute query (`SELECT`) against database. Using a null projection (column list) is not
     * supported. First we call our method [assertTable] to make sure we have set our [String]
     * field [mTable] (if it is `null` it throws [IllegalStateException]). If our [Array] of
     * [String] parameter [columns] is not `null` we call our method [mapColumns] on it to
     * translate the columns to their aliases (if an alias exists for it). Finally we return the
     * [Cursor] returned by the [SQLiteDatabase.query] method of `db` when it is called with the
     * arguments:
     *
     *  * [mTable] - The table name to compile the query against.
     *
     *  * [columns] - the list of columns to return. Passing `null` will return all columns,
     *  which is discouraged to prevent reading data from storage that isn't going to be used.
     *
     *  * The string returned by our property [selection] (aka java `getSelection` method) - filter
     *  declaring which rows to return, formatted as an SQL `WHERE` clause (excluding the `WHERE`
     *  itself). Passing `null` will return all rows for the given table.
     *
     *  * The array of strings returned by our property [selectionArgs] (aka java method
     *  `getSelectionArgs`0 - You may include ?s in [selection], which will be replaced by the
     *  values from [selectionArgs], in order that they appear in the [selection]. The values
     *  will be bound as [String]'s.
     *
     *  * [groupBy] - filter declaring how to group rows, formatted as an SQL GROUP BY clause
     *  (excluding the "GROUP BY" itself). Passing `null` will cause the rows to not be grouped.
     *
     *  * [having] - filter used to declare which row groups to include in the cursor, if row
     *  grouping is being used, formatted as an SQL "HAVING" clause (excluding the "HAVING"
     *  itself). Passing `null` will cause all row groups to be included, and is required when
     *  row grouping is not being used.
     *
     *  * [orderBy] - How to order the rows, formatted as an SQL "ORDER BY" clause (excluding the
     *  "ORDER BY" itself). Passing `null` will use the default sort order, which may be unordered.
     *
     *  * [limit] - limits the number of rows returned by the query, formatted as "LIMIT" clause.
     *  Passing `null` denotes no "LIMIT" clause.
     *
     * @param db      Database to query.
     * @param columns Database projection (column list) to return, must be non-`null`.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL "GROUP BY" clause
     * (excluding the "GROUP BY" itself). Passing `null` will cause the rows to not be grouped.
     * @param having  A filter declaring which row groups to include in the cursor, if row grouping
     * is being used, formatted as an SQL "HAVING" clause (excluding the "HAVING" itself). Passing
     * `null` will cause all row groups to be included, and is required when row grouping is not
     * being used.
     * @param orderBy How to order the rows, formatted as an SQL "ORDER BY" clause (excluding the
     * "ORDER BY" itself). Passing `null` will use the default sort order, which may be unordered.
     * @param limit   Limits the number of rows returned by the query, formatted as "LIMIT" clause.
     * Passing `null` denotes no "LIMIT" clause.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]'s are not synchronized, see the documentation for more details.
     */
    fun query(
        db: SQLiteDatabase,
        columns: Array<String>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?
    ): Cursor {
        assertTable()
        columns?.let { mapColumns(it) }
        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
        Log.v(TAG, "query(columns=" + Arrays.toString(columns) + ") " + this)
        return db.query(
            /* table = */ mTable!!,
            /* columns = */ columns,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* groupBy = */ groupBy,
            /* having = */ having,
            /* orderBy = */ orderBy,
            /* limit = */ limit
        )
    }

    /**
     * Execute an `UPDATE` against database. First we call our method [assertTable] to make sure we
     * have set our [String] field [mTable] (if it is `null` it throws [IllegalStateException]).
     * Then we return the [Cursor] returned by the [SQLiteDatabase.update] method of `db` for the
     * table [mTable], updating the [ContentValues] parameter [values] for the SQL selection
     * statement returned by our property [selection] (aka java method `getSelection`) and the
     * selection arguments returned by our property [selectionArgs] (aka java method `getSelectionArgs`).
     *
     * @param db     Database to query.
     * @param values A map from column names to new column values. `null` is a valid value that will
     * be translated to NULL
     * @return The number of rows affected.
     */
    fun update(db: SQLiteDatabase, values: ContentValues?): Int {
        assertTable()
        Log.v(TAG, "update() $this")
        return db.update(
            /* table = */ mTable!!,
            /* values = */ values,
            /* whereClause = */ selection,
            /* whereArgs = */ selectionArgs
        )
    }

    /**
     * Execute `DELETE` against database. First we call our method [assertTable] to make sure we
     * have set our [String] field [mTable] (if it is `null` it throws [IllegalStateException]).
     * Then we return the value returned by the [SQLiteDatabase.delete] method of `db` for the table
     * [mTable] for the SQL selection statement returned by our property [selection] (aka java method
     * `getSelection`) and the selection arguments returned by our property [selectionArgs] (aka
     * java method `getSelectionArgs`).
     *
     * @param db Database to query.
     * @return The number of rows affected.
     */
    fun delete(db: SQLiteDatabase): Int {
        assertTable()
        Log.v(TAG, "delete() $this")
        return db.delete(
            /* table = */ mTable!!,
            /* whereClause = */ selection,
            /* whereArgs = */ selectionArgs
        )
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "basicsyncadapter"
    }
}