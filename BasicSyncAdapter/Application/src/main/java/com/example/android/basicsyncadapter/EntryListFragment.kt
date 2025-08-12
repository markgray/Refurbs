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
@file:Suppress("SENSELESS_COMPARISON", "unused", "DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.basicsyncadapter

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SyncStatusObserver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.text.format.Time
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.MenuItemCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.ListFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.example.android.basicsyncadapter.provider.FeedProvider
import com.example.android.basicsyncadapter.provider.FeedContract
import com.example.android.common.accounts.GenericAccountService
import androidx.core.net.toUri

/**
 * List fragment containing a list of Atom entry objects (articles) stored in the local database.
 * Database access is mediated by a content provider, specified in [FeedProvider] This content
 * provider is automatically populated by [SyncService]. Selecting an item from the displayed list
 * displays the article in the default browser. If the content provider doesn't return any data,
 * then the first sync hasn't run yet. This sync adapter assumes data exists in the provider once a
 * sync has run. If your app doesn't work like this, you should add a flag that notes if a sync has
 * run, so you can differentiate between "no available data" and "no initial sync", and display this
 * in the UI. The ActionBar displays a "Refresh" button. When the user clicks "Refresh", the sync
 * adapter runs immediately. An indeterminate ProgressBar element is displayed, showing that the
 * sync is occurring.
 */
class EntryListFragment
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
    : ListFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Cursor adapter for controlling [ListView] results.
     */
    private var mAdapter: SimpleCursorAdapter? = null

    /**
     * Handle to a [SyncStatusObserver]. The [ProgressBar] element is visible until the
     * [SyncStatusObserver] reports that the sync is complete. This allows us to delete
     * our [SyncStatusObserver] once the application is no longer in the foreground.
     */
    private var mSyncObserverHandle: Any? = null

    /**
     * Options menu used to populate ActionBar.
     */
    private var mOptionsMenu: Menu? = null

    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate` then we call [setHasOptionsMenu] with `true` to report that this fragment would
     * like to participate in populating the options menu by receiving a call to [onCreateOptionsMenu]
     * and related methods.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     * this is the state. We do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Called when a fragment is first attached to its context. Create sync [Account] at launch, if
     * needed. This will create a new [Account] with the system for our application, register our
     * [SyncService] with it, and establish a sync schedule. First we call our super's implementation
     * of `onAttach`, then we call the method [SyncUtils.CreateSyncAccount] to create an entry for
     * this application in the system account list, if it isn't already there.
     *
     * @param context The calling [Context] being used to instantiate the fragment.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Create account, if needed
        SyncUtils.CreateSyncAccount(activity as Context)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. This gives subclasses a chance to initialize themselves once they
     * know their view hierarchy has been completely created. The fragment's view hierarchy is not
     * however attached to its parent at this point.
     *
     * First we call our super's implementation of `onViewCreated`, then we initialize our
     * [SimpleCursorAdapter] field [mAdapter] with a new instance constructed to use the layout
     * [android.R.layout.simple_list_item_activated_2], with a `null` cursor specified because we do
     * not have one yet, displaying the cursor column names specified in [FROM_COLUMNS], and
     * displaying them in the layout fields specified in [TO_FIELDS]. We then set the view binder
     * of [mAdapter] to an anonymous class whose `setViewValue` override intercepts data from the
     * column [COLUMN_PUBLISHED] in order to convert the long time stamp into a formatted string
     * which it uses to set the text of the `viewToBind`, all other columns are allowed to be
     * processed by [SimpleCursorAdapter].
     *
     * We then set our list adapter to [mAdapter], set our empty text to the string with id
     * `R.string.loading` ("Waiting for sync..."). We then fetch the [LoaderManager] for this
     * fragment (creating it if needed) and call its [LoaderManager.initLoader] method to ensure
     * a loader is initialized and active. (If the loader doesn't already exist, one is created
     * and (if the fragment is currently started) starts the loader. Otherwise the last created
     * loader is re-used.)
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed from a
     * previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdapter = SimpleCursorAdapter(
            activity,  // Current context
            android.R.layout.simple_list_item_activated_2,  // Layout for individual rows
            null,  // Cursor
            FROM_COLUMNS,  // Cursor columns to use
            TO_FIELDS,  // Layout fields to use
            0 // No flags
        )
        mAdapter!!.viewBinder = SimpleCursorAdapter.ViewBinder { viewToBind: View, cursor: Cursor, i: Int ->

            /**
             * Binds the Cursor column defined by the specified index to the specified `viewToBind`.
             * When binding is handled by this `ViewBinder`, this method must return `true`. If this
             * method returns `false`, SimpleCursorAdapter will attempt to handle the binding on its
             * own.
             *
             * If the column `i` is [COLUMN_PUBLISHED] we initialize [Time] variable `val t` with a
             * new instance, and set its fields in accordance to the UTC milliseconds of the long
             * value stored in the [Cursor] `cursor` under the key `i`. We then set the text of
             * the [View] `viewToBind` to a formatted string created from `t` using the format
             * "%Y-%m-%d %H:%M", and return true to the caller.
             *
             * If the column `i` is not [COLUMN_PUBLISHED] we return `false` to the caller so that
             * [SimpleCursorAdapter] will display the data.
             *
             * @param viewToBind the [View] to bind the data to
             * @param cursor the [Cursor] to get the data from
             * @param i the column at which the data can be found in the cursor
             * @return `true` if the data was bound to the viewToBind, `false` otherwise
             */
            if (i == COLUMN_PUBLISHED) {
                // Convert timestamp to human-readable date
                val t = Time()
                t.set(cursor.getLong(i))
                (viewToBind as TextView).text = t.format("%Y-%m-%d %H:%M")
                true
            } else {
                // Let SimpleCursorAdapter handle other fields automatically
                false
            }
        }
        listAdapter = mAdapter
        setEmptyText(getText(R.string.loading))
        loaderManager.initLoader(0, null, this)
    }

    /**
     * Called when the fragment is visible to the user and actively running. First we call our super's
     * implementation of `onResume`, then we call the [SyncStatusObserver.onStatusChanged] method of
     * our field [mSyncStatusObserver] to set the state of the `Refresh` button, and if a sync is
     * active, turn on the [ProgressBar] widget. Otherwise, turn it off.
     *
     * We create [Int] variable `val mask` by or'ing together [ContentResolver.SYNC_OBSERVER_TYPE_PENDING]
     * and [ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE] then use it when we add our [SyncStatusObserver]
     * field [mSyncStatusObserver] as a [SyncStatusObserver]. We save the [Any] returned by the
     * [ContentResolver.addStatusChangeListener] in our field [mSyncObserverHandle] so that we can
     * remove the observer later.
     */
    override fun onResume() {
        super.onResume()
        mSyncStatusObserver.onStatusChanged(0)

        // Watch for sync state changes
        val mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING or
            ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver)
    }

    /**
     * Called when the Fragment is no longer resumed. First we call our super's implementation of
     * `onPause`, then if our [Any] field [mSyncObserverHandle] is not `null` we call the
     * [ContentResolver.removeStatusChangeListener] method to remove our [SyncStatusObserver] and
     * set [mSyncObserverHandle] to `null`.
     */
    override fun onPause() {
        super.onPause()
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle)
            mSyncObserverHandle = null
        }
    }

    /**
     * Instantiate and return a new [Loader] for the given ID. Query the content provider for data.
     * Loaders do queries in a background thread. They also provide a [ContentObserver] that is
     * triggered when data in the content provider changes. When the sync adapter updates the
     * content provider, the [ContentObserver] responds by resetting the loader and then reloading
     * it. We return a `CursorLoader` constructed to retrieve the URI [FeedContract.Entry.CONTENT_URI]
     * ("content://com.example.android.basicsyncadapter/entries") with the projection to use specified
     * by [PROJECTION], with null as the selection (returns all rows), and sorted by the column
     * [FeedContract.Entry.COLUMN_NAME_PUBLISHED] ("published") in "desc" (descending) order.
     *
     * @param i The ID whose loader is to be created.
     * @param bundle Any arguments supplied by the caller.
     * @return Return a new [Loader] instance that is ready to start loading.
     */
    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        // We only have one loader, so we can ignore the value of i.
        // (It'll be '0', as set in onCreate().)
        return CursorLoader(requireActivity(),  // Context
            FeedContract.Entry.CONTENT_URI,  // URI
            PROJECTION,  // Projection
            null,  // Selection
            null,  // Selection args
            FeedContract.Entry.COLUMN_NAME_PUBLISHED + " desc") // Sort
    }

    /**
     * Called when a previously created loader has finished its load. Move the Cursor returned by
     * the query into the [ListView] adapter. This refreshes the existing UI with the data in the
     * [Cursor]. To do this we just call the [SimpleCursorAdapter.changeCursor] method of
     * our field [mAdapter].
     *
     * @param cursorLoader The Loader that has finished.
     * @param cursor The data generated by the Loader.
     */
    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        mAdapter!!.changeCursor(cursor)
    }

    /**
     * Called when the [ContentObserver] defined for the content provider detects that data has
     * changed. The [ContentObserver] resets the loader, and then re-runs the loader. In the adapter,
     * set the [Cursor] value to `null`. This removes the reference to the Cursor, allowing it to be
     * garbage-collected. To do this we just call the [SimpleCursorAdapter.changeCursor] method with
     * `null` of our field [mAdapter].
     *
     * @param cursorLoader The [Loader] that is being reset.
     */
    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
        mAdapter!!.changeCursor(null)
    }

    /**
     * Create the ActionBar. First we call our super's implementation of `onCreateOptionsMenu`
     * then we save our [Menu] parameter [menu] in our field [mOptionsMenu]. Finally we use our
     * [MenuInflater] parameter [inflater] to inflate our menu layout `R.menu.main` into [menu].
     *
     * @param menu The options menu in which you place our items.
     * @param inflater [MenuInflater] to use to instantiate menu XML files into [Menu] objects
     */
    @Deprecated("Deprecated in Java")  // TODO: Replace with MenuProvider
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mOptionsMenu = menu
        inflater.inflate(R.menu.main, menu)
    }

    /**
     * Respond to user gestures on the ActionBar. We `when` switch on the id of our [MenuItem]
     * parameter [item] catching only `R.id.menu_refresh` in which case we call our method
     * [SyncUtils.TriggerRefresh] to trigger an immediate sync ("refresh") and return `true` to the
     * caller to consume the selection here. Otherwise we return the value returned by calling our
     * super's implementation of `onOptionsItemSelected`.
     *
     * @param item The menu item that was selected.
     * @return [Boolean]: Return `false` to allow normal menu processing to proceed, `true` to
     * consume it here.
     */
    @Deprecated("Deprecated in Java") // TODO: Replace with MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                SyncUtils.TriggerRefresh()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * This method will be called when an item in the list is selected, we then load the article in
     * the default browser. First we call our super's implementation of `onListItemClick`. We
     * initialize [Cursor] variable `val c` having our [SimpleCursorAdapter] field [mAdapter] return
     * its cursor positioned at the item at position [position]. We then initialize [String] variable
     * `val articleUrlString` by getting the string stored in the column [COLUMN_URL_STRING]
     * in [Cursor] `c`. If this is `null` we log the error and return. If it is not `null`, we log
     * the string, then create [Uri] variable `val articleURL` from it. We create [Intent] variable
     * `val i` with action [Intent.ACTION_VIEW] for the `articleURL`, and finally we start the
     * activity that will process [Intent] `i`.
     *
     * @param listView The ListView where the click happened
     * @param view The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id The row id of the item that was clicked
     */
    override fun onListItemClick(listView: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(listView, view, position, id)

        // Get a URI for the selected item, then start an Activity that displays the URI. Any
        // Activity that filters for ACTION_VIEW and a URI can accept this. In most cases, this will
        // be a browser.

        // Get the item at the selected position, in the form of a Cursor.
        val c = mAdapter!!.getItem(position) as Cursor
        // Get the link to the article represented by the item.
        val articleUrlString = c.getString(COLUMN_URL_STRING)
        if (articleUrlString == null) {
            Log.e(TAG, "Attempt to launch entry with null link")
            return
        }
        Log.i(TAG, "Opening URL: $articleUrlString")
        // Get a Uri object for the URL string
        val articleURL = articleUrlString.toUri()
        val i = Intent(Intent.ACTION_VIEW, articleURL)
        startActivity(i)
    }

    /**
     * Set the state of the Refresh button. If a sync is active, turn on the [ProgressBar] widget.
     * Otherwise, turn it off. If our [Menu] field [mOptionsMenu] is `null` we return having done
     * nothing. Otherwise we initialize [MenuItem] variable `val refreshItem` by finding the item in
     * [mOptionsMenu] with id `R.id.menu_refresh` and if this is not `null` we branch on the
     * value of our [Boolean] parameter [refreshing]:
     *
     *  * `true`: We set the action view of [MenuItem] `refreshItem` to the layout file
     *  `R.layout.actionbar_indeterminate_progress` (an indeterminateProgressStyle [ProgressBar]).
     *
     *  * `false`: We set the action view of [MenuItem] `refreshItem` to null
     *
     * @param refreshing `true` if an active sync is occurring, `false` otherwise
     */
    fun setRefreshActionButtonState(refreshing: Boolean) {
        if (mOptionsMenu == null) {
            return
        }
        val refreshItem: MenuItem = mOptionsMenu!!.findItem(R.id.menu_refresh)
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress)
            } else {
                @Suppress("DEPRECATION") // TODO: Fix setActionView deprecation
                MenuItemCompat.setActionView(refreshItem, null)
            }
        }
    }

    /**
     * Create a new anonymous [SyncStatusObserver]. It's attached to the app's [ContentResolver] in
     * [onResume], and removed in [onPause]. If status changes, it sets the state of the Refresh
     * button. If a sync is active or pending, the Refresh button is replaced by an indeterminate
     * ProgressBar; otherwise, the button itself is displayed.
     */
    private val mSyncStatusObserver = SyncStatusObserver  {
        requireActivity().runOnUiThread(Runnable {
            // Create a handle to the account that was created by
            // SyncService.CreateSyncAccount(). This will be used to query the system to
            // see how the sync status has changed.
            val account = GenericAccountService.GetAccount(SyncUtils.ACCOUNT_TYPE)
            if (account == null) {
                // GetAccount() returned an invalid value. This shouldn't happen, but
                // we'll set the status to "not refreshing".
                setRefreshActionButtonState(false)
                return@Runnable
            }

            // Test the ContentResolver to see if the sync adapter is active or pending.
            // Set the state of the refresh button accordingly.
            val syncActive = ContentResolver.isSyncActive(
                account, FeedContract.CONTENT_AUTHORITY)
            val syncPending = ContentResolver.isSyncPending(
                account, FeedContract.CONTENT_AUTHORITY)
            setRefreshActionButtonState(syncActive || syncPending)
        })
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "EntryListFragment"

        /**
         * Projection for querying the content provider.
         */
        private val PROJECTION = arrayOf(
            BaseColumns._ID, // Kotlin!!
            FeedContract.Entry.COLUMN_NAME_TITLE,
            FeedContract.Entry.COLUMN_NAME_LINK,
            FeedContract.Entry.COLUMN_NAME_PUBLISHED
        )

        // Column indexes. The index of a column in the Cursor is the same as its relative position
        // in the projection.

        /**
         * Column index for _ID
         */
        private const val COLUMN_ID = 0

        /**
         * Column index for title
         */
        private const val COLUMN_TITLE = 1

        /**
         * Column index for link
         */
        private const val COLUMN_URL_STRING = 2

        /**
         * Column index for published
         */
        private const val COLUMN_PUBLISHED = 3

        /**
         * List of Cursor columns to read from when preparing an adapter to populate the ListView.
         */
        private val FROM_COLUMNS = arrayOf(
            FeedContract.Entry.COLUMN_NAME_TITLE,
            FeedContract.Entry.COLUMN_NAME_PUBLISHED
        )

        /**
         * List of Views which will be populated by Cursor data.
         */
        private val TO_FIELDS = intArrayOf(
            android.R.id.text1,
            android.R.id.text2)
    }
}
