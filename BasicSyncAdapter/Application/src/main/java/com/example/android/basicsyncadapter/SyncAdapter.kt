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
@file:Suppress("unused", "ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog")

package com.example.android.basicsyncadapter

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.OperationApplicationException
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.provider.BaseColumns
import android.util.Log
import com.example.android.basicsyncadapter.net.FeedParser
import com.example.android.basicsyncadapter.provider.FeedContract
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.ParseException

/**
 * Define a sync adapter for the app. This class is instantiated in [SyncService], which also binds
 * [SyncAdapter] to the system. [SyncAdapter] should only be initialized in [SyncService], never
 * anywhere else. The system calls [onPerformSync] via an RPC call through the [IBinder] object
 * supplied by [SyncService].
 */
internal class SyncAdapter : AbstractThreadedSyncAdapter {
    /**
     * [ContentResolver], for performing database operations. Set in our constructor.
     */
    private val mContentResolver: ContentResolver

    /**
     * Constructor. Calls our super's constructor, then Obtains handle to content resolver to
     * initialize our [ContentResolver] field [mContentResolver] for later use. The parameters are
     * only used by our super's constructor.
     *
     * @param context the [Context] that this is running within.
     * @param autoInitialize if `true` then sync requests that have [ContentResolver.SYNC_EXTRAS_INITIALIZE]
     * set will be internally handled by [AbstractThreadedSyncAdapter] by calling [ContentResolver.setIsSyncable]
     * with 1 if it is currently set to <0.
     */
    constructor(context: Context, autoInitialize: Boolean) : super(context, autoInitialize) {
        mContentResolver = context.contentResolver
    }

    /**
     * Constructor. Calls our super's constructor, then Obtains handle to content resolver to
     * initialize our [ContentResolver] field [mContentResolver] for later use. The parameters are
     * only used by our super's constructor.
     *
     * @param context the [Context] that this is running within.
     * @param autoInitialize if `true` then sync requests that have [ContentResolver.SYNC_EXTRAS_INITIALIZE]
     * set will be internally handled by [AbstractThreadedSyncAdapter] by calling [ContentResolver.setIsSyncable]
     * with 1 if it is currently set to <0.
     * @param allowParallelSyncs if `true` then allow syncs for different accounts to run
     * at the same time, each in their own thread. This must be consistent with the setting
     * in the [SyncAdapter]'s configuration file.
     */
    constructor(context: Context, autoInitialize: Boolean, allowParallelSyncs: Boolean) : super(context, autoInitialize, allowParallelSyncs) {
        mContentResolver = context.contentResolver
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run *in situ*, and you don't have to set up a separate thread for them.
     *
     *
     * This is where we actually perform any work required to perform a sync.
     * [android.content.AbstractThreadedSyncAdapter] guarantees that this will be called on a non-UI thread,
     * so it is safe to perform blocking I/O here.
     *
     *
     * The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     *
     *
     * Perform a sync for this account. SyncAdapter-specific parameters may
     * be specified in extras, which is guaranteed to not be null. Invocations
     * of this method are guaranteed to be serialized.
     *
     *
     * First we log the fact we are "Beginning network synchronization", then wrapped in a try block
     * intended to catch MalformedURLException, IOException, XmlPullParserException, ParseException,
     * RemoteException, or OperationApplicationException we initialize `URL location` with
     * an URL for FEED_URL ("http://android-developers.blogspot.com/atom.xml"), and declare null
     * `InputStream stream`. Wrapped in an inner try block intended to catch exceptions with
     * a finally block that makes sure `stream` is closed exception or not, we initialize
     * `stream` with the `InputStream` returned by our method `downloadUrl` for
     * `URL location`. We then call our method `updateLocalFeedData` with `stream`
     * and our parameter `SyncResult syncResult` to Read XML from the input stream, storing it
     * into the content provider.
     *
     *
     * The catch blocks for the outer try block all log the exception, and increment the field in the
     * `stats` field of our parameter `SyncResult syncResult` reserved for that particular
     * exception:
     *
     *  *
     * MalformedURLException - Thrown to indicate that a malformed URL has occurred. Either
     * no legal protocol could be found in a specification string or the string could not be
     * parsed. Increments `numParseExceptions` (for some reason)
     *
     *  *
     * IOException - Signals that an I/O exception of some sort has occurred. This class is
     * the general class of exceptions produced by failed or interrupted I/O operations.
     * Increments `numIoExceptions`
     *
     *  *
     * XmlPullParserException - This exception is thrown to signal XML Pull Parser related
     * faults. Increments `numParseExceptions`
     *
     *  *
     * ParseException - Signals that an error has been reached unexpectedly while parsing.
     * Increments `numParseExceptions`
     *
     *  *
     * RemoteException - Parent exception for all Binder remote-invocation errors.
     * Increments `databaseError` Used to indicate that the SyncAdapter experienced a
     * hard error due to an error it received from interacting with the storage layer. The
     * SyncManager will record that the sync request failed and it will not reschedule the
     * request.
     *
     *  *
     * OperationApplicationException - Thrown when an application of a ContentProviderOperation
     * (Represents a single operation to be performed as part of a batch of operations)
     * fails due to the specified constraints. Increments `databaseError`
     *
     *
     * If no exceptions are thrown, we log "Network synchronization complete"
     *
     * @param account    the account that should be synced
     * @param extras     SyncAdapter-specific parameters
     * @param authority  the authority of this sync request
     * @param provider   a ContentProviderClient that points to the ContentProvider for this
     * authority
     * @param syncResult SyncAdapter-specific parameters
     */
    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.i(TAG, "Beginning network synchronization")
        try {
            val location = URL(FEED_URL)
            var stream: InputStream? = null
            try {
                Log.i(TAG, "Streaming data from network: $location")
                stream = downloadUrl(location)
                updateLocalFeedData(stream, syncResult)
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                stream?.close()
            }
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Feed URL is malformed", e)
            syncResult.stats.numParseExceptions++
            return
        } catch (e: IOException) {
            Log.e(TAG, "Error reading from network: $e")
            syncResult.stats.numIoExceptions++
            return
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "Error parsing feed: $e")
            syncResult.stats.numParseExceptions++
            return
        } catch (e: ParseException) {
            Log.e(TAG, "Error parsing feed: $e")
            syncResult.stats.numParseExceptions++
            return
        } catch (e: RemoteException) {
            Log.e(TAG, "Error updating database: $e")
            syncResult.databaseError = true
            return
        } catch (e: OperationApplicationException) {
            Log.e(TAG, "Error updating database: $e")
            syncResult.databaseError = true
            return
        }
        Log.i(TAG, "Network synchronization complete")
    }

    /**
     * Read XML from an input stream, storing it into the content provider.
     *
     *
     * This is where incoming data is persisted, committing the results of a sync. In order to
     * minimize (expensive) disk operations, we compare incoming data with what's already in our
     * database, and compute a merge. Only changes (insert/update/delete) will result in a database
     * write.
     *
     *
     * As an additional optimization, we use a batch operation to perform all database writes at
     * once.
     *
     *
     * Merge strategy:
     *
     *  * 1. Get cursor to all items in feed
     *  * 2. For each item, check if it's in the incoming data.
     *
     *  * a. YES: Remove from "incoming" list. Check if data has mutated, if so, perform
     * database UPDATE.
     *  * b. NO: Schedule DELETE from database.
     *
     * (At this point, incoming database only contains missing items.)
     *  * 3. For any items remaining in incoming list, ADD to database.
     *
     * First we initialize `FeedParser feedParser` with a new instance (our net.FeedParser
     * class parses generic Atom feeds). Then we initialize `ContentResolver contentResolver`
     * with a ContentResolver instance for our application's package. Then we fill our variable
     * `List<FeedParser.Entry> entries` with the entries that the `feedParser` method
     * `parse` parses from its reading of our parameter `stream`. We initialize our variables
     * `ArrayList<ContentProviderOperation> batch` with a new `ArrayList` , and
     * `HashMap<String, FeedParser.Entry> entryMap` with a new `HashMap`. Then for every
     * `FeedParser.Entry e` in `entries` we store `e` under the key `e.id`
     * in `entryMap`.
     *
     *
     * We now initialize `Uri uri` with the uri for all entries in our local database
     * CONTENT_URI ("content://com.example.android.basicsyncadapter/entries") and initialize
     * `Cursor c` with the results of querying `uri` using the projection PROJECTION.
     * We declare `int id`, `String entryId`, `String title`, `String link`,
     * and `long published` (these variables will hold the columns we read from each row of
     * `Cursor c` in the loop to follow).
     *
     *
     * We now loop for all the rows in `Cursor c` first incrementing the `numEntries`
     * field of the `stats` field of `SyncResult syncResult` (Counter for tracking how
     * many entries were affected by the sync operation, as defined by the SyncAdapter). Reading
     * from `Cursor c` we set `id` to the int stored in column COLUMN_ID, `entryId`
     * to the string stored in COLUMN_ENTRY_ID, `title` to the string stored in column COLUMN_TITLE,
     * `link` to the string stored in COLUMN_LINK, and `published` to the long stored in
     * column COLUMN_PUBLISHED. We next try to fetch `FeedParser.Entry match` from the entry
     * in `entryMap` stored under key `entryId`. If `match` is:
     *
     *  *
     * not null - we remove it from `entryMap` to avoid inserting it again later. We
     * create `Uri existingUri` to point to the Uri for this entry. Then if any of the
     * fields `title`, `link`, or `published` have changed we call the
     * `newUpdate` method of `ContentProviderOperation` to create a builder for
     * building an update `ContentProviderOperation` for updating `existingUri`
     * which we proceed to set values for COLUMN_NAME_TITLE, COLUMN_NAME_LINK, and
     * COLUMN_NAME_PUBLISHED using `withValue` from the values for these columns in
     * `FeedParser.Entry match`. We then build this builder and add the
     * `ContentProviderOperation` to our list `batch`. We then increment the
     * `numUpdates` field of the `stats` field of `syncResult`, and loop
     * back for the next `Entry`.
     *
     *  *
     * null - we just log "No action: "
     *
     *
     * After handling all the rows in `Cursor c` we close `c`, and move on to add the
     * values in `entryMap` that are new. To do this we loop through all the `Entry e`
     * that remain in `entryMap` creating a Builder suitable for building an insert
     * `ContentProviderOperation`, then proceeding to set values for COLUMN_NAME_ENTRY_ID,
     * COLUMN_NAME_TITLE, COLUMN_NAME_LINK, and COLUMN_NAME_PUBLISHED using `withValue` from
     * the values for these columns in `e`. We then build this builder and add the
     * `ContentProviderOperation` to our list `batch`. We then increment the
     * `numInserts` field of the `stats` field of `syncResult`, and loop
     * back for the next `Entry e`.
     *
     *
     * Having filled `batch` with `ContentProviderOperation` objects we call the
     * `applyBatch` of our `ContentResolver mContentResolver` to apply the transactions
     * in `batch` to CONTENT_AUTHORITY ("com.example.android.basicsyncadapter"). We then call
     * the `notifyChange` method of `mContentResolver` to notify registered observers
     * that a row was updated for the Uri FeedContract.Entry.CONTENT_URI
     * ("content://com.example.android.basicsyncadapter/entries")
     *
     * @param stream     `InputStream` we are to read from
     * @param syncResult `SyncResult` we are to update when done.
     * @throws IOException                   Signals that an I/O exception of some sort has occurred.
     * @throws XmlPullParserException        This exception is thrown to signal XML Pull Parser related faults.
     * @throws RemoteException               Parent exception for all Binder remote-invocation errors
     * @throws OperationApplicationException Thrown when an application of a ContentProviderOperation
     * fails due to specified constraints.
     * @throws ParseException                Signals that an error has been reached unexpectedly while parsing.
     */
    @Throws(IOException::class, XmlPullParserException::class, RemoteException::class, OperationApplicationException::class, ParseException::class)
    fun updateLocalFeedData(stream: InputStream?, syncResult: SyncResult) {
        val feedParser = FeedParser()
        val contentResolver: ContentResolver = context.contentResolver
        Log.i(TAG, "Parsing stream as Atom feed")
        val entries: List<FeedParser.Entry> = feedParser.parse(stream!!)
        Log.i(TAG, "Parsing complete. Found " + entries.size + " entries")
        val batch = ArrayList<ContentProviderOperation>()

        // Build hash table of incoming entries
        val entryMap = HashMap<String, FeedParser.Entry>()
        for (e in entries) {
            entryMap[e.id!!] = e
        }

        // Get list of all items
        Log.i(TAG, "Fetching local entries for merge")
        val uri = FeedContract.Entry.CONTENT_URI // Get all entries
        val c = contentResolver.query(uri, PROJECTION, null, null, null)!!
        Log.i(TAG, "Found " + c.count + " local entries. Computing merge solution...")

        // Find stale data
        var id: Int
        var entryId: String
        var title: String
        var link: String
        var published: Long
        while (c.moveToNext()) {
            syncResult.stats.numEntries++
            id = c.getInt(COLUMN_ID)
            entryId = c.getString(COLUMN_ENTRY_ID)
            title = c.getString(COLUMN_TITLE)
            link = c.getString(COLUMN_LINK)
            published = c.getLong(COLUMN_PUBLISHED)
            val match = entryMap[entryId]
            if (match != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                entryMap.remove(entryId)
                // Check to see if the entry needs to be updated
                val existingUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                    .appendPath(Integer.toString(id)).build()
                if (match.title != null && match.title != title || match.link != null && match.link != link || match.published != published) {
                    // Update existing record
                    Log.i(TAG, "Scheduling update: $existingUri")
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                        .withValue(FeedContract.Entry.COLUMN_NAME_TITLE, match.title)
                        .withValue(FeedContract.Entry.COLUMN_NAME_LINK, match.link)
                        .withValue(FeedContract.Entry.COLUMN_NAME_PUBLISHED, match.published)
                        .build())
                    syncResult.stats.numUpdates++
                } else {
                    Log.i(TAG, "No action: $existingUri")
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                val deleteUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                    .appendPath(Integer.toString(id)).build()
                Log.i(TAG, "Scheduling delete: $deleteUri")
                batch.add(ContentProviderOperation.newDelete(deleteUri).build())
                syncResult.stats.numDeletes++
            }
        }
        c.close()

        // Add new items
        for (e in entryMap.values) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.id)
            batch.add(ContentProviderOperation.newInsert(FeedContract.Entry.CONTENT_URI)
                .withValue(FeedContract.Entry.COLUMN_NAME_ENTRY_ID, e.id)
                .withValue(FeedContract.Entry.COLUMN_NAME_TITLE, e.title)
                .withValue(FeedContract.Entry.COLUMN_NAME_LINK, e.link)
                .withValue(FeedContract.Entry.COLUMN_NAME_PUBLISHED, e.published)
                .build())
            syncResult.stats.numInserts++
        }
        Log.i(TAG, "Merge solution ready. Applying batch update")
        mContentResolver.applyBatch(FeedContract.CONTENT_AUTHORITY, batch)
        @Suppress("DEPRECATION") // TODO: fix notifyChange deprecation
        mContentResolver.notifyChange(
            FeedContract.Entry.CONTENT_URI,  // URI where data was modified
            null,  // No local observer
            false // IMPORTANT: Do not sync to network
        )
        // This sample doesn't support uploads, but if *your* code does, make sure you set
        // syncToNetwork=false in the line above to prevent duplicate syncs.
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream. We
     * initialize `HttpURLConnection conn` by calling the `openConnection` method of
     * `URL url` which returns a URLConnection instance that represents a connection to the
     * remote object referred to by the URL. We configure `conn` to have a read timeout of
     * NET_READ_TIMEOUT_MILLIS (10000ms), a connect timeout of NET_CONNECT_TIMEOUT_MILLIS (15000ms)
     * a request method of "GET", and set the DoInput flag to true (since we intend to use the URL
     * connection for input). We then call the `connect` method of `conn` to open a
     * communications link. Finally we return an input stream that reads from this open connection
     * returned by the `getInputStream` method of `conn`.
     *
     * @param url URL to connect to
     * @return an `InputStream` connected to our parameter `URL url`
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    @Throws(IOException::class)
    private fun downloadUrl(url: URL): InputStream {
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = NET_READ_TIMEOUT_MILLIS
        conn.connectTimeout = NET_CONNECT_TIMEOUT_MILLIS
        conn.requestMethod = "GET"
        conn.doInput = true
        // Starts the query
        conn.connect()
        return conn.inputStream
    }

    companion object {
        const val TAG = "SyncAdapter"

        /**
         * URL to fetch content from during a sync.
         *
         *
         * This points to the Android Developers Blog. (Side note: We highly recommend reading the
         * Android Developer Blog to stay up to date on the latest Android platform developments!)
         */
        private const val FEED_URL = "https://android-developers.blogspot.com/atom.xml"

        /**
         * Network connection timeout, in milliseconds.
         */
        private const val NET_CONNECT_TIMEOUT_MILLIS = 15000 // 15 seconds

        /**
         * Network read timeout, in milliseconds.
         */
        private const val NET_READ_TIMEOUT_MILLIS = 10000 // 10 seconds

        /**
         * Projection used when querying content provider. Returns all known fields.
         */
        private val PROJECTION = arrayOf(
            BaseColumns._ID,
            FeedContract.Entry.COLUMN_NAME_ENTRY_ID,
            FeedContract.Entry.COLUMN_NAME_TITLE,
            FeedContract.Entry.COLUMN_NAME_LINK,
            FeedContract.Entry.COLUMN_NAME_PUBLISHED)

        // Constants representing column positions from PROJECTION.
        const val COLUMN_ID = 0
        const val COLUMN_ENTRY_ID = 1
        const val COLUMN_TITLE = 2
        const val COLUMN_LINK = 3
        const val COLUMN_PUBLISHED = 4
    }
}