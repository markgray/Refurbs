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
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This example shows various ways of animating multiple properties in parallel.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * [DevBytes](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0).
 * [MultiPropertyAnimations](https://www.youtube.com/watch?v=WvCZcy3WGP4)
 */
class MultiPropertyAnimations : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener`argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_property_animations)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * A very manual approach to animation that uses a [ValueAnimator] to animate a fractional value
     * and then turns that value into the final property values which are then set directly on the
     * target object. Specified as the [OnClickListener] by an android:onClick="runValueAnimator"
     * attribute for the first button in our layout. First we initialize [ValueAnimator] variable
     * `val anim` with an instance that will animate between 0f and 400f, then add an anonymous
     * [ValueAnimator.AnimatorUpdateListener] to it whose
     * [ValueAnimator.AnimatorUpdateListener.onAnimationUpdate] lambda override retrieves the current
     * animation fraction from its [ValueAnimator] parameter `animator` and uses that fraction to
     * interpolate how far between [TX_START] and [TX_END] to translate our [View] parameter [view]
     * relative to its left position, and how far between [TY_START] and [TY_END] to translate our
     * parameter [view] relative to its top position.
     *
     * Finally we start `anim` running.
     *
     * @param view the [View] that was clicked
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
            val fraction: Float = animator.animatedFraction
            view.translationX = TX_START + fraction * (TX_END - TX_START)
            view.translationY = TY_START + fraction * (TY_END - TY_START)
        }
        anim.start()
    }

    /**
     * [ViewPropertyAnimator] is the cleanest and most efficient way of animating [View] properties,
     * even when there are multiple properties to be animated in parallel. Specified as the
     * [OnClickListener] by a android:onClick="runViewPropertyAnimator" attribute for the second
     * button in our layout. We use the [View.animate] method of [View] parameter [view] to retrieve
     * a [ViewPropertyAnimator] object for [view] and have it animate the "translationX" property to
     * [TX_END], and the "translationY" property to [TY_END] (it starts automatically).
     *
     * @param view the [View] that was clicked
     */
    fun runViewPropertyAnimator(view: View) {
        view.animate().translationX(TX_END).translationY(TY_END)
    }

    /**
     * Multiple ObjectAnimator objects can be created and run in parallel. Specified as the
     * [OnClickListener] by a android:onClick="runObjectAnimators" attribute for the third button
     * in our layout. We create an [ObjectAnimator] that will animate the [View.TRANSLATION_X]
     * property of our [View] parameter [view] to [TX_END] and start it running, then create an
     * [ObjectAnimator] that will animate the [View.TRANSLATION_Y] property of our [View] parameter
     * [view] to [TY_END] and start it running.
     *
     * @param view the [View] that was clicked
     */
    fun runObjectAnimators(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, TX_END).start()
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, TY_END).start()
        // Optional: use an AnimatorSet to run these in parallel
    }

    /**
     * Using [PropertyValuesHolder] objects enables the use of a single [ObjectAnimator] per target,
     * even when there are multiple properties being animated on that target. Specified as the
     * [OnClickListener] by a android:onClick="runObjectAnimator" attribute for the fourth button
     * in our layout. We initialize [PropertyValuesHolder] variable `val pvhTX` with an instance
     * which will animate the [View.TRANSLATION_X] property to [TX_END], and [PropertyValuesHolder]
     * variable `val pvhTY` with an instance which will animate the [View.TRANSLATION_Y] property to
     * [TY_END]. Then we create an [ObjectAnimator] from them that will animate these properties
     * of our [View] parameter [view] and start it running.
     *
     * @param view the [View] that was clicked
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
