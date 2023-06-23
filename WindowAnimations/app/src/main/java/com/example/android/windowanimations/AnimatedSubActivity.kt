/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.example.android.windowanimations

import android.app.Activity
import android.os.Bundle

/**
 * See WindowAnimations.java for comments on the overall application.
 *
 *
 * This is a sub-activity which provides custom animation behavior. When this activity
 * is exited, the user will see the behavior specified in the overridePendingTransition() call.
 */
class AnimatedSubActivity : Activity() {
    /**
     * Called when the activity is starting. We just call our super's implementation of `onCreate`,
     * and set our content view to our layout file R.layout.activity_window_anim_sub.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_anim_sub)
    }

    /**
     * Called when your activity is done and should be closed. First we call our super's implementation
     * of `finish`, then we call the `overridePendingTransition` method to specify the use
     * of the xml animation with resource id R.anim.slide_in_right for the incoming activity, and
     * R.anim.slide_out_right for the outgoing activity.
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
    }
}