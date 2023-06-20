/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.example.android.swiperefreshlayoutbasic;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewAnimator;

import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {

    /**
     * TAG used for logging
     */
    public static final String TAG = "MainActivity";

    /**
     * Whether the Log Fragment is currently shown
     */
    private boolean mLogShown;

    /**
     * Called when the activity is starting. We first call through to our super's implementation of
     * {@code onCreate}, then we set our content view to our layout file R.layout.activity_main. If
     * our parameter {@code savedInstanceState} is null, this is the first time we were called so we
     * use the FragmentManager for interacting with fragments associated with this activity to begin
     * {@code FragmentTransaction transaction}, initialize {@code SwipeRefreshLayoutBasicFragment fragment}
     * with a new instance and use {@code transaction} to replace (add) {@code fragment} to the container
     * view with ID R.id.sample_content_fragment in our layout. We then commit {@code transaction}.
     *
     * @param savedInstanceState If this is null we need to create and add our {@code SwipeRefreshLayoutBasicFragment}
     *                           fragment, if not null we are being recreated after a configuration
     *                           change so the fragment already exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            SwipeRefreshLayoutBasicFragment fragment = new SwipeRefreshLayoutBasicFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a {@code MenuInflater}
     * for this context to inflate our menu layout file R.menu.main into our parameter {@code Menu menu}
     * and return true so that the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Prepare the Screen's standard options menu to be displayed. We initialize {@code MenuItem logToggle}
     * by finding the menu item in our parameter {@code Menu menu} with id R.id.menu_toggle_log, set
     * it to visible only if the view in our layout with id R.id.sample_output is an instance of
     * {@code ViewAnimator}, and set its title to R.string.sample_hide_log ("Hide Log") if our flag
     * {@code mLogShown} is true or to R.string.sample_show_log ("Show Log") if it is false. Finally
     * we return the value returned by our super's implementation of {@code onPrepareOptionsMenu} to
     * the caller.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We use a switch to handle
     * only the {@code MenuItem} with id R.id.menu_toggle_log. If it is that item we toggle the value
     * of {@code mLogShown}, then initialize {@code ViewAnimator output} by finding the view with
     * ID R.id.sample_output. If {@code mLogShown} is true we set the displayed child of {@code output}
     * to 1, if it is false we we set the displayed child of {@code output} to 0. We then call the
     * method {@code invalidateOptionsMenu} to declare that the options menu has changed, so should
     * be recreated, and return true to the caller to consume the event here. If the item id is not
     * R.id.menu_toggle_log we return the value returned by our super's implementation of
     * {@code onOptionsItemSelected}.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create a chain of targets that will receive log data. We initialize {@code LogWrapper logWrapper}
     * with a new instance, and set it as the LogNode that log data will be sent to. We create a new
     * instance for {@code MessageOnlyLogFilter msgFilter} (strips out everything except the message
     * text) and set it as the LogNode that {@code logWrapper} will next send data to. We then initialize
     * {@code LogFragment logFragment} by using the FragmentManager for interacting with fragments
     * associated with this activity to find the fragment with the resource id R.id.log_fragment,
     * then set its {@code LogView} as the LogNode that {@code msgFilter} will send data to. Finally
     * we log the message "Ready".
     */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        //noinspection ConstantConditions
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }
}
