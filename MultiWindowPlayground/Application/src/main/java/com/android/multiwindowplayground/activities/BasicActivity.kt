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

import android.os.Bundle
import com.android.multiwindowplayground.R

/**
 * This activity is the most basic, simple use case and is to be launched in a new task without any
 * special flags or options.
 *
 * @see com.android.multiwindowplayground.MainActivity.onStartBasicActivity
 */
class BasicActivity : LoggingActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.activity_logging`.
     * We set our background color to `R.color.gray` (0x424242), then call the [setDescription]
     * method to set the description text in the view with id `R.id.description` to the string with
     * resource ID `R.string.activity_description_basic` ("This Activity was launched in a new task
     * without any additional flags or options.")
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)

        // Set the color and description
        setBackgroundColor(R.color.gray)
        setDescription(R.string.activity_description_basic)
    }
}
