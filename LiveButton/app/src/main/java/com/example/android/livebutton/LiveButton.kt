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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "MemberVisibilityCanBePrivate")

package com.example.android.livebutton

import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This app shows a simple application of anticipation and follow-through techniques as
 * the button animates into its pressed state and animates back out of it, overshooting
 * end state before resolving.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * [DevBytes](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0).
 * [LiveButton](https://www.youtube.com/watch?v=uQ7PTe7QMQM)
 */
class LiveButton : ComponentActivity() {
    /**
     * [DecelerateInterpolator] used for the ACTION_DOWN movement of the button
     */
    var sDecelerator: DecelerateInterpolator = DecelerateInterpolator()

    /**
     * [OvershootInterpolator] used for the ACTION_UP movement of the button, with an overshoot of 10f
     */
    var sOvershooter: OvershootInterpolator = OvershootInterpolator(10f)

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_main`.
     *
     * We initialize our [RelativeLayout] variable `rootView`
     * to the view with ID `R.id.root_view` then call
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
     * We initialize our [Button] variable `val clickMeButton` by finding the view with id
     * `R.id.clickMe` ("Click me!") and set the duration of its [ViewPropertyAnimator] to 200ms.
     * Finally we set its [OnTouchListener] to an anonymous class which animates its scaling
     * properties when an ACTION_DOWN or ACTION_UP [MotionEvent] is received.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overshoot)
        val rootView = findViewById<RelativeLayout>(R.id.root_view)
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
        val clickMeButton = findViewById<Button>(R.id.clickMe)
        clickMeButton.animate().duration = 200
        clickMeButton.setOnTouchListener { _, arg1 ->

            /**
             * Called when a touch event is dispatched to a view. This allows listeners to get a
             * chance to respond before the target view. If the action of our [MotionEvent] parameter
             * [arg1] is [MotionEvent.ACTION_DOWN] we set the [TimeInterpolator] of the
             * [ViewPropertyAnimator] of `clickMeButton` to [DecelerateInterpolator] field
             * [sDecelerator] and have it animate the `scaleX` property to .7f and the `scaleY`
             * property to .7f or if the action is [MotionEvent.ACTION_UP] we set the [TimeInterpolator]
             * of the [ViewPropertyAnimator] of `clickMeButton` to [OvershootInterpolator] field
             * [sOvershooter] and have it animate the `scaleX` property to 1f and the `scaleY`
             * property to 1f as well. Whether the action was one we wanted or not we return
             * `false` to the caller so that the event will be passed on to the view.
             *
             * @param _ The [View] the touch event has been dispatched to.
             * @param arg1 The [MotionEvent] object containing full information about the event.
             * @return `true` if the listener has consumed the event, `false` otherwise.
             */
            if (arg1.action == MotionEvent.ACTION_DOWN) {
                clickMeButton.animate().setInterpolator(sDecelerator).scaleX(.7f).scaleY(.7f)
            } else if (arg1.action == MotionEvent.ACTION_UP) {
                clickMeButton.animate().setInterpolator(sOvershooter).scaleX(1f).scaleY(1f)
            }
            false
        }
    }
}
