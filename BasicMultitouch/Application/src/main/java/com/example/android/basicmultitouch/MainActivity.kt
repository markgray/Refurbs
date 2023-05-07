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
package com.example.android.basicmultitouch

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * This is an example of keeping track of individual touches across multiple [MotionEvent]s.
 *
 * This is illustrated by a [TouchDisplayView] custom [View] that responds to
 * touch events and draws coloured circles for each pointer, stores the last
 * positions of this pointer and draws them. This example shows the relationship
 * between [MotionEvent] indices, pointer identifiers and actions.
 *
 * @see android.view.MotionEvent
 */
class MainActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file [R.layout.layout_mainactivity]
     * (this layout file contains a [TouchDisplayView] widget overlayed over a [TextView] in a
     * shared [FrameLayout]).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_mainactivity)
    }
}