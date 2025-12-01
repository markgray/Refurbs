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
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * See [WindowAnimations] for comments on the overall application.
 *
 * This is a sub-activity which provides custom animation behavior. When this activity is exited,
 * the user will see the behavior specified in either the [overrideActivityTransition] call (if the
 * [Build.VERSION.SDK_INT] is 34 or greater) or the [overridePendingTransition] call (if the
 * [Build.VERSION.SDK_INT] is less than 34).
 */
class AnimatedSubActivity : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_window_anim_sub`.
     *
     * We initialize our [RelativeLayout] variable `rootView`
     * to the view with ID `R.id.root_view_anim_sub` then call
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
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_anim_sub)
        val rootView = findViewById<RelativeLayout>(R.id.root_view_anim_sub)
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
    }

    /**
     * Called when your activity is done and should be closed. First we call our super's implementation
     * of `finish`, then we branch on the [Build.VERSION.SDK_INT] of the device we are running on:
     *
     *  - [Build.VERSION.SDK_INT] is 34 or greater: we call the [overrideActivityTransition] method
     *  to override the [Activity.OVERRIDE_TRANSITION_CLOSE] closing transition animation substituting
     *  the xml animation with resource ID `R.anim.slide_in_right` for the default enter animation,
     *  and the xml animation with resource ID `R.anim.slide_out_right` for the default enter
     *  animation.
     *
     *  - [Build.VERSION.SDK_INT] is less than 34: we call the [overridePendingTransition] method to
     *  specify the use of the xml animation with resource id `R.anim.slide_in_right` for the incoming
     *  activity, and `R.anim.slide_out_right` for the outgoing activity.
     */
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                /* overrideType = */ OVERRIDE_TRANSITION_CLOSE,
                /* enterAnim = */ R.anim.slide_in_right,
                /* exitAnim = */ R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION") // Needed for SDK older than 34
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        }

    }
}
