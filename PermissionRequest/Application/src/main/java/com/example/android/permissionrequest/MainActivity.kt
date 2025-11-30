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

package com.example.android.permissionrequest

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ViewAnimator
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
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
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * [Fragment] which can display a view. For devices with displays with a width of 720dp or greater,
 * the sample log is always visible, on other devices it's visibility is controlled by an item on
 * the Action Bar.
 *
 * NOTE: see AndroidManifest.xml for the android:networkSecurityConfig="@xml/network_security_config"
 * attribute as well as the file xml/network_security_config.xml, which are required to allow our
 * SimpleWebServer to use http instead of https!
 */
class MainActivity : SampleActivityBase() {
    /**
     * Whether the Log Fragment is currently shown
     */
    private var mLogShown = false

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID
     * `R.id.sample_main_layout` then call [ViewCompat.setOnApplyWindowInsetsListener]
     * to take over the policy for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda in variable `v` and the
     * [WindowInsetsCompat] passed the lambda in variable `windowInsets`. It initializes
     * its [Insets] variable `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets`
     * with [WindowInsetsCompat.Type.systemBars] as the argument, then it updates the layout
     * parameters of `v` to be a [ViewGroup.MarginLayoutParams] with the left margin set to
     * `insets.left`, the right margin set to `insets.right`, the top margin set to `insets.top`,
     * and the bottom margin set to `insets.bottom`. Finally it returns
     * [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets will not keep
     * passing down to descendant views).
     *
     * If our [Bundle] parameter [savedInstanceState] is `null`, this is the first time we were
     * called so we use the [FragmentManager] for interacting with  fragments associated with this
     * activity to begin [FragmentTransaction] variable `val transaction`. We create a new instance
     * of [PermissionRequestFragment] to initialize variable `val fragment` then use it to replace
     * the contents of the view with id `R.id.sample_content_fragment` as part of `transaction`.
     * Finally we commit `transaction`. If [savedInstanceState] is not `null` we are being
     * called after a configuration change so the system will have restored the old fragment.
     *
     * @param savedInstanceState when this is `null` this is the first time we have been called so
     * we need to create and add our [PermissionRequestFragment].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        // TODO: Move text down.
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
        if (savedInstanceState == null) {
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            val fragment = PermissionRequestFragment()
            transaction.replace(R.id.sample_content_fragment, fragment)
            transaction.commit()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a [MenuInflater] for
     * this context to inflate our menu layout file `R.menu.main` into our [Menu] parameter [menu]
     * and return `true` to the caller so our menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed;
     * if you return `false` it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Prepare the Screen's standard options menu to be displayed. This is called right before the
     * menu is shown, every time it is shown. We initialize [MenuItem] variable `val logToggle` by
     * finding the item with id `R.id.menu_toggle_log` in our [Menu] parameter [menu]. If the view
     * with id `R.id.sample_output` is an instance of [ViewAnimator] we set `logToggle` to visible
     * (otherwise we set it invisible). If our [Boolean] flag field [mLogShown] is `true` we set the
     * title of `logToggle` to the string with id `R.string.sample_hide_log` ("Hide Log") otherwise
     * we set it to the string with id `R.string.sample_show_log` ("Show Log"). We then return the
     * value returned by our super's `onPrepareOptionsMenu` to the caller.
     *
     * @param menu The options menu as last shown or first initialized by [onCreateOptionsMenu].
     * @return You must return `true` for the menu to be displayed;  If you return `false` it will
     * not be shown.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val logToggle = menu.findItem(R.id.menu_toggle_log)
        logToggle.isVisible = findViewById<View>(R.id.sample_output) is ViewAnimator
        logToggle.setTitle(if (mLogShown) R.string.sample_hide_log else R.string.sample_show_log)
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the id of our
     * [MenuItem] parameter [item] is `R.id.menu_toggle_log`, we toggle the value of our [Boolean]
     * flag field [mLogShown], then we initialize [ViewAnimator] variable `val output` by finding
     * the view with id `R.id.sample_output`. We branch on the value of our flag [mLogShown]:
     *
     *  * `true`: we set the displayed child of `output` to 1
     *
     *  * `false`: we set the displayed child of `output` to 0
     *
     * We then call the [invalidateOptionsMenu] method to declare that the options menu has changed,
     * so should be recreated and return true to consume the event here
     *
     * If the [MenuItem] is not one we recognize we return the value returned by our super's
     * `onOptionsItemSelected` method to the caller.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_toggle_log) {
            mLogShown = !mLogShown
            val output: ViewAnimator = findViewById(R.id.sample_output)
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
     * Create a chain of targets that will receive log data. We initialize [LogWrapper] variable
     * `val logWrapper` with a new instance and set it to be the [LogNode] that data will be sent
     * to. We initialize [MessageOnlyLogFilter] variable `val msgFilter` with a new instance to
     * strip out everything except the message text and set it to be the [LogNode] that data will
     * be sent to by `logWrapper`. We initialize [LogFragment] variable `val logFragment` by using
     * the [FragmentManager] for interacting with fragments associated with this activity to find
     * the fragment with id `R.id.log_fragment`, and set its [LogView] to be the [LogNode] that
     * data will be sent to by `msgFilter`.
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
         * TAG used for logging.
         */
        const val TAG: String = "MainActivity"
    }
}
