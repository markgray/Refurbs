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
 * This activity has a minimum size. It was launched into the top/end corner with a a default size
 * of 750dp by 500dp, with a minimum size of 750dp as defined in its layout attribute in the
 * AndroidManifest definition. Note that this Activity was launched in a different task, otherwise
 * the properties from the Activity that launched this one would have been applied.
 *
 * @see com.android.multiwindowplayground.MainActivity.onStartMinimumSizeActivity
 */
class MinimumSizeActivity : LoggingActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file [R.layout.activity_logging].
     * We set our background color to [R.color.pink] (0xC2185B), then call the [setDescription]
     * method to set the description text in the view with id [R.id.description] to the string with
     * resource ID [R.string.activity_minimum_description]
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)
        setBackgroundColor(R.color.pink)
        setDescription(R.string.activity_minimum_description)
    }
}