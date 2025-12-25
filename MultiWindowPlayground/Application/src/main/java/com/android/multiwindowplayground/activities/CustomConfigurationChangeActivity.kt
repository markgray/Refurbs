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
package com.android.multiwindowplayground.activities

import android.content.res.Configuration
import android.os.Bundle
import com.android.multiwindowplayground.R

/**
 * This activity handles configuration changes itself. In the AndroidManifest, this activity
 * has been configured to receive callbacks for screenSize, smallestScreenSize, screenLayout,
 * and orientation changes. Try resizing this activity to different sizes to see which
 * configuration properties change. Each configuration change triggers a call to
 * [onConfigurationChanged], which is logged in the our [LoggingActivity] super's
 * override of `onConfigurationChanged`.
 *
 * @see com.android.multiwindowplayground.MainActivity.onStartCustomConfigurationActivity
 */
class CustomConfigurationChangeActivity : LoggingActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.activity_logging`.
     * We set our background color to `R.color.cyan` (0x00838F), then call the [setDescription]
     * method to set the description text in the view with id `R.id.description` to the string with
     * resource ID `R.string.activity_custom_description`
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)
        setBackgroundColor(R.color.cyan)
        setDescription(R.string.activity_custom_description)
    }

    /**
     * Called by the system when the device configuration changes while your activity is running. We
     * just call through to our super's ([LoggingActivity]) implementation of `onConfigurationChanged`.
     *
     * @param newConfig The new device [Configuration].
     */
    @Suppress("RedundantOverride", "RedundantSuppression")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        /*
        Note: The implementation in LoggingActivity logs the output of the new configuration.
        This callback is received whenever the configuration is updated, for example when the
        size of this Activity is changed.
         */
    }
}
