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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.example.android.requestduringlayout

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button

/**
 * This example shows what horrible things can result from calling requestLayout() during
 * a layout pass. DON'T DO THIS.
 *
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=HbAeTGoKG6k).
 */
class RequestDuringLayout : Activity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.activity_request_during_layout. We
     * initialize our variable `MyLayout myLayout` by finding the view with id R.id.container,
     * initialize `Button addViewButton` by finding the view with id R.id.addView ("Add"), initialize
     * `Button removeViewButton` by finding the view with id R.id.removeView ("Remove") and
     * initialize `Button forceLayoutButton` by finding the view with id R.id.forceLayout ("Layout").
     * Set the `OnClickListener` of `addViewButton` to an anonymous class which sets the
     * `mAddRequestPending` field of `myLayout` to true, then calls its `requestLayout`
     * method. Set the `OnClickListener` of `removeViewButton` to an anonymous class which
     * sets the `mRemoveRequestPending` field of `myLayout` to true, then calls its
     * `requestLayout` method. And we set the `OnClickListener` of `removeViewButton`
     * to an anonymous class which calls the `requestLayout` method of `myLayout`.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_during_layout)
        val myLayout = findViewById<MyLayout>(R.id.container)
        val addViewButton = findViewById<Button>(R.id.addView)
        val removeViewButton = findViewById<Button>(R.id.removeView)
        val forceLayoutButton = findViewById<Button>(R.id.forceLayout)
        addViewButton.setOnClickListener { v: View? ->
            myLayout.mAddRequestPending = true
            myLayout.requestLayout()
        }
        removeViewButton.setOnClickListener { v: View? ->
            myLayout.mRemoveRequestPending = true
            myLayout.requestLayout()
        }
        forceLayoutButton.setOnClickListener { v: View? -> myLayout.requestLayout() }
    }
}