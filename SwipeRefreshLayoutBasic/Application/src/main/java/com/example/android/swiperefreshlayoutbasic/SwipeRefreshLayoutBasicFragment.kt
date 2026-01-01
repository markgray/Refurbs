/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "DEPRECATION")

package com.example.android.swiperefreshlayoutbasic

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.android.common.dummydata.Cheeses
import com.example.android.common.logger.Log

/**
 * A basic sample that shows how to use [SwipeRefreshLayout] to add the 'swipe-to-refresh' gesture
 * to a layout. In this sample, [SwipeRefreshLayout] contains a scrollable [android.widget.ListView]
 * as its only child. To provide an accessible way to trigger the refresh, this app also provides a
 * refresh action item. In this sample app, the refresh updates the [ListView] with a random set of
 * new items.
 */
class SwipeRefreshLayoutBasicFragment : Fragment() {
    /**
     * The [SwipeRefreshLayout] that detects swipe gestures and
     * triggers callbacks in the app.
     */
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    /**
     * The [android.widget.ListView] that displays the content that should be refreshed.
     */
    private var mListView: ListView? = null

    /**
     * The [android.widget.ListAdapter] used to populate the [android.widget.ListView]
     * defined in the previous statement.
     */
    private var mListAdapter: ArrayAdapter<String?>? = null

    /**
     * Called when the [Fragment] is starting. First we call through to our super's implementation
     * of `onCreate`, then we call the [setHasOptionsMenu] method with `true` to notify the system
     * to allow an options menu for this fragment.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notify the system to allow an options menu for this fragment.
        @Suppress("DEPRECATION") // TODO: Replace with MenuProvider
        setHasOptionsMenu(true)
    }

    /**
     * Inflates the [View] which will be displayed by this [Fragment] from the app's resources.
     * We initialize [View] variable `val view` with the [View] inflated by our [LayoutInflater]
     * parameter [inflater] from our layout file `R.layout.fragment_sample` using our [ViewGroup]
     * parameter [container] for the LayoutParams without attaching to it. We initialize our
     * [SwipeRefreshLayout] field [mSwipeRefreshLayout] by finding the [View] in `view` with id
     * `R.id.swipe_refresh`, and then set the color resources used in its progress animation from the
     * color resource id's `R.color.swipe_color_1`, `R.color.swipe_color_2`, `R.color.swipe_color_3`,
     * and `R.color.swipe_color_4`. We initialize our [ListView] field [mListView] by finding the
     * [View] in `view` with id [android.R.id.list] then return `view` to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here, we do not use.
     * @return Return the [View] for the fragment's UI
     */
    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(
            /*resource=*/ R.layout.fragment_sample,
            /*root=*/ container,
            /*attachToRoot=*/ false
        )

        // Retrieve the SwipeRefreshLayout and ListView instances
        mSwipeRefreshLayout = view.findViewById(/*id=*/ R.id.swipe_refresh)

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout!!.setColorSchemeResources(
            R.color.swipe_color_1, R.color.swipe_color_2,
            R.color.swipe_color_3, R.color.swipe_color_4)

        // Retrieve the ListView
        mListView = view.findViewById(/*id=*/ android.R.id.list)
        return view
    }

    /**
     * This is called after the [onCreateView] has finished. Here we can pick out the [View]s we
     * need to configure from the content view. First we call our super's implementation of
     * `onViewCreated`. We initialize our [ArrayAdapter] field [mListAdapter] with a new instance
     * which uses the [Context] of the [FragmentActivity] this fragment is currently associated
     * with, layout file [android.R.layout.simple_list_item_1] as the layout file to use when
     * instantiating views, [android.R.id.text1] as the id of the [TextView] within that layout
     * that is to be populated, and the [ArrayList] of [String] of [LIST_ITEM_COUNT] (20) random
     * cheeses returned by the [Cheeses.randomList] method of [Cheeses] for the objects to represent
     * in the [ListView]. We then set the adapter of our [ListView] field [mListView] to [mListAdapter].
     * Finally we set the [SwipeRefreshLayout.OnRefreshListener] of our [SwipeRefreshLayout] field
     * [mSwipeRefreshLayout] to a lambda whose `onRefresh` override logs that it was called and calls
     * our method [initiateRefresh] method to replace the dataset of [mListAdapter] with a new list
     * of [LIST_ITEM_COUNT] (20) random cheeses.
     *
     * @param view View created in [onCreateView]
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*
         * Create an ArrayAdapter to contain the data for the ListView. Each item in the ListView
         * uses the system-defined simple_list_item_1 layout that contains one TextView.
         */
        mListAdapter = ArrayAdapter(
            /*context=*/ requireActivity(),
            /*resource=*/ android.R.layout.simple_list_item_1,
            /*textViewResourceId=*/ android.R.id.text1,
            /*objects=*/ Cheeses.randomList(LIST_ITEM_COUNT) as List<String?>)

        // Set the adapter between the ListView and its backing data.
        mListView!!.adapter = mListAdapter

        /**
         * Implement [SwipeRefreshLayout.OnRefreshListener]. When users do the "swipe to refresh"
         * gesture, [SwipeRefreshLayout] invokes [OnRefreshListener.onRefresh]. In
         * [OnRefreshListener.onRefresh] call a method that refreshes the content. Call the same
         * method in response to the Refresh action from the action bar.
         */
        mSwipeRefreshLayout!!.setOnRefreshListener {
            Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout")
            initiateRefresh()
        }
    }

    /**
     * Create the ActionBar. We use our [MenuInflater] parameter [inflater] to inflate our menu
     * layout file `R.menu.main` into our [Menu] parameter [menu].
     *
     * @param menu     The options menu in which you place our items.
     * @param inflater [MenuInflater] to use to instantiate menu XML files into Menu objects
     */
    @Deprecated("Deprecated in Java") // TODO: Use MenuProvider
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    /**
     * Respond to the user's selection of the Refresh action item. Start the [SwipeRefreshLayout]
     * progress bar, then initiate the background task that refreshes the content. If the method
     * [MenuItem.getItemId] (kotlin `itemId` property) of our [MenuItem] parameter [item] returns
     * `R.id.menu_refresh`, we log the fact it was selected, and if our [SwipeRefreshLayout] field
     * [mSwipeRefreshLayout] is not currently refreshing we set it to be refreshing, call our method
     * [initiateRefresh] which launches a delaying task which when finished returns a new list of
     * [LIST_ITEM_COUNT] (20) random cheese names to [onRefreshComplete] which it uses to replace
     * the dataset of [ArrayAdapter] of [String] field [mListAdapter]. Having started the refresh
     * we return `true` to the caller to consume the event here.
     *
     * If the [MenuItem] id of [item] is not `R.id.menu_refresh`, we return the value returned by
     * our super's implementation of `onOptionsItemSelected`.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to proceed,
     * `true` to consume it here.
     */
    @Deprecated("Deprecated in Java") // TODO: Use MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_refresh) {
            Log.i(LOG_TAG, "Refresh menu item selected")

            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!mSwipeRefreshLayout!!.isRefreshing) {
                mSwipeRefreshLayout!!.isRefreshing = true
            }

            // Start our refresh background task
            initiateRefresh()
            return true
        }
        @Suppress("DEPRECATION") // TODO: Replace with MenuProvider
        return super.onOptionsItemSelected(item)
    }

    /**
     * By abstracting the refresh process to a single method, the app allows both the
     * [OnRefreshListener.onRefresh] method and the Refresh action item to refresh the content.
     *
     * First we log that we were called, then we create a new instance of [DummyBackgroundTask]
     * (custom [AsyncTask]) and immediately execute it in the background.
     */
    private fun initiateRefresh() {
        Log.i(LOG_TAG, "initiateRefresh")

        /**
         * Execute the background task, which uses [AsyncTask] to load the data.
         */
        DummyBackgroundTask().execute()
    }

    /**
     * When the [DummyBackgroundTask] custom [AsyncTask] finishes, it calls [onRefreshComplete],
     * which updates the data in the ListAdapter and turns off the progress bar. First we log the
     * fact that we were called. Then we remove all items from our [ArrayAdapter] of [String] field
     * [mListAdapter], and looping through all the [String] variable `var cheese` objects in our
     * [List] of [String] parameter [result] we add each `cheese` in turn to [mListAdapter]. Finally
     * we call the [SwipeRefreshLayout.setRefreshing] method (kotlin `isRefreshing` property) of
     * [SwipeRefreshLayout] field [mSwipeRefreshLayout] to stop the refreshing indicator.
     *
     * @param result new list of random cheeses.
     */
    private fun onRefreshComplete(result: List<String?>?) {
        Log.i(LOG_TAG, "onRefreshComplete")

        // Remove all items from the ListAdapter, and then replace them with the new items
        mListAdapter!!.clear()
        for (cheese in result!!) {
            mListAdapter!!.add(cheese)
        }

        // Stop the refreshing indicator
        mSwipeRefreshLayout!!.isRefreshing = false
    }

    /**
     * Dummy [AsyncTask] which simulates a long running task to fetch new cheeses.
     */
    @SuppressLint("StaticFieldLeak")
    private inner class DummyBackgroundTask : AsyncTask<Void?, Void?, List<String?>?>() {
        /**
         * We override this method to perform our computation on a background thread. Wrapped in try
         * block intended to catch and log [InterruptedException] we sleep for [TASK_DURATION] (3000)
         * milliseconds in order to simulate a background-task, then we return the list of
         * [LIST_ITEM_COUNT] (20) random cheeses returned by the [Cheeses.randomList] method of
         * [Cheeses] to the caller. (This list will be passed to our [onPostExecute] override which
         * will execute on the UI thread).
         *
         * @param params The parameters of the task, [Void] because we use none.
         * @return A result, defined by the subclass of this task.
         */
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<String?> {
            // Sleep for a small amount of time to simulate a background-task
            try {
                Thread.sleep(TASK_DURATION.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // Return a new random list of cheeses
            return Cheeses.randomList(LIST_ITEM_COUNT)
        }

        /**
         * Runs on the UI thread after [doInBackground]. The specified [result] parameter is the
         * value returned by [doInBackground]. First we call our super's implementation of
         * `onPostExecute` then we call the [SwipeRefreshLayoutBasicFragment.onRefreshComplete]
         * method of [SwipeRefreshLayoutBasicFragment] with our [List] of [String] parameter [result]
         * to have it replace the list of cheeses currently displayed with the new list.
         *
         * @param result The result of the operation computed by [.doInBackground].
         */
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: List<String?>?) {
            super.onPostExecute(result)

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(result)
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private val LOG_TAG = SwipeRefreshLayoutBasicFragment::class.java.simpleName

        /**
         * Number of items in our list of random cheeses.
         */
        private const val LIST_ITEM_COUNT = 20

        /**
         * Time in milliseconds that we sleep in order to simulate a background-task
         */
        const val TASK_DURATION: Int = 3 * 1000 // 3 seconds
    }
}
