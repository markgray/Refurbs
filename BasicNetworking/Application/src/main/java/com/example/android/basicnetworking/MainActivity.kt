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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate", "DEPRECATION")

package com.example.android.basicnetworking

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogNode
import com.example.android.common.logger.LogView
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * Sample application demonstrating how to test whether a device is connected,
 * and if so, whether the connection happens to be wifi or mobile (it could be
 * something else).
 *
 * This sample uses the logging framework to display log output in the log
 * fragment ([LogFragment]).
 */
class MainActivity : AppCompatActivity() {
    /**
     * Reference to the [LogFragment] showing events, so we can clear it with a button as necessary.
     */
    private var mLogFragment: LogFragment? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.sample_main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize [SimpleTextFragment] variable `val introFragment` by finding the fragment
     * that was identified by the id `R.id.intro_fragment` when our layout file was inflated
     * from XML. We then call its [SimpleTextFragment.setText] method to have it set its text
     * to the string with resource id `R.string.intro_message` and then we retrieve its [TextView]
     * to call its [TextView.setTextSize] method to set its text size to 16.0 DIP. Finally we call
     * our method [initializeLogging] to create a chain of targets that will receive log data and
     * display them in the [LogFragment] which has id `R.id.log_fragment` in our layout file.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // Initialize text fragment that displays intro text.
        val introFragment =
            supportFragmentManager.findFragmentById(R.id.intro_fragment) as SimpleTextFragment?
        introFragment!!.setText(R.string.intro_message)
        introFragment.textView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f)

        // Initialize the logging framework.
        initializeLogging()
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We fetch a [MenuInflater]
     * for our activity's context and use it to inflate our menu layout `R.menu.main` into our [Menu]
     * parameter [menu] and return `true` to the caller so that the menu will be displayed.
     *
     * @param menu The options [Menu] in which to place our items.
     * @return We return `true` so the menu will be displayed
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * This hook is called whenever an item in our options menu is selected. We switch on the id of
     * our parameter `MenuItem item`:
     *
     *  * `R.id.test_action`: we call our method [checkNetworkConnection] to check whether the
     *  device is connected, and if so, whether the connection is wifi or mobile and to log the
     *  information. We then return `true` to the caller to consume the event.
     *
     *  * `R.id.clear_action`: We fetch the [LogView] from our [LogFragment] field [mLogFragment]
     *  and call its [LogView.setText] method (aka kotlin `text` property) to set its text the empty
     *  string, and then return `true` to the caller to consume the even.
     *
     * If the `item` did not have an id we are interested in, we return `false` to the caller to
     * allow normal menu processing to proceed.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.test_action -> {
                checkNetworkConnection()
                return true
            }

            R.id.clear_action -> {
                mLogFragment!!.logView!!.text = ""
                return true
            }
        }
        return false
    }

    /**
     * Check whether the device is connected, and if so, whether the connection is wifi or mobile
     * (it could be something else). We initialize [ConnectivityManager] variable `val connMgr` with
     * a handle to the system level service CONNECTIVITY_SERVICE. We then use it to fetch details
     * about the currently active default data network to initialize [NetworkInfo] variable
     * `val activeInfo`. If this is not `null`, and its [NetworkInfo.isConnected] method returns
     * `true` to indicate that network connectivity exists we set our [Boolean] field [wifiConnected]
     * to `true` if the the `type` of network to which the info in `activeInfo` pertains is TYPE_WIFI,
     * and our [Boolean] field [mobileConnected] to `true` if its type is TYPE_MOBILE. Then if:
     *
     *  * [wifiConnected] is true, we log the string `R.string.wifi_connection` ("The active
     *  connection is wifi")
     *
     *  * [mobileConnected] is true, we log the string R.string.mobile_connection ("The active
     *  connection is mobile")
     *
     * If we are not connected we log the string `R.string.no_wifi_or_mobile` ("No wireless or mobile
     * connection")
     */
    @Suppress("DEPRECATION") // TODO: Fix activeNetworkInfo deprecation
    private fun checkNetworkConnection() {
        val connMgr = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeInfo: NetworkInfo? = connMgr.activeNetworkInfo
        if (activeInfo != null && activeInfo.isConnected) {
            wifiConnected = activeInfo.type == ConnectivityManager.TYPE_WIFI
            mobileConnected = activeInfo.type == ConnectivityManager.TYPE_MOBILE
            if (wifiConnected) {
                Log.i(TAG, getString(R.string.wifi_connection))
            } else if (mobileConnected) {
                Log.i(TAG, getString(R.string.mobile_connection))
            }
        } else {
            Log.i(TAG, getString(R.string.no_wifi_or_mobile))
        }
    }

    /**
     * Create a chain of targets that will receive log data. We initialize [LogWrapper] variable
     * `val logWrapper` with a new instance and set the [Log.logNode] property to `logWrapper` to
     * have it be the [LogNode] that data will be sent to. We initialize [MessageOnlyLogFilter]
     * variable `val msgFilter` with a new instance and set it to be the `next` [LogNode] that
     * `logWrapper` will pipe data to. We locate our [LogFragment] by using the fragment manager
     * to find the fragment in our layout with id `R.id.log_fragment` to initialize our [mLogFragment]
     * field, and set its [LogView] to be the next [LogNode] that `msgFilter` pipes data to (which
     * causes all the messages we log to be displayed in that view).
     */
    fun initializeLogging() {

        // Using Log, front-end to the logging chain, emulates
        // android.util.log method signatures.

        // Wraps Android's native log framework
        val logWrapper = LogWrapper()
        Log.logNode = logWrapper

        // A filter that strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter

        // On screen logging via a fragment with a TextView.
        mLogFragment = supportFragmentManager.findFragmentById(R.id.log_fragment) as LogFragment?
        msgFilter.next = mLogFragment!!.logView
    }

    companion object {
        /**
         * TAG used for logging
         */
        const val TAG: String = "Basic Network Demo"

        /**
         * Whether there is a Wi-Fi connection.
         */
        private var wifiConnected = false

        /**
         * Whether there is a mobile connection.
         */
        private var mobileConnected = false
    }
}
