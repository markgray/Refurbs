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
 * This activity handles configuration changes itself. The list of configuration changes that are
 * supported is defined in its AndroidManifest definition. Each configuration change triggers a
 * call to [.onConfigurationChanged], which is logged in the [ ].
 *
 * @see com.android.multiwindowplayground.MainActivity.onStartCustomConfigurationActivity
 */
class CustomConfigurationChangeActivity : LoggingActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_logging.
     * We set our background color to R.color.cyan (0x00838F), then call the `setDescription`
     * method to set the description text in the view with id R.id.description to the string with
     * resource ID R.string.activity_custom_description
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)
        setBackgroundColor(R.color.cyan)
        setDescription(R.string.activity_custom_description)
    }

    /**
     * Called by the system when the device configuration changes while your activity is running. We
     * just call through to our super's implementation of `onConfigurationChanged`.
     *
     * @param newConfig The new device configuration.
     */
    @Suppress("RedundantOverride")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        /*
        Note: The implementation in LoggingActivity logs the output of the new configuration.
        This callback is received whenever the configuration is updated, for example when the
        size of this Activity is changed.
         */
    }
}