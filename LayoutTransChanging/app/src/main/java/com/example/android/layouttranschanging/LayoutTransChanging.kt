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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")

package com.example.android.layouttranschanging

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.isNotEmpty

/**
 * This example shows how to use [LayoutTransition] to animate simple changes in a layout container.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [LayoutTransChanging video](https://www.youtube.com/watch?v=55wLsaWpQ4g).
 */
class LayoutTransChanging : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `insets` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument, then it updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `insets.left`, the right margin
     * set to `insets.right`, the top margin set to `insets.top`, and the bottom margin set to
     * `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED] to the caller
     * (so that the window insets will not keep passing down to descendant views).
     *
     * We initialize [Button] variable `val addButton` by finding the view with id `R.id.addButton`
     * ("Add Item"), [Button] variable `val removeButton` by finding the view with id
     * `R.id.removeButton` ("Remove Item"), and [LinearLayout] variable `val container` by finding
     * the view with id `R.id.container`. We initialize [Context] variable `val context` to 'this'.
     * We use repeat to call the [LinearLayout.addView] method of [LinearLayout] variable `container`
     * to add two new instances of [ColoredView] to `container`. We set the [OnClickListener]
     * of `addButton` to an anonymous class which adds a new instance of [ColoredView] at index 1 to
     * `container` when the [Button] is clicked, and set the [OnClickListener] of `removeButton` to
     * an anonymous class which removes the view at index 1 from `container` when the [Button] is
     * clicked (or the view at index 0 if it is the last one standing).
     *
     * We initialize [LayoutTransition] variable `val transition` to the [LayoutTransition] object
     * for the `container` [ViewGroup], then call its [LayoutTransition.enableTransitionType] method
     * to enable the [LayoutTransition.CHANGING] transition type (flag enabling the animation that
     * runs on those items that are changing due to a layout change not caused by items being added
     * to or removed from the container -- this is for the changing of the size of the [ColoredView]
     * views when they are clicked on).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
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

        val addButton = findViewById<Button>(R.id.addButton)
        val removeButton = findViewById<Button>(R.id.removeButton)
        val container = findViewById<LinearLayout>(R.id.container)
        val context: Context = this

        // Start with two views
        repeat(times = 2) {
            container.addView(ColoredView(this))
        }
        addButton.setOnClickListener { // Adding a view will cause a LayoutTransition animation
            // Note that this will crash if you remove all views then try to add one.
            container.addView(ColoredView(context), 1)
        }
        removeButton.setOnClickListener {
            if (container.isNotEmpty()) {
                // Removing a view will cause a LayoutTransition animation
                container.removeViewAt(Math.min(1, container.childCount - 1))
            }
        }

        // Note that this assumes a LayoutTransition is set on the container, which is the
        // case here because the container has the attribute "animateLayoutChanges" set to true
        // in the layout file. You can also call setLayoutTransition(new LayoutTransition()) in
        // code to set a LayoutTransition on any container.
        val transition = container.layoutTransition

        // New capability as of Jellybean; monitor the container for *all* layout changes
        // (not just add/remove/visibility changes) and animate these changes as well.
        transition.enableTransitionType(LayoutTransition.CHANGING)
    }

    /**
     * Custom view painted with a random background color and two different sizes which are
     * toggled between due to user interaction.
     *
         * Our constructor. First we call our super's constructor. In our `init` block we initialize
     * [Int] variable `val red`, [Int] variable `val green`, and [Int] variable `val blue` to random
     * numbers between 127 and 255, then initialize [Int] variable `val color` to a ARGB color formed
     * by shifting each to their proper position and or'ing them together along with 0xff for the
     * alpha channel. We then set our background color to `color`, set our layout params to
     * [LinearLayout.LayoutParams] field [mCompressedParams] and set our [OnClickListener] to a
     * lambda which will toggle our layout params between our [LinearLayout.LayoutParams] field
     * [mCompressedParams] and our [LinearLayout.LayoutParams] field [mExpandedParams] depending on
     * the value of [Boolean] field [mExpanded] then toggle [mExpanded], and request a new layout.
     *
     * @param context The [Context] the view is running in, through which it can access the
     * current theme, resources, etc.
     */
    class ColoredView(context: Context) : View(context) {
        /**
         * Flag to indicate whether we are in the expanded state (`true`) or not (`false`).
         */
        private var mExpanded = false

        /**
         * `LayoutParams` for the non-expanded state of our view.
         */
        private val mCompressedParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50)

        /**
         * `LayoutParams` for the expanded state of our view.
         */
        private val mExpandedParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)

        init {
            val red = (Math.random() * 128 + 127).toInt()
            val green = (Math.random() * 128 + 127).toInt()
            val blue = (Math.random() * 128 + 127).toInt()
            val color = (0xff shl 24) or (red shl 16) or (green shl 8) or blue
            setBackgroundColor(color)
            layoutParams = mCompressedParams
            setOnClickListener {
                // Size changes will cause a LayoutTransition animation if the CHANGING
                // transition is enabled
                layoutParams = if (mExpanded) mCompressedParams else mExpandedParams
                mExpanded = !mExpanded
                requestLayout()
            }
        }
    }
}
