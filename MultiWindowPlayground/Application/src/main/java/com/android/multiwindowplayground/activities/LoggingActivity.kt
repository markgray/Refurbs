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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.android.multiwindowplayground.activities

import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.android.multiwindowplayground.R
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogNode
import com.example.android.common.logger.LogView
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * Activity that logs all key lifecycle callbacks to [Log].
 * Output is also logged to the UI into a [LogFragment] through [initializeLogging]
 * and [stopLogging].
 */
abstract class LoggingActivity : AppCompatActivity() {
    /**
     * TAG used for logging.
     */
    protected var mLogTag: String = javaClass.simpleName

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we log the fact that we were called.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(mLogTag, "onCreate")
    }

    /**
     * This is the same as [onPostCreate] but is called for activities
     * created with the attribute [android.R.attr.persistableMode] set to
     * `persistAcrossReboots`. First we call through to our super's implementation of
     * `onCreate`, then we log the fact that we were called.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     * @param persistentState The data coming from the PersistableBundle first
     * saved in [.onSaveInstanceState]. We do not override
     * `onSaveInstanceState` so do not use.
     */
    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        Log.d(mLogTag, "onPostCreate")
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but
     * has not (yet) been killed. First we call through to our super's implementation of `onPause`,
     * then we log the fact that we were called.
     */
    override fun onPause() {
        super.onPause()
        Log.d(mLogTag, "onPause")
    }

    /**
     * Perform any final cleanup before an activity is destroyed. First we call through to our
     * super's implementation of `onDestroy`, then we log the fact that we were called.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(mLogTag, "onDestroy")
    }

    /**
     * Called after [onRestoreInstanceState], [onRestart], or [onPause], for your activity to start
     * interacting with the user. First we call through to our super's implementation of `onResume`,
     * then we log the fact that we were called.
     */
    override fun onResume() {
        super.onResume()
        Log.d(mLogTag, "onResume")
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     * First we call through to our super's implementation of `onConfigurationChanged`, then
     * we log the fact that we were called.
     *
     * @param newConfig The new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(mLogTag, "onConfigurationChanged: $newConfig")
    }

    /**
     * Called when activity start-up is complete (after [onStart] and [onRestoreInstanceState]
     * have been called). First we call through to our super's implementation of `onPostCreate`,
     * then we log the fact that we were called.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Log.d(mLogTag, "onPostCreate")
    }

    /**
     * Called after [onCreate]  or after [onRestart] when the activity had been stopped, but is now
     * again being displayed to the user.  It will be followed by [onResume]. First we call through
     * to our super's implementation of `onPostCreate`, next we call our method [initializeLogging]
     * to set up targets to receive log data. Finally we log the fact that we were called.
     */
    override fun onStart() {
        super.onStart()
        // Start logging to UI.
        initializeLogging()
        Log.d(mLogTag, "onStart")
    }

    /**
     * Called when you are no longer visible to the user. First we call through to our super's
     * implementation of `onStop`, next we call our method [stopLogging] to stop our logging
     * (it does this by setting the [LogNode] the data will be sent to ([Log.logNode]) to `null`).
     * Finally we log the fact that we were called (since the [LogNode] data will be sent to is
     * `null`, nothing happens, but why not).
     */
    override fun onStop() {
        super.onStop()
        // Stop logging to UI when this activity is stopped.
        stopLogging()
        Log.d(mLogTag, "onStop")
    }

    /**
     * Called by the system when the activity changes from fullscreen mode to multi-window mode and
     * visa-versa. First we call through to our super's implementation of `onMultiWindowModeChanged`,
     * then we log the fact that we were called.
     *
     * @param isInMultiWindowMode `true` if the activity is in multi-window mode.
     */
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        Log.d(mLogTag, "onMultiWindowModeChanged: $isInMultiWindowMode")
    }

    // Logging and UI methods below.

    /**
     * Set up targets to receive log data. We initialize [LogWrapper] variable `val logWrapper` with
     * a new instance, and set it as the [LogNode] that log data will be sent to. We create a new
     * instance for [MessageOnlyLogFilter] variable `val msgFilter` (strips out everything except
     * the message text) and set it as the [LogNode] that `logWrapper` will next send data to. We
     * then create [LogFragment] variable `val logFragment` by using the [FragmentManager] for
     * interacting with fragments associated with this activity to find the fragment with the
     * resource id `R.id.log_fragment`, and set its [LogView] as the [LogNode] that `msgFilter`
     * will send data to.
     */
    fun initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        val logWrapper = LogWrapper()
        Log.logNode =logWrapper

        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter

        // On screen logging via a fragment with a TextView.
        val logFragment = supportFragmentManager
            .findFragmentById(R.id.log_fragment) as LogFragment?
        msgFilter.next = logFragment!!.logView
    }

    /**
     * Stops logging. We set the [LogNode] that log data will be sent to to `null`.
     */
    fun stopLogging() {
        Log.logNode = null
    }

    /**
     * Set the description text if a TextView with the resource id `R.id.description` is available.
     * We initialize [TextView] variable `val description` by finding the view with id
     * `R.id.description` and if this is not `null` we set its text to the string with the
     * resource id in our [Int] parameter [textId].
     *
     * @param textId resource ID of string we are to display
     */
    protected fun setDescription(@StringRes textId: Int) {
        // Set the text and background color
        val description = findViewById<TextView>(R.id.description)
        description?.setText(textId)
    }

    /**
     * Set the background color for the description text. We initialize [View] variable
     * `val scrollView` by finding the view with id `R.id.scrollview` and if this is not `null`
     * we set its background to the color with the resource ID in our [Int] parameter [colorId].
     *
     * @param colorId resource id of the color to use.
     */
    protected fun setBackgroundColor(@ColorRes colorId: Int) {
        val scrollView = findViewById<View>(R.id.scrollview)
        scrollView?.setBackgroundResource(colorId)
    }
}
