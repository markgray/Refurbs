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

package com.example.android.common.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogWrapper;

/**
 * Base launcher activity, to handle most of the common plumbing for samples.
 */
@SuppressLint("Registered")
public class SampleActivityBase extends FragmentActivity {

    /**
     * TAG used for logging
     */
    public static final String TAG = "SampleActivityBase";

    /**
     * Called when the activity is starting. We just call through to our super's implementation of
     * {@code onCreate}.
     *
     * @param savedInstanceState we do not override {@code onSaveInstanceState} so do not use
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Called after {@link #onCreate} &mdash; or after {@link #onRestart} when
     * the activity had been stopped, but is now again being displayed to the
     * user.  It will be followed by {@link #onResume}. We call our super's implementation
     * of {@code onStart} then call our method {@code initializeLogging} to set
     * up targets to receive log data.
     */
    @Override
    protected  void onStart() {
        super.onStart();
        initializeLogging();
    }

    /**
     * Set up targets to receive log data. We initialize {@code LogWrapper logWrapper} with a new
     * instance and set it to be the LogNode that data will be sent to. We then log the message "Ready"
     */
    public void initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);

        Log.i(TAG, "Ready");
    }
}
