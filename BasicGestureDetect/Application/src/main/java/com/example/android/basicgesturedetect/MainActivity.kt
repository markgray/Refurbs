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

package com.example.android.basicgesturedetect

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.android.common.activities.SampleActivityBase
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogNode
import com.example.android.common.logger.LogView
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * A simple launcher activity containing a summary sample description which is also the view whose
 * gestures are logged to a sample log fragment.
 */
class MainActivity : SampleActivityBase() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_main`.
     *
     * We initialize our [LinearLayout] variable `rootView`
     * to the view with ID `R.id.sample_main_layout` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * If the [FragmentManager] for interacting with fragments associated with this activity
     * cannot find a fragment with the tag [FRAGTAG] ("BasicGestureDetectFragment"), we initialize
     * our [FragmentTransaction] variable `val transaction` by using the fragment manager to begin
     * a transaction. We then initialize [BasicGestureDetectFragment] variable `val fragment` to a
     * new instance, use `transaction` to add `fragment` with the tag name [FRAGTAG] without
     * specifying a container for it to be placed in (it uses `findViewById` to find the view that
     * it interacts with). Then we commit the transaction.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView = findViewById<LinearLayout>(R.id.sample_main_layout)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        if (supportFragmentManager.findFragmentByTag(FRAGTAG) == null) {
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            val fragment = BasicGestureDetectFragment()
            transaction.add(fragment, FRAGTAG)
            transaction.commit()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a [MenuInflater] with
     * this context to inflate our menu layout file `R.menu.main` into our [Menu] parameter [menu]
     * and return `true` so that the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed, if you return `false` it will
     * not be shown. We return `true`.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Create a chain of targets that will receive log data. We initialize [LogWrapper] variable
     * `val logWrapper` with a new instance, and set it as the [LogNode] that log data will be sent
     * to. We create a new instance for [MessageOnlyLogFilter] variable `val msgFilter` (strips out
     * everything except the message text) and set it as the [LogNode] that `logWrapper` will next
     * send data to. We then initialize [LogFragment] variable `val logFragment` by using the
     * [FragmentManager] for interacting with fragments associated with this activity to find the
     * fragment with the resource id `R.id.log_fragment`, then set its [LogView] as the [LogNode]
     * that `msgFilter` will send data to. Finally we log the message "Ready".
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
         * TAG used for logging
         */
        const val TAG: String = "MainActivity"

        /**
         * Fragment tag used for our custom `Fragment`
         */
        const val FRAGTAG: String = "BasicGestureDetectFragment"
    }
}
