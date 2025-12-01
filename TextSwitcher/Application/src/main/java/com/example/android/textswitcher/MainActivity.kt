/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.textswitcher

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.ViewSwitcher
import android.widget.ViewSwitcher.ViewFactory
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This sample shows the use of the [TextSwitcher] View with animations. A [TextSwitcher] is
 * a special type of [ViewSwitcher] that animates the current text out and new text in when
 * [TextSwitcher.setText] is called.
 */
class MainActivity : ComponentActivity() {
    /**
     * [TextSwitcher] view in our layout with ID `R.id.switcher`
     */
    private var mSwitcher: TextSwitcher? = null

    /**
     * Current counter value that we display in our [TextSwitcher] field [mSwitcher]
     */
    private var mCounter = 0

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.sample_main`.
     *
     * We initialize our [LinearLayout] variable `rootView`
     * to the view with ID `R.id.LinearLayout1` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [TextSwitcher] field [mSwitcher] by finding the view with id
     * `R.id.switcher`, then set the factory used to create the two views between which
     * [mSwitcher] will flip to our [ViewFactory] field  [mFactory]. We load [Animation]
     * variable `val fadeIn` with the animation with resource id [android.R.anim.fade_in],
     * and [Animation] variable `val fadeOut` with the animation with resource id
     * [android.R.anim.fade_out]. We then set the animation started when a [View] enters
     * the screen of [mSwitcher] to `fadeIn` and the animation started when a [View] exits
     * the screen to `fadeOut`. We initialize [Button] variable `val nextButton` by finding
     * the view in our layout with id `R.id.button` and set its [View.OnClickListener] to a
     * lambda which increments our [Int] field [mCounter], then sets the text of the next view of
     * [mSwitcher] to the string value of [mCounter] and switches to the next view (this will
     * animate the old text out and animate the next text in). Finally we set the text of the
     * text view that is currently showing to the string value of [mCounter] (this does not perform
     * the animations).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)
        val rootView = findViewById<LinearLayout>(R.id.LinearLayout1)
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

        // Get the TextSwitcher view from the layout
        mSwitcher = findViewById(R.id.switcher)

        // Set the factory used to create TextViews to switch between.
        mSwitcher!!.setFactory(mFactory)

        /*
         * Set the in and out animations. Using the fade_in/out animations
         * provided by the framework.
         */
        val fadeIn: Animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val fadeOut: Animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        mSwitcher!!.inAnimation = fadeIn
        mSwitcher!!.outAnimation = fadeOut

        /*
         * Setup the 'next' button. The counter is incremented when clicked and
         * the new value is displayed in the TextSwitcher. The change of text is
         * automatically animated using the in/out animations set above.
         */
        val nextButton: Button = findViewById(R.id.button)
        nextButton.setOnClickListener {
            mCounter++
            mSwitcher!!.setText(mCounter.toString())
        }

        // Set the initial text without an animation
        mSwitcher!!.setCurrentText(mCounter.toString())
    }

    /**
     * The [ViewFactory] used to create [TextView]s that the [TextSwitcher] will switch between.
     */
    private val mFactory: ViewFactory =
        /**
         * Creates a new [View] to be added in a [ViewSwitcher]. We initialize [TextView] variable
         * `val t` with a new instance constructed using our [MainActivity] for the [Context], set
         * its gravity to [Gravity.TOP] and [Gravity.CENTER_HORIZONTAL], and set its text appearance
         * to [android.R.style.TextAppearance_Large] (a textSize of 22sp). Finally we return `t` to
         * the caller.
         *
         * @return a [View]
         */
        ViewFactory { // Create a new TextView
            val t = TextView(this@MainActivity)
            t.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                t.setTextAppearance(android.R.style.TextAppearance_Large)
            } else {
                @Suppress("DEPRECATION") // Needed for SDK older than M
                t.setTextAppearance(this@MainActivity, android.R.style.TextAppearance_Large)
            }
            t
        }
}
