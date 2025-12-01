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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.viewanimations

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This example shows how to use pre-3.0 view Animation classes to create various animated UI
 * effects. See also the demo PropertyAnimations, which shows how this is done using the new
 * ObjectAnimator API introduced in Android 3.0.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=_UWXqFBF86U)
 */
class ViewAnimations : ComponentActivity() {
    /**
     * [CheckBox] with id `R.id.checkbox`, "Use Animation Resources" switches to xml [Animation]
     */
    var mCheckBox: CheckBox? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.activity_view_animations`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID
     * `R.id.root_view` then call [ViewCompat.setOnApplyWindowInsetsListener] to
     * take over the policy for applying window insets to `rootView`, with the
     * `listener` argument a lambda that accepts the [View] passed the lambda
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
     * Next we initialize our [CheckBox] field [mCheckBox] by finding the view with id
     * `R.id.checkbox` ("Use Animation Resources"), initialize [Button] variable `val alphaButton`
     * by finding the view with id `R.id.alphaButton` ("Alpha"), initialize [Button] variable
     * `val translateButton` ("Translate") by finding the view with id `R.id.translateButton`,
     * initialize [Button] variable `val rotateButton` ("Rotate") by finding the view with id
     * `R.id.rotateButton`, initialize [Button] variable `val scaleButton` ("Scale") by finding
     * the view with id `R.id.scaleButton`, and initialize [Button] variable `val setButton`
     * ("Set") by finding the view with id `R.id.setButton`. We initialize our [AlphaAnimation]
     * variable `val alphaAnimation` with an instance which will animate the alpha property from 1
     * to 0 and set its duration to 1000ms, [TranslateAnimation] variable `val translateAnimation`
     * with an instance which will animate from an X value of an absolute number of pixels of 0, to
     * an X value of 1 times the width of the parent view, and from a Y value of an absolute number
     * of pixels of 0, to a Y value of an absolute number of pixels of 100, and set its duration to
     * 1000ms. We initialize [RotateAnimation] variable `val rotateAnimation` with an instance which
     * will animate from 0 to 360 degrees, with a pivot X value relative to self of .5 and a pivot Y
     * value relative to self of .5, and set its duration to 1000ms. We initialize [ScaleAnimation]
     * variable `val scaleAnimation` with an instance which will animate the scaling of X from 1 to
     * 2 and the scaling of Y from 1 to 2 and set its duration to 1000ms. We initialize
     * [AnimationSet] variable `val setAnimation` with a new instance, and add `alphaAnimation`,
     * `translateAnimation`, `rotateAnimation`, and `scaleAnimation` to it. Then we call our method
     * [setupAnimation] to setup `alphaButton` with `alphaAnimation` and the resource animation
     * `R.anim.alpha_anim`, setup `translateButton` with `translateAnimation` and the resource
     * animation `R.anim.translate_anim`, setup `rotateButton` with `rotateAnimation` and the
     * resource animation `R.anim.rotate_anim`, setup `scaleButton` with `scaleAnimation` and the
     * resource animation `R.anim.scale_anim`, and setup `setButton` with `setAnimation` and the
     * resource animation `R.anim.set_anim`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_animations)
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
        mCheckBox = findViewById(R.id.checkbox)
        val alphaButton: Button = findViewById(R.id.alphaButton)
        val translateButton: Button = findViewById(R.id.translateButton)
        val rotateButton: Button = findViewById(R.id.rotateButton)
        val scaleButton: Button = findViewById(R.id.scaleButton)
        val setButton: Button = findViewById(R.id.setButton)

        // Fade the button out and back in
        val alphaAnimation = AlphaAnimation(1f, 0f)
        alphaAnimation.duration = 1000

        // Move the button over and then back
        val translateAnimation = TranslateAnimation(
            /* fromXType = */ Animation.ABSOLUTE,
            /* fromXValue = */ 0f,
            /* toXType = */ Animation.RELATIVE_TO_PARENT,
            /* toXValue = */ 1f,
            /* fromYType = */ Animation.ABSOLUTE,
            /* fromYValue = */ 0f,
            /* toYType = */ Animation.ABSOLUTE,
            /* toYValue = */ 100f
        )
        translateAnimation.duration = 1000

        // Spin the button around in a full circle
        val rotateAnimation = RotateAnimation(
            /* fromDegrees = */ 0f,
            /* toDegrees = */ 360f,
            /* pivotXType = */ Animation.RELATIVE_TO_SELF,
            /* pivotXValue = */ .5f,
            /* pivotYType = */ Animation.RELATIVE_TO_SELF,
            /* pivotYValue = */ .5f
        )
        rotateAnimation.duration = 1000

        // Scale the button in X and Y.
        val scaleAnimation = ScaleAnimation(
            /* fromX = */ 1f,
            /* toX = */ 2f,
            /* fromY = */ 1f,
            /* toY = */ 2f
        )
        scaleAnimation.duration = 1000

        // Run the animations above in sequence on the final button. Looks horrible.
        val setAnimation = AnimationSet(/* shareInterpolator = */ true)
        setAnimation.addAnimation(alphaAnimation)
        setAnimation.addAnimation(translateAnimation)
        setAnimation.addAnimation(rotateAnimation)
        setAnimation.addAnimation(scaleAnimation)
        setupAnimation(alphaButton, alphaAnimation, R.anim.alpha_anim)
        setupAnimation(translateButton, translateAnimation, R.anim.translate_anim)
        setupAnimation(rotateButton, rotateAnimation, R.anim.rotate_anim)
        setupAnimation(scaleButton, scaleAnimation, R.anim.scale_anim)
        setupAnimation(setButton, setAnimation, R.anim.set_anim)
    }

    /**
     * Adds an [View.OnClickListener] which animates the [View] parameter [view] using a
     * programmatically created [Animation] parameter [animation] if our [CheckBox] field
     * [mCheckBox] is unchecked, or using an xml animation with resource id [Int] parameter
     * [animationID] if it is checked.
     *
     * @param view the [View] which we want to add an animating [View.OnClickListener] to
     * @param animation programmatically created [Animation]
     * @param animationID resource id of an xml animation file
     */
    private fun setupAnimation(view: View, animation: Animation, animationID: Int) {
        view.setOnClickListener { v ->
            /**
             * Called when our view has been clicked. If `CheckBox mCheckBox` is checked we load
             * the xml animation with resource id `int animationID` and start it running, and if
             * the checkbox is unchecked we start `Animation animation` running.
             *
             * @param v `View` that was clicked
             */
            // If the button is checked, load the animation from the given resource
            // id instead of using the passed-in animation parameter. See the xml files
            // for the details on those animations.
            v.startAnimation(if (mCheckBox!!.isChecked) AnimationUtils.loadAnimation(this@ViewAnimations, animationID) else animation)
        }
    }
}
