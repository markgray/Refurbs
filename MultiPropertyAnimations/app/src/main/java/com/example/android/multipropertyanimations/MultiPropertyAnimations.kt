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
package com.example.android.multipropertyanimations

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.view.View

/**
 * This example shows various ways of animating multiple properties in parallel.
 *
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * [DevBytes](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0).
 * [MultiPropertyAnimations](https://www.youtube.com/watch?v=WvCZcy3WGP4)
 */
class MultiPropertyAnimations : Activity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.activity_multi_property_animations
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_property_animations)
    }

    /**
     * A very manual approach to animation uses a ValueAnimator to animate a fractional value and
     * then turns that value into the final property values which are then set directly on the target
     * object. Specified as the `onClickListener` by an android:onClick="runValueAnimator" attribute
     * for the first button in our layout. First we initialize `ValueAnimator anim` with an instance
     * that will animate between 0 and 400, then add an anonymous `AnimatorUpdateListener` to it
     * whose `onAnimationUpdate` override retrieves the current animation fraction from its parameter
     * `ValueAnimator animator` and uses that fraction to interpolate how far between TX_START and
     * TX_END to translate our parameter `View view` relative to its left position, and how far between
     * TY_START and TY_END to translate our parameter `View view` relative to its top position.
     *
     *
     * Finally we start `anim` running.
     *
     * @param view `View` that was clicked
     */
    fun runValueAnimator(view: View) {
        val anim = ValueAnimator.ofFloat(0f, 400f)
        anim.addUpdateListener { animator: ValueAnimator ->
            /**
             * Notifies the occurrence of another frame of the animation. First we retrieve the current
             * animation fraction from our parameter `ValueAnimator animator` then we use it to
             * interpolate how far between TX_START and TX_END to translate our `view` relative
             * to its left position, and how far between TY_START and TY_END to translate it relative
             * to its top position.
             *
             * Parameter: animator The animation which was repeated.
             */
            val fraction = animator.animatedFraction
            view.translationX = TX_START + fraction * (TX_END - TX_START)
            view.translationY = TY_START + fraction * (TY_END - TY_START)
        }
        anim.start()
    }

    /**
     * ViewPropertyAnimator is the cleanest and most efficient way of animating View properties, even
     * when there are multiple properties to be animated in parallel. Specified as the `onClickListener`
     * by a android:onClick="runViewPropertyAnimator" attribute for the second button in our layout.
     * We retrieve a `ViewPropertyAnimator` for our parameter `View view` and have it animate
     * the "translationX" property to TX_END, and the "translationY" property to TY_END (it starts automatically).
     *
     * @param view `View` that was clicked
     */
    fun runViewPropertyAnimator(view: View) {
        view.animate().translationX(TX_END).translationY(TY_END)
    }

    /**
     * Multiple ObjectAnimator objects can be created and run in parallel. Specified as the `onClickListener`
     * by a android:onClick="runObjectAnimators" attribute for the third button in our layout. We create
     * an `ObjectAnimator` that will animate the TRANSLATION_X property of our parameter `View view`
     * to TX_END and start it running, then create an `ObjectAnimator` that will animate the TRANSLATION_Y
     * property of our parameter `View view` to TY_END and start it running.
     *
     * @param view `View` that was clicked
     */
    fun runObjectAnimators(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, TX_END).start()
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, TY_END).start()
        // Optional: use an AnimatorSet to run these in parallel
    }

    /**
     * Using PropertyValuesHolder objects enables the use of a single ObjectAnimator per target, even
     * when there are multiple properties being animated on that target. Specified as the `onClickListener`
     * by a android:onClick="runObjectAnimator" attribute for the fourth button in our layout. We initialize
     * `PropertyValuesHolder pvhTX` with an instance which will animate the TRANSLATION_X property
     * to TX_END, and `PropertyValuesHolder pvhTY` with an instance which will animate the TRANSLATION_Y
     * property to TY_END. Then we create an `ObjectAnimator` from them that will animate these properties
     * of our parameter `View view` and start it running.
     *
     * @param view `View` that was clicked
     */
    fun runObjectAnimator(view: View?) {
        val pvhTX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, TX_END)
        val pvhTY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, TY_END)
        ObjectAnimator.ofPropertyValuesHolder(view, pvhTX, pvhTY).start()
    }

    companion object {
        /**
         * Start X translation of our views
         */
        private const val TX_START = 0f

        /**
         * Start Y translation of our views
         */
        private const val TY_START = 0f

        /**
         * End X translation of our views
         */
        private const val TX_END = 400f

        /**
         * End Y translation of our views
         */
        private const val TY_END = 200f
    }
}