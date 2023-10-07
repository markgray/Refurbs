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
import android.content.Intent
import com.android.multiwindowplayground.R

/**
 * This Activity is to be launched adjacent to another Activity using the flag
 * [Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT]. If possible, it has been launched
 * into the adjacent area from the activity that started it. This is only a hint
 * to the system. For example - if the application is not in split-screen mode,
 * it will be launched full-screen. If it is launched in the same task as the
 * initial Activity, it will retain its activity properties and its location.
 *
 * @see com.android.multiwindowplayground.MainActivity.onStartAdjacentActivity
 */
class AdjacentActivity : LoggingActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file [R.layout.activity_logging].
     * We set our background color to [R.color.teal] (0x00695C), then call the [setDescription]
     * method to set the description text in the view with id [R.id.description] to the string with
     * resource ID [R.string.activity_adjacent_description] (See above comment for its value)
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)
        setBackgroundColor(R.color.teal)
        setDescription(R.string.activity_adjacent_description)
    }
}