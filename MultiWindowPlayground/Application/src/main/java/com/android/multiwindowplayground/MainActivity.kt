/*
 * Copyright (C) 2016 The Android Open Source Project
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
@file:Suppress("UNUSED_PARAMETER", "RedundantSuppression")

package com.android.multiwindowplayground

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.android.multiwindowplayground.activities.AdjacentActivity
import com.android.multiwindowplayground.activities.BasicActivity
import com.android.multiwindowplayground.activities.CustomConfigurationChangeActivity
import com.android.multiwindowplayground.activities.LaunchBoundsActivity
import com.android.multiwindowplayground.activities.LoggingActivity
import com.android.multiwindowplayground.activities.MinimumSizeActivity
import com.android.multiwindowplayground.activities.UnresizableActivity

/**
 * The launcher Activity that is started when the application is first started.
 */
class MainActivity : LoggingActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with
     * ID `R.id.root_view` then call [ViewCompat.setOnApplyWindowInsetsListener]
     * to take over the policy for applying window insets to `rootView`, with
     * the `listener` argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize [View] variable `val multiDisabledMessage` by finding the view with resource id
     * `R.id.warning_multiwindow_disabled`. If the [isInMultiWindowMode] method returns `false`
     * (the activity is NOT in multi-window mode) we set the visibility of `multiDisabledMessage`
     * to [View.VISIBLE], otherwise we set its visibility to [View.GONE].
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        val multiDisabledMessage = findViewById<View>(R.id.warning_multiwindow_disabled)
        // Display an additional message if the app is not in multi-window mode.
        if (!isInMultiWindowMode) {
            multiDisabledMessage.visibility = View.VISIBLE
        } else {
            multiDisabledMessage.visibility = View.GONE
        }
    }

    /**
     * Starts the activity [UnresizableActivity]. First we log the fact that we were called.
     * We initialize [Intent] variable `val intent` with a new instance intended to execute the
     * hard-coded class name [UnresizableActivity]. We set the flag [Intent.FLAG_ACTIVITY_NEW_TASK]
     * (the activity will become the start of a new task on the history stack, rather than replacing
     * us). We then call [startActivity] to launch the intent's activity.
     *
     * @param view the [View] that was clicked: the [Button] in our layout file with resource ID
     * `R.id.start_unresizable` ("Start unresizable Activity") specifies us as its [OnClickListener]
     * with an android:onClick="onStartUnresizableClick" attribute.
     */
    fun onStartUnresizableClick(view: View?) {
        Log.d(mLogTag, "** starting UnresizableActivity")

        /*
         * This activity is marked as 'unresizable' in the AndroidManifest. We need to specify the
         * FLAG_ACTIVITY_NEW_TASK flag here to launch it into a new task stack, otherwise the
         * properties from the root activity would have been inherited (which was here marked as
         * resizable by default).
        */
        val intent = Intent(this, UnresizableActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * Starts the activity [MinimumSizeActivity]. First we log the fact that we were called. We then
     * call [startActivity] to launch a new [Intent] intended to execute the hard-coded class name
     * [MinimumSizeActivity].
     *
     * @param view the [View] that was clicked: the [Button] in our layout file with resource ID
     * `R.id.start_minimumsize` ("Start Activity with minimum size") specifies us as its
     * [OnClickListener] with an android:onClick="onStartMinimumSizeActivity" attribute.
     */
    fun onStartMinimumSizeActivity(view: View?) {
        Log.d(mLogTag, "** starting MinimumSizeActivity")
        startActivity(Intent(this, MinimumSizeActivity::class.java))
    }

    /**
     * Starts the activity [AdjacentActivity]. First we log the fact that we were called. We
     * initialize [Intent] variable `val intent` with a new instance intended to execute the
     * hard-coded class name [AdjacentActivity]. We add to `intent` the flags
     * [Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT] (new activity will be displayed adjacent to the one
     * launching it) and [Intent.FLAG_ACTIVITY_NEW_TASK] (the activity will become the start of a
     * new task on the history stack, rather than replacing us). We then call [startActivity] to
     * launch the intent's activity.
     *
     * @param view the [View] that was clicked: the [Button] in our layout file with resource ID
     * `R.id.start_adjacent` ("Start Activity adjacent") specifies us as its [OnClickListener] with
     * an android:onClick="onStartAdjacentActivity" attribute.
     */
    fun onStartAdjacentActivity(view: View?) {
        Log.d(mLogTag, "** starting AdjacentActivity")

        /*
         * Start this activity adjacent to the focused activity (ie. this activity) if possible.
         * Note that this flag is just a hint to the system and may be ignored. For example,
         * if the activity is launched within the same task, it will be launched on top of the
         * previous activity that started the Intent. That's why the Intent.FLAG_ACTIVITY_NEW_TASK
         * flag is specified here in the intent - this will start the activity in a new task.
         */
        val intent = Intent(this, AdjacentActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Starts the activity [LaunchBoundsActivity] (free-form multi-window mode is required for this
     * to work, otherwise it just replaces this activity). First we log the fact that we were called.
     * Then we initialize [Rect] variable `val bounds` with a new instance whose upper left corner
     * is (500,300) and whose lower right corner is (100,0). We initialize [ActivityOptions] variable
     * `val options` with a basic [ActivityOptions] instance that has no special animation associated
     * with it. We then set the bounds (window size) that the activity should be launched in to
     * [Rect] variable `bounds`. We initialize [Intent] variable `val intent` with a new instance
     * intended to execute the hard-coded class name [LaunchBoundsActivity], and launch that new
     * activity with a [Bundle] made from `options` as its options [Bundle].
     *
     * @param view the [View] that was clicked: the [Button] in our layout file with resource ID
     * `R.id.start_launchbounds` ("Start Activity with launch bounds") specifies us as its
     * [OnClickListener] with an android:onClick="onStartLaunchBoundsActivity" attribute.
     */
    fun onStartLaunchBoundsActivity(view: View?) {
        Log.d(mLogTag, "** starting LaunchBoundsActivity")

        // Define the bounds in which the Activity will be launched into.
        val bounds = Rect(500, 300, 100, 0)

        // Set the bounds as an activity option.
        val options: ActivityOptions = ActivityOptions.makeBasic()
        options.launchBounds = bounds

        // Start the LaunchBoundsActivity with the specified options
        val intent = Intent(this, LaunchBoundsActivity::class.java)
        startActivity(intent, options.toBundle())
    }

    /**
     * Starts the activity [BasicActivity]. First we log the fact that we were called.
     * We then call [startActivity] to launch a new [Intent] intended to execute the
     * hard-coded class name [BasicActivity].
     *
     * @param view view that was clicked: the Button in our layout with ID button_start_basic ("Start
     * basic, default Activity") specifies us with an android:onClick="onStartBasicActivity"
     * attribute.
     */
    fun onStartBasicActivity(view: View?) {
        Log.d(mLogTag, "** starting BasicActivity")

        // Start an Activity with the default options in the 'singleTask' launch mode as defined in
        // the AndroidManifest.xml.
        startActivity(Intent(this, BasicActivity::class.java))
    }

    /**
     * Starts the activity [CustomConfigurationChangeActivity]. First we log the fact that we
     * were called. We then call [startActivity] to launch a new [Intent] intended to execute
     * the hard-coded class name [CustomConfigurationChangeActivity].
     *
     * @param view the [View] that was clicked: the [Button] in our layout file with resource
     * ID `R.id.start_customconfiguration` ("Start activity that handles configuration changes")
     * specifies us as its [OnClickListener] with an android:onClick="onStartCustomConfigurationActivity"
     * attribute.
     */
    fun onStartCustomConfigurationActivity(view: View?) {
        Log.d(mLogTag, "** starting CustomConfigurationChangeActivity")

        // Start an Activity that handles all configuration changes itself.
        startActivity(Intent(this, CustomConfigurationChangeActivity::class.java))
    }
}
