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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This example shows what horrible things can result from calling requestLayout() during
 * a layout pass. DON'T DO THIS.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=HbAeTGoKG6k).
 */
class RequestDuringLayout : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.activity_request_during_layout`.
     *
     * We initialize our [MyLayout] variable `myLayout` to the view with ID `R.id.container`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for
     * applying window insets to `myLayout`, with the `listener` argument a lambda that
     * accepts the [View] passed the lambda in variable `v` and the [WindowInsetsCompat]
     * passed the lambda in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize [Button] variable `val addViewButton` by finding the view with id
     * `R.id.addView` ("Add"), initialize [Button] variable `val removeViewButton` by finding
     * the view with id `R.id.removeView` ("Remove") and initialize [Button] variable
     * `val forceLayoutButton` by finding the view with id `R.id.forceLayout` ("Layout").
     * We set the [View.OnClickListener] of `addViewButton` to an anonymous class which sets the
     * [MyLayout.mAddRequestPending] field of `myLayout` to `true`, then calls its
     * `MyLayout.requestLayout` method, and we set the [View.OnClickListener] of `removeViewButton`
     * to an anonymous class which sets the [MyLayout.mRemoveRequestPending] field of `myLayout` to
     * `true`, then calls its [MyLayout.requestLayout] method. Finally we set the
     * [View.OnClickListener] of `forceLayoutButton` to an anonymous class which calls the
     *[ MyLayout.requestLayout] method of `myLayout`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_during_layout)
        val myLayout: MyLayout = findViewById(R.id.container)
        ViewCompat.setOnApplyWindowInsetsListener(myLayout) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        val addViewButton: Button = findViewById(R.id.addView)
        val removeViewButton: Button = findViewById(R.id.removeView)
        val forceLayoutButton: Button = findViewById(R.id.forceLayout)
        addViewButton.setOnClickListener { _: View? ->
            myLayout.mAddRequestPending = true
            myLayout.requestLayout()
        }
        removeViewButton.setOnClickListener { _: View? ->
            myLayout.mRemoveRequestPending = true
            myLayout.requestLayout()
        }
        forceLayoutButton.setOnClickListener { _: View? -> myLayout.requestLayout() }
    }
}
