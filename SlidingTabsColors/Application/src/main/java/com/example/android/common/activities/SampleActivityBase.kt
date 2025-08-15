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
package com.example.android.common.activities

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogNode
import com.example.android.common.logger.LogWrapper

/**
 * Base launcher activity, to handle most of the common plumbing for samples.
 */
open class SampleActivityBase : FragmentActivity() {
    /**
     * Called when the activity is starting. We just call through to our super's implementation of
     * `onCreate`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    @Suppress("RedundantOverride", "RedundantSuppression")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Called after [onCreate]  or after [onRestart] when the activity had been stopped, but is now
     * again being displayed to the user. It will be followed by [onResume]. We call our super's
     * implementation of `onStart` then call our method [initializeLogging] to set up targets to
     * receive log data.
     */
    override fun onStart() {
        super.onStart()
        initializeLogging()
    }

    /**
     * Set up targets to receive log data. We initialize [LogWrapper] variable `val logWrapper` with
     * a new instance and set it to be the [LogNode] that data will be sent to. We then log the
     * message "Ready".
     */
    open fun initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        val logWrapper = LogWrapper()
        Log.logNode = logWrapper
        Log.i(TAG, "Ready")
    }

    companion object {
        /**
         * TAG used for logging
         */
        const val TAG: String = "SampleActivityBase"
    }
}