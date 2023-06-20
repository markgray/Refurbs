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

package com.example.android.swiperefreshlayoutbasic;

import com.example.android.common.dummydata.Cheeses;
import com.example.android.common.logger.Log;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * A basic sample that shows how to use {@link android.support.v4.widget.SwipeRefreshLayout} to add
 * the 'swipe-to-refresh' gesture to a layout. In this sample, SwipeRefreshLayout contains a
 * scrollable {@link android.widget.ListView} as its only child.
 *
 * <p>To provide an accessible way to trigger the refresh, this app also provides a refresh
 * action item.
 *
 * <p>In this sample app, the refresh updates the ListView with a random set of new items.
 */
public class SwipeRefreshLayoutBasicFragment extends Fragment {

    /**
     * TAG used for logging.
     */
    private static final String LOG_TAG = SwipeRefreshLayoutBasicFragment.class.getSimpleName();

    /**
     * Number of items in our list of random cheeses.
     */
    private static final int LIST_ITEM_COUNT = 20;

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * The {@link android.widget.ListView} that displays the content that should be refreshed.
     */
    private ListView mListView;

    /**
     * The {@link android.widget.ListAdapter} used to populate the {@link android.widget.ListView}
     * defined in the previous statement.
     */
    private ArrayAdapter<String> mListAdapter;

    /**
     * Called when the {@code Fragment} is starting. First we call through to our super's implementation
     * of {@code onCreate}, then we call the {@code setHasOptionsMenu(true)} method to notify the system
     * to allow an options menu for this fragment.
     *
     * @param savedInstanceState we do not override {@code onSaveInstanceState} so do not use
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notify the system to allow an options menu for this fragment.
        setHasOptionsMenu(true);
    }

    /**
     * Inflates the {@link View} which will be displayed by this {@link Fragment}, from the app's
     * resources. We initialize {@code View view} with the {@code View} inflated by our parameter
     * {@code LayoutInflater inflater} from our layout file R.layout.fragment_sample using our parameter
     * {@code ViewGroup container} for the LayoutParams without attaching to it. We initialize our
     * field {@code SwipeRefreshLayout mSwipeRefreshLayout} by finding the view in {@code view} with
     * id R.id.swipe_refresh, and then set the color resources used in its progress animation from
     * the color resource id's R.color.swipe_color_1, R.color.swipe_color_2, R.color.swipe_color_3,
     * and R.color.swipe_color_4. We initialize our field {@code ListView mListView} by finding the
     * view in {@code view} with id android.R.id.list then return {@code view} tp the caller.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here, we do not use.
     * @return Return the View for the fragment's UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sample, container, false);

        // Retrieve the SwipeRefreshLayout and ListView instances
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh);


        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        // Retrieve the ListView
        mListView = view.findViewById(android.R.id.list);

        return view;
    }

    /**
     * This is called after the {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has finished.
     * Here we can pick out the {@link View}s we need to configure from the content view. First we call
     * our super's implementation of {@code onViewCreated}. We initialize our field {@code mListAdapter}
     * with a new instance which uses the {@code Context} of the {@code FragmentActivity} this fragment
     * is currently associated with, layout file android.R.layout.simple_list_item_1 as the layout file
     * to use when instantiating views, android.R.id.text1 as the id of the TextView within that layout
     * that is to be populated, and the {@code ArrayList<String>} of LIST_ITEM_COUNT (20) random cheeses
     * returned by the {@code randomList} method of {@code Cheeses} for the objects to represent in the
     * ListView. We then set the adapter of our field {@code ListView mListView} to {@code mListAdapter}.
     * Finally we set the {@code OnRefreshListener} of our field {@code SwipeRefreshLayout mSwipeRefreshLayout}
     * to an anonymous class whose {@code onRefresh} override logs that it was called and calls our method
     * {@code initiateRefresh} to replace the dataset of {@code mListAdapter} with a new list of LIST_ITEM_COUNT
     * (20) random cheeses.
     *
     * @param view View created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /*
         * Create an ArrayAdapter to contain the data for the ListView. Each item in the ListView
         * uses the system-defined simple_list_item_1 layout that contains one TextView.
         */
        //noinspection ConstantConditions
        mListAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                Cheeses.randomList(LIST_ITEM_COUNT));

        // Set the adapter between the ListView and its backing data.
        mListView.setAdapter(mListAdapter);

        /*
         * Implement {@link SwipeRefreshLayout.OnRefreshListener}. When users do the "swipe to
         * refresh" gesture, SwipeRefreshLayout invokes
         * {@link SwipeRefreshLayout.OnRefreshListener#onRefresh onRefresh()}. In
         * {@link SwipeRefreshLayout.OnRefreshListener#onRefresh onRefresh()}, call a method that
         * refreshes the content. Call the same method in response to the Refresh action from the
         * action bar.
         */
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout");

                initiateRefresh();
            }
        });
    }

    /**
     * Create the ActionBar. We use our parameter {@code MenuInflater inflater} to inflate our menu
     * layout file R.menu.main into our parameter {@code Menu menu}.
     *
     * @param menu     The options menu in which you place our items.
     * @param inflater {@code MenuInflater} to use to instantiate menu XML files into Menu objects
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    /**
     * Respond to the user's selection of the Refresh action item. Start the SwipeRefreshLayout
     * progress bar, then initiate the background task that refreshes the content. We switch on the
     * id of our parameter {@code MenuItem item} catching only R.id.menu_refresh. If it is our menu
     * item we log the fact it was selected, and if our field {@code SwipeRefreshLayout mSwipeRefreshLayout}
     * is not currently refreshing we set it to be refreshing, call our method {@code initiateRefresh}
     * to replace the dataset of {@code mListAdapter} with a new list of LIST_ITEM_COUNT (20) random
     * cheeses, and return true to the caller to consume the event here.
     * <p>
     * If the MenuItem id of item is not R.id.menu_refresh, we return the value returned by calling
     * our super's implementation of {@code onOptionsItemSelected}.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Log.i(LOG_TAG, "Refresh menu item selected");

                // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
                if (!mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(true);
                }

                // Start our refresh background task
                initiateRefresh();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * By abstracting the refresh process to a single method, the app allows both the
     * SwipeGestureLayout onRefresh() method and the Refresh action item to refresh the content.
     * <p>
     * First we log that we were called, then we create a new instance of the {@code AsyncTask}
     * {@code DummyBackgroundTask} and immediately execute it in the background.
     */
    private void initiateRefresh() {
        Log.i(LOG_TAG, "initiateRefresh");

        /*
         * Execute the background task, which uses {@link android.os.AsyncTask} to load the data.
         */
        new DummyBackgroundTask().execute();
    }

    /**
     * When the {@code DummyBackgroundTask} AsyncTask finishes, it calls onRefreshComplete(), which
     * updates the data in the ListAdapter and turns off the progress bar. First we log the fact that
     * we were called. Then we remove all items from our field {@code ArrayAdapter<String> mListAdapter},
     * and looping through all the {@code String cheese} objects in our parameter {@code List<String> result}
     * we add each {@code cheese} in turn to {@code mListAdapter}. Finally we call the {@code setRefreshing(false)}
     * method of {@code SwipeRefreshLayout mSwipeRefreshLayout} to stop the refreshing indicator.
     *
     * @param result new list of random cheeses.
     */
    private void onRefreshComplete(List<String> result) {
        Log.i(LOG_TAG, "onRefreshComplete");

        // Remove all items from the ListAdapter, and then replace them with the new items
        mListAdapter.clear();
        for (String cheese : result) {
            mListAdapter.add(cheese);
        }

        // Stop the refreshing indicator
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Dummy {@link AsyncTask} which simulates a long running task to fetch new cheeses.
     */
    @SuppressLint("StaticFieldLeak")
    private class DummyBackgroundTask extends AsyncTask<Void, Void, List<String>> {

        /**
         * Time in milliseconds that we sleep in order to simulate a background-task
         */
        static final int TASK_DURATION = 3 * 1000; // 3 seconds

        /**
         * We override this method to perform our computation on a background thread. Wrapped in try
         * block intended to catch and log InterruptedException we sleep for TASK_DURATION (3000)
         * milliseconds in order to simulate a background-task, then we return the list of LIST_ITEM_COUNT
         * (20) random cheeses returned by the {@code randomList} method of {@code Cheeses} to the caller.
         * (This list will be passed to our {@code onPostExecute} method which will execute on the UI
         * thread).
         *
         * @param params The parameters of the task, Void because we use none.
         * @return A result, defined by the subclass of this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            // Sleep for a small amount of time to simulate a background-task
            try {
                Thread.sleep(TASK_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Return a new random list of cheeses
            return Cheeses.randomList(LIST_ITEM_COUNT);
        }

        /**
         * Runs on the UI thread after {@link #doInBackground}. The specified result is the value
         * returned by {@link #doInBackground}. First we call our super's implementation of
         * {@code onPostExecute} then we call the {@code onRefreshComplete} method of
         * {@code SwipeRefreshLayoutBasicFragment} with our parameter {@code result} in order for
         * it to replace the list of cheeses currently displayed with the new list.
         *
         * @param result The result of the operation computed by {@link #doInBackground}.
         */
        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(result);
        }

    }
}
