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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.mediaeffects

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import com.example.android.common.activities.SampleActivityBase
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * [Fragment] which can display a view.
 *
 *
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
class MainActivity : SampleActivityBase() {
    /**
     * Flag for whether the Log Fragment is currently shown
     */
    private var mLogShown = false

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_main.
     * If our parameter `savedInstanceState` is null this is the first time we have been called
     * so we create `FragmentTransaction transaction` by using the FragmentManager for interacting
     * with fragments associated with this activity to begin a fragment transaction. We initialize
     * `MediaEffectsFragment fragment` with a new instance and add a request to `transaction`
     * for it to replace the fragment in the view with ID R.id.sample_content_fragment. Finally we commit
     * `transaction`.
     *
     * @param savedInstanceState if this is null, this is the first time we were called so we need
     * to create and add our `MediaEffectsFragment` fragment,
     * otherwise we are being called after a configuration change and the
     * system will restore the old fragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = MediaEffectsFragment()
            transaction.replace(R.id.sample_content_fragment, fragment)
            transaction.commit()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a `MenuInflater`
     * for this context to inflate our menu layout file R.menu.main into our parameter `Menu menu`,
     * then return true so the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Prepare the Screen's standard options menu to be displayed. We initialize `MenuItem logToggle`
     * by finding the item in our parameter `Menu menu` with id R.id.menu_toggle_log and if
     * the view with id R.id.sample_output is an instance of `ViewAnimator` we set it to visible.
     * If our flag `mLogShown` is true we set the title of `logToggle` to the string with
     * resource id R.string.sample_hide_log ("Hide Log") otherwise we set it to the string with resource
     * id R.string.sample_show_log ("Show Log"). Finally we return the value returned by our super's
     * implementation of `onPrepareOptionsMenu` to the caller.
     *
     * @param menu The options menu as last shown or first initialized by
     * onCreateOptionsMenu().
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val logToggle = menu.findItem(R.id.menu_toggle_log)
        logToggle.isVisible = findViewById<View>(R.id.sample_output) is ViewAnimator
        logToggle.setTitle(if (mLogShown) R.string.sample_hide_log else R.string.sample_show_log)
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the item id of our
     * parameter `MenuItem item` is R.id.menu_toggle_log we toggle the value of our flag
     * `mLogShown` and initialize `ViewAnimator output` by finding the view with id
     * R.id.sample_output. If `mLogShown` is now true set set the displayed child of `output`
     * to 1, otherwise we set it to 0. We then call the method `invalidateOptionsMenu` to declare
     * that the options menu has changed, so should be recreated, and return true to consume the event
     * here. If it is not our menu item we return the value returned by our super's implementation of
     * `onOptionsItemSelected`.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_toggle_log) {
            mLogShown = !mLogShown
            val output = findViewById<ViewAnimator>(R.id.sample_output)
            if (mLogShown) {
                output.displayedChild = 1
            } else {
                output.displayedChild = 0
            }
            invalidateOptionsMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Create a chain of targets that will receive log data. We initialize `LogWrapper logWrapper`
     * with a new instance, then set it to be the `LogNode` that data will be sent to. We initialize
     * `MessageOnlyLogFilter msgFilter` with a new instance, and set it to be the next node to
     * receive Log data after `logWrapper` has done its work (this will pipe data through `msgFilter`
     * which will filter out everything except the message text). We initialize `LogFragment logFragment`
     * by using the `FragmentManager` to find the fragment in our layout with id R.id.log_fragment.
     * We then set the next node to receive Log data after `msgFilter` to be the `LogView`
     * contained in `logFragment`. We then log the message "Ready".
     */
    override fun initializeLogging() {
        // Wraps Android's native log framework.
        val logWrapper = LogWrapper()
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.logNode = logWrapper

        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter

        // On screen logging via a fragment with a TextView.
        val logFragment = supportFragmentManager
            .findFragmentById(R.id.log_fragment) as LogFragment?
        msgFilter.next = logFragment!!.logView
        Log.i(TAG, "Ready")
    }

    companion object {
        /**
         * TAG used for logging
         */
        const val TAG: String = "MainActivity"
    }
}