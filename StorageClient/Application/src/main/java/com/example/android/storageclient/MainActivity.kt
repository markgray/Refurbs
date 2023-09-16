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

package com.example.android.storageclient

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import com.example.android.common.activities.SampleActivityBase
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * A simple launcher activity containing a summary sample description
 * and a few action bar buttons.
 */
class MainActivity : SampleActivityBase() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_main. If
     * the FragmentManager for interacting with fragments associated with this activity is unable to
     * find a fragment with the tag FRAGTAG, we initialize `FragmentTransaction transaction` by
     * beginning it, create a new instance for `StorageClientFragment fragment`, add `fragment`
     * to `transaction` with the tag FRAGTAG and commit `transaction`.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (supportFragmentManager.findFragmentByTag(FRAGTAG) == null) {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = StorageClientFragment()
            transaction.add(fragment, FRAGTAG)
            transaction.commit()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a `MenuInflater`
     * for this context to inflate our menu layout file R.menu.main into our parameter `Menu menu`
     * and return true so the menu will be displayed.
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
     * Create a chain of targets that will receive log data. We initialize `LogWrapper logWrapper`
     * with a new instance, then set it to be the LogNode that data will be sent to. We initialize
     * `MessageOnlyLogFilter msgFilter` with a new instance and set it to be the LogNode that
     * `logWrapper` will send data to. We initialize `LogFragment logFragment` by using
     * the FragmentManager for interacting with fragments associated with this activity to find the
     * fragment whose container has the id R.id.log_fragment, and set its `LogView` to be the
     * LogNode that `msgFilter` will send data to. We then set the text appearance of the
     * `LogView` of `logFragment` to be that indicated by the style file R.style.Log,
     * and set its background color to WHITE. Finally we log the message "Ready".
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            logFragment.logView!!.setTextAppearance(R.style.Log)
        } else {
            logFragment.logView!!.setTextAppearance(this, R.style.Log)
        }
        logFragment.logView!!.setBackgroundColor(Color.WHITE)
        Log.i(TAG, "Ready")
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "MainActivity"

        /**
         * Fragment tag for our `StorageClientFragment` fragment.
         */
        const val FRAGTAG: String = "StorageClientFragment"
    }
}