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

package com.example.android.swiperefreshlistfragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.ListFragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.android.common.dummydata.Cheeses
import com.example.android.common.logger.Log

/**
 * A sample which shows how to use [SwipeRefreshLayout] within a [ListFragment] to add the
 * 'swipe-to-refresh' gesture to a [ListView]. This is provided through the provided re-usable
 * [SwipeRefreshListFragment] class. To provide an accessible way to trigger the refresh, this
 * app also provides a refresh action item. This item should be displayed in the Action Bar's
 * overflow item. In this sample app, the refresh updates the [ListView] with a random set of
 * new items. This sample also provides the functionality to change the colors displayed in the
 * [SwipeRefreshLayout] through the options menu. This is meant to showcase the use of color
 * rather than being something that should be integrated into apps.
 */
class SwipeRefreshListFragmentFragment : SwipeRefreshListFragment() {
    /**
     * Called when the fragment is starting. First we call our super's implementation of `onCreate`
     * then we call the [setHasOptionsMenu] method with `true` to notify the system that this fragment
     * would like to participate in populating the options menu by receiving a call to
     * [onCreateOptionsMenu] and related methods.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notify the system to allow an options menu for this fragment.
        @Suppress("DEPRECATION") // TODO: Replace with MenuProvider
        setHasOptionsMenu(true)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. First we call our super's implementation of `onViewCreated`. We
     * initialize [ListAdapter] variable `val adapter` with a new [ArrayAdapter] constructed from
     * the [LIST_ITEM_COUNT] (20) random cheeses selected by the [Cheeses.randomList] method
     * of [Cheeses] using our [FragmentActivity] for [Context], the layout file with resource id
     * [android.R.layout.simple_list_item_1] to use when instantiating views, and the resource id
     * [android.R.id.text1] for the [TextView] within the layout resource to be populated. We then
     * set the list adapter of our [ListView] to `adapter`. Finally we call the
     * [SwipeRefreshListFragment.setOnRefreshListener] method of our [SwipeRefreshListFragment]
     * super to set its [OnRefreshListener] to an anonymous class whose [OnRefreshListener.onRefresh]
     * override calls our [initiateRefresh] method to initiate a background refresh of our list of
     * random cheeses.
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*
         * Create an ArrayAdapter to contain the data for the ListView. Each item in the ListView
         * uses the system-defined simple_list_item_1 layout that contains one TextView.
         */
        val adapter: ListAdapter = ArrayAdapter(
            /*context=*/ requireActivity(),
            /*resource=*/ android.R.layout.simple_list_item_1,
            /*textViewResourceId=*/ android.R.id.text1,
            /*objects=*/ Cheeses.randomList(LIST_ITEM_COUNT)
        )

        // Set the adapter between the ListView and its backing data.
        listAdapter = adapter

        /*
         * Implement SwipeRefreshLayout.OnRefreshListener. When users do the "swipe to refresh"
         *  gesture, SwipeRefreshLayout invokes SwipeRefreshLayout.OnRefreshListener.onRefresh().
         * In SwipeRefreshLayout.OnRefreshListener#onRefresh.onRefresh(), call a method that
         * refreshes the content. Call the same method in response to the Refresh action from the
         * action bar.
         */
        setOnRefreshListener {
            Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout")
            initiateRefresh()
        }
    }

    /**
     * Initialize the contents of the Fragment host's standard options menu. We just use our
     * [MenuInflater] parameter [inflater] to inflate our menu layout file `R.menu.main_menu`
     * into our [Menu] parameter [menu].
     *
     * @param menu The options menu in which you place your items.
     * @param inflater [MenuInflater] we can use to inflate an xml file.
     */
    @Deprecated("Deprecated in Java", ReplaceWith("inflater.inflate(R.menu.main_menu, menu)")) // TODO: Use MenuProvider
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(/*menuRes=*/ R.menu.main_menu, /*menu=*/ menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We `when` switch on
     * the value returned by the [MenuItem.getItemId] method (kotlin `itemId` property) of our
     * [MenuItem] parameter [item]:
     *
     *  * `R.id.menu_refresh`: we log the fact that we were selected, then if our super's method
     * [isRefreshing] returns false (we are not already refreshing) we  set [isRefreshing] to `true`
     * to start the [SwipeRefreshLayout] progress bar. We then call our [initiateRefresh] method to
     * start our refresh background task and return `true` to consume the event here.
     *
     *  * `R.id.menu_color_scheme_1`: we log the fact that we were selected, call the
     *  [MenuItem.setChecked] method (kotlin `isChecked` property) of our [MenuItem] parameter
     *  [item] to have it display a checked mark, call our super's [setColorScheme] method with
     *  four resource ids for colors for it to use when displaying the [SwipeRefreshLayout]
     *  progress bar, and return `true` to consume the event here.
     *
     *  * `R.id.menu_color_scheme_2`: we log the fact that we were selected, call the
     *  [MenuItem.setChecked] method (kotlin `isChecked` property) of our [MenuItem] parameter
     *  [item] to have it display a checked mark, call our super's [setColorScheme] method with
     *  four resource ids for colors for it to use when displaying the [SwipeRefreshLayout]
     *  progress bar, and return `true` to consume the event here.
     *
     *  * `R.id.menu_color_scheme_3`: we log the fact that we were selected, call the
     *  [MenuItem.setChecked] method (kotlin `isChecked` property) of our [MenuItem] parameter
     *  [item] to have it display a checked mark, call our super's [setColorScheme] method with
     *  four resource ids for colors for it to use when displaying the [SwipeRefreshLayout]
     *  progress bar, and return `true` to consume the event here.
     *
     * If the item id is not one we know about, we return the value returned by our super's
     * implementation of `onOptionsItemSelected` to the caller.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                Log.i(LOG_TAG, "Refresh menu item selected")

                // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
                if (!isRefreshing) {
                    isRefreshing = true
                }

                // Start our refresh background task
                initiateRefresh()
                return true
            }

            R.id.menu_color_scheme_1 -> {
                Log.i(LOG_TAG, "setColorScheme #1")
                item.isChecked = true

                // Change the colors displayed by the SwipeRefreshLayout by providing it with 4
                // color resource ids
                setColorScheme(
                    colorRes1 = R.color.color_scheme_1_1,
                    colorRes2 = R.color.color_scheme_1_2,
                    colorRes3 = R.color.color_scheme_1_3,
                    colorRes4 = R.color.color_scheme_1_4
                )
                return true
            }

            R.id.menu_color_scheme_2 -> {
                Log.i(LOG_TAG, "setColorScheme #2")
                item.isChecked = true

                // Change the colors displayed by the SwipeRefreshLayout by providing it with 4
                // color resource ids
                setColorScheme(
                    colorRes1 = R.color.color_scheme_2_1,
                    colorRes2 = R.color.color_scheme_2_2,
                    colorRes3 = R.color.color_scheme_2_3,
                    colorRes4 = R.color.color_scheme_2_4
                )
                return true
            }

            R.id.menu_color_scheme_3 -> {
                Log.i(LOG_TAG, "setColorScheme #3")
                item.isChecked = true

                // Change the colors displayed by the SwipeRefreshLayout by providing it with 4
                // color resource ids
                setColorScheme(
                    colorRes1 = R.color.color_scheme_3_1,
                    colorRes2 = R.color.color_scheme_3_2,
                    colorRes3 = R.color.color_scheme_3_3,
                    colorRes4 = R.color.color_scheme_3_4
                )
                return true
            }
        }
        @Suppress("DEPRECATION") // TODO: Replace with MenuProvider
        return super.onOptionsItemSelected(item)
    }

    /**
     * By abstracting the refresh process to a single method, the app allows both the
     * [OnRefreshListener.onRefresh] method and the Refresh action item to refresh the content.
     * We log the fact that we were called, then call the [DummyBackgroundTask.execute] method
     * of a new instance of [DummyBackgroundTask] to load our list of random cheeses in a
     * background [AsyncTask] thread.
     */
    private fun initiateRefresh() {
        Log.i(LOG_TAG, "initiateRefresh")

        /**
         * Execute the background task, which uses [AsyncTask] to load the data.
         */
        DummyBackgroundTask().execute()
    }

    /**
     * TODO: Continue here.
     * When the [AsyncTask] finishes, it calls [onRefreshComplete], which updates the data in the
     * [ListAdapter] and turns off the progress bar. First we log the fact that we were called. We
     * initialize our [ArrayAdapter] of [String] variable `val adapter` with a reference to the
     * [ListAdapter] associated with this fragment's [ListView], and call its [ArrayAdapter.clear]
     * method to remove all elements from its data set. We then loop through all of the [String]
     * variable `val cheese` in our [List] of [String] parameter [result] adding each `cheese` to
     * `adapter`. Finally we set the [isRefreshing] property of our super to `false` to stop the
     * refreshing progress indicator.
     *
     * @param result [List] of 20 random cheeses names to use as the data set for our [ListAdapter].
     */
    private fun onRefreshComplete(result: List<String?>?) {
        Log.i(LOG_TAG, "onRefreshComplete")

        // Remove all items from the ListAdapter, and then replace them with the new items
        @Suppress("UNCHECKED_CAST")
        val adapter = listAdapter as ArrayAdapter<String>?
        adapter!!.clear()
        for (cheese in result!!) {
            adapter.add(cheese)
        }

        // Stop the refreshing indicator
        isRefreshing = false
    }

    /**
     * Dummy [AsyncTask] which simulates a long running task to fetch new cheeses.
     */
    @SuppressLint("StaticFieldLeak")
    private inner class DummyBackgroundTask : AsyncTask<Void?, Void?, List<String?>?>() {
        /**
         * Override this method to perform a computation on a background thread. The specified
         * parameters are the parameters passed to [execute] by the caller of this task (none in
         * our case). Wrapped in a try block intended to catch and log [InterruptedException] we
         * sleep for [TASK_DURATION] (3000ms) then we return the list of [LIST_ITEM_COUNT] (20)
         * random cheeses generated by the [Cheeses.randomList] method.
         *
         * @param params The parameters of the task, unused in our case.
         * @return a list of [LIST_ITEM_COUNT] (20) random cheeses.
         */
        @Suppress("RedundantNullableReturnType")
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<String?>? {
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
         * Runs on the UI thread after [doInBackground]. The specified result is the value
         * returned by [doInBackground]. First we call our super's implementation of
         * `onPostExecute`, then we call our [onRefreshComplete] method to update the
         * data in the [ListAdapter] and turn off the progress bar.
         *
         * @param result The result of the operation computed by [doInBackground].
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
        private val LOG_TAG = SwipeRefreshListFragmentFragment::class.java.simpleName

        /**
         * Number of random cheeses in our list of cheeses.
         */
        private const val LIST_ITEM_COUNT = 20

        /**
         * Time to kill before returning the list of random cheeses.
         */
        const val TASK_DURATION: Int = 3 * 1000 // 3 seconds
    }
}
