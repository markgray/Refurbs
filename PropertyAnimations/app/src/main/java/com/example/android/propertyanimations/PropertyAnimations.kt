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

package com.example.android.propertyanimations

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
 * This example shows how to use property animations, specifically [ObjectAnimator], to perform
 * various view animations. Compare this approach to that of the ViewAnimations demo, which
 * shows how to achieve similar effects using the pre-3.0 animation APIs.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=3UbJhmkeSig)
 */
class PropertyAnimations : ComponentActivity() {
    /**
     * [CheckBox] in our layout with id `R.id.checkbox` ("Use Animation Resources") selects the
     * use of objectAnimator xml resource files instead of the java constructed animations
     */
    var mCheckBox: CheckBox? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.activity_property_animations`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID
     * `R.id.container` then call [ViewCompat.setOnApplyWindowInsetsListener] to
     * take over the policy for applying window insets to `rootView`, with the
     * `listener` argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `insets` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [CheckBox] field [mCheckBox] by finding the view with id `R.id.checkbox`
     * ("Use Animation Resources"), then initialize [Button] variable `val alphaButton` by finding
     * the view with id `R.id.alphaButton` ("Alpha"), [Button] variable `val translateButton` by
     * finding the view with id `R.id.translateButton` ("Translate"), [Button] variable
     * `val rotateButton` by finding the view with id `R.id.rotateButton` ("Rotate"), [Button]
     * variable `val scaleButton` by finding the view with id `R.id.scaleButton` ("Scale"), and
     * [Button] variable `val setButton` by finding the view with id `R.id.setButton` ("Set").
     * We initialize [ObjectAnimator] variable `val alphaAnimation` with an instance which will
     * animate the [View.ALPHA] property of `alphaButton` to 0, set its repeat count to 1, and set
     * its repeat mode to [ValueAnimator.REVERSE]. We initialize [ObjectAnimator] variable
     * `val translateAnimation` with an instance which will animate the [View.TRANSLATION_X]
     * property of `translateButton` to 800, set its repeat count to 1, and set its repeat mode
     * to [ValueAnimator.REVERSE]. We initialize [ObjectAnimator] variable `val rotateAnimation`
     * with an instance which will animate the [View.ROTATION] property of `rotateButton` to 360,
     * set its repeat count to 1, and set its repeat mode to [ValueAnimator.REVERSE]. We initialize
     * [PropertyValuesHolder] variable `val pvhX` with an instance which will animate the
     * [View.SCALE_X] property to 2, initialize [PropertyValuesHolder] variable `val pvhY` with an
     * instance which will animate the [View.SCALE_Y] property to 2, then initialize [ObjectAnimator]
     * `val scaleAnimation` with an instance which will apply the animations `pvhX` and `pvhY` to
     * `scaleButton`, set its repeat count to 1, and set its repeat mode to [ValueAnimator.REVERSE].
     * We initialize [AnimatorSet] variable `val setAnimation` with a new instance, create a
     * [AnimatorSet.Builder] for it that will use `translateAnimation` as a dependency to run after
     * `alphaAnimation` and before `rotateAnimation`, then create another [AnimatorSet.Builder]
     * for it that will use `rotateAnimation` as a dependency to have it run before `scaleAnimation`.
     *
     * Having set up the animations to be used we call our method [setupAnimation] to have it add an
     * [View.OnClickListener] to `alphaButton` whose [View.OnClickListener.onClick] override runs
     * `alphaAnimation` if [mCheckBox] is unchecked or runs the xml animation with resource id
     * `R.animator.fade` if it is checked, call our method [setupAnimation] to have it add a
     * [View.OnClickListener] to `translateButton` whose [View.OnClickListener.onClick] override
     * runs `translateAnimation` on it if [mCheckBox] is unchecked or runs the xml animation with
     * resource id `R.animator.move` if it is checked, call our method [setupAnimation] to have it
     * add an [View.OnClickListener] to `rotateButton` whose [View.OnClickListener.onClick] override
     * runs `rotateAnimation` if [mCheckBox] is unchecked or runs the xml animation with resource id
     * `R.animator.spin` if it is checked, call our method [setupAnimation] to have it add an
     * [View.OnClickListener] to `scaleButton` whose [View.OnClickListener.onClick] override runs
     * `scaleAnimation` if [mCheckBox] is unchecked or runs the xml animation with resource id
     * `R.animator.scale` if it is checked, and finally call our method [setupAnimation] to have it
     * add an [View.OnClickListener] to `setButton` whose [View.OnClickListener.onClick] override
     * will run `setAnimation` to animate the other buttons if [mCheckBox] is unchecked or runs the
     * xml animation with resource id `R.animator.combo` on itself if it is checked.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_animations)
        val rootView = findViewById<LinearLayout>(R.id.container)
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
        val alphaAnimation = ObjectAnimator.ofFloat(alphaButton, View.ALPHA, 0f)
        alphaAnimation.repeatCount = 1
        alphaAnimation.repeatMode = ValueAnimator.REVERSE

        // Move the button over to the right and then back
        val translateAnimation = ObjectAnimator.ofFloat(translateButton, View.TRANSLATION_X, 800f)
        translateAnimation.repeatCount = 1
        translateAnimation.repeatMode = ValueAnimator.REVERSE

        // Spin the button around in a full circle
        val rotateAnimation = ObjectAnimator.ofFloat(rotateButton, View.ROTATION, 360f)
        rotateAnimation.repeatCount = 1
        rotateAnimation.repeatMode = ValueAnimator.REVERSE

        // Scale the button in X and Y. Note the use of PropertyValuesHolder to animate
        // multiple properties on the same object in parallel.
        val pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 2f)
        val pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 2f)
        val scaleAnimation = ObjectAnimator.ofPropertyValuesHolder(scaleButton, pvhX, pvhY)
        scaleAnimation.repeatCount = 1
        scaleAnimation.repeatMode = ValueAnimator.REVERSE

        // Run the animations above in sequence
        val setAnimation = AnimatorSet()
        setAnimation.play(translateAnimation).after(alphaAnimation).before(rotateAnimation)
        setAnimation.play(rotateAnimation).before(scaleAnimation)
        setupAnimation(alphaButton, alphaAnimation, R.animator.fade)
        setupAnimation(translateButton, translateAnimation, R.animator.move)
        setupAnimation(rotateButton, rotateAnimation, R.animator.spin)
        setupAnimation(scaleButton, scaleAnimation, R.animator.scale)
        setupAnimation(setButton, setAnimation, R.animator.combo)
    }

    /**
     * Sets the [View.OnClickListener] of our [View] parameter [view] to an anonymous instance which
     * runs the animation in [Animator] parameter [animation] if [CheckBox] field [mCheckBox] is
     * unchecked, or runs the xml [Animator] whose resource id is [Int] parameter [animationID] on
     * the [View] paraemter [view] if [CheckBox] field [mCheckBox] is checked.
     *
     * @param view the [View] we are to add an [View.OnClickListener] to.
     * @param animation the [Animator] to run if [CheckBox] field [mCheckBox] is unchecked
     * @param animationID Resource id of an xml [Animator] to run if [CheckBox] field [mCheckBox]
     * is checked
     */
    private fun setupAnimation(view: View, animation: Animator, animationID: Int) {

        view.setOnClickListener(View.OnClickListener { v: View ->
            /**
             * Called when a view has been clicked. If `CheckBox mCheckBox` is checked we initialize
             * `Animator anim` by inflating the xml `Animator` with the resource id `int animationID`,
             * set its target to `v`, start it running and return. If `CheckBox mCheckBox` is unchecked
             * we just start `Animator animation` running.
             *
             * @param v The view that was clicked.
             */
            // If the button is checked, load the animation from the given resource
            // id instead of using the passed-in animation parameter. See the xml files
            // for the details on those animations.
            if (mCheckBox!!.isChecked) {
                val anim = AnimatorInflater.loadAnimator(this@PropertyAnimations, animationID)
                anim.setTarget(v)
                anim.start()
                return@OnClickListener
            }
            animation.start()
        })
    }
}
