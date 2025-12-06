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
@file:Suppress("unused", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.curvedmotion

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This app shows how to move a view in a curved path between two endpoints. The real work is done
 * by [PathEvaluator], which interpolates along a path using Bezier control and anchor points in the
 * path.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the AndroidDevelopers channel on YouTube at
 * https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0.
 * https://www.youtube.com/watch?v=JVGg4zPRHNE
 */
class CurvedMotion : ComponentActivity() {
    /**
     * Flag indicating whether our [Button] field [mButton] is at the top left of the screen
     * (beginning position) or has been moved to the bottom of the screen.
     */
    var mTopLeft: Boolean = true

    /**
     * [Button] in our layout file with id `R.id.button` ("Click Me!") whose position on the
     * screen we animate when the button is clicked.
     */
    var mButton: Button? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_curved_motion`.
     *
     * We initialize our [RelativeLayout] variable `rootView`
     * to the view with ID `R.id.root_view` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [Button] field [mButton] by finding the view with id `R.id.button`
     * ("Click Me!"), and set its [OnClickListener] to an anonymous class whose
     * [OnClickListener.onClick] override causes the button to be moved from the top left of
     * the screen to the bottom right (or back again) using a curved animation.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_curved_motion)
        val rootView = findViewById<RelativeLayout>(R.id.root_view)
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
        mButton = findViewById(R.id.button)
        mButton!!.setOnClickListener {
            // Capture current location of button
            val oldLeft = mButton!!.left
            val oldTop = mButton!!.top

            // Change layout parameters of button to move it
            moveButton()

            // Add OnPreDrawListener to catch button after layout but before drawing
            mButton!!.viewTreeObserver.addOnPreDrawListener(
                object : OnPreDrawListener {
                    /**
                     * Callback method to be invoked when the view tree is about to be drawn. At this
                     * point, all views in the tree have been measured and given a frame. Clients can
                     * use this to adjust their scroll bounds or even to request a new layout before
                     * drawing occurs.
                     *
                     * First we remove `this` as an [OnPreDrawListener] of the [ViewTreeObserver] of
                     * [mButton]. Then we initialize [Int] variable `val left` with the left position
                     * of [mButton] relative to its parent, and [Int] variable `val top` with the top
                     * position (the new location). We calculate [Int] variable `val deltaX` by
                     * subtracting `oldLeft` from `left` and [Int] variable `val deltaY` by subtracting
                     * `oldTop` from `top`. We initialize [AnimatorPath] variable `val path` with a
                     * new instance, then call its [AnimatorPath.moveTo] method to add a [PathPoint]
                     * object that describes a discontinuous move to (-deltaX,-deltaY) to it, and call
                     * its [AnimatorPath.curveTo] method to add a [PathPoint] that describes a Bezier
                     * curve for the `CURVE` operation of the [PathEvaluator]. We then initialize
                     * [ObjectAnimator] variable `val anim` with an instance which will animate the
                     * "buttonLoc" property of `this` [CurvedMotion] using a new instance of [PathEvaluator]
                     * that will be called on each animation frame to provide the necessary interpolation
                     * between the Object values to derive the animated value, and the [PathPoint]
                     * objects in `path` as the values that the animation will animate between over
                     * time. We then start `anim` running and return `true` to proceed with the current
                     * drawing pass.
                     *
                     * @return Return `true` to proceed with the current drawing pass, or `false` to cancel.
                     */
                    override fun onPreDraw(): Boolean {
                        mButton!!.viewTreeObserver.removeOnPreDrawListener(this)

                        // Capture new location
                        val left: Int = mButton!!.left
                        val top: Int = mButton!!.top
                        val deltaX: Int = left - oldLeft
                        val deltaY: Int = top - oldTop

                        // Set up path to new location using a Bezier spline curve
                        val path = AnimatorPath()
                        path.moveTo(-deltaX.toFloat(), -deltaY.toFloat())
                        path.curveTo(-(deltaX / 2).toFloat(), -deltaY.toFloat(), 0f, (-deltaY / 2).toFloat(), 0f, 0f)

                        // Set up the animation
                        val anim: ObjectAnimator = ObjectAnimator.ofObject(
                            this@CurvedMotion, "buttonLoc",
                            PathEvaluator(), *path.points.toTypedArray()
                        )
                        anim.interpolator = sDecelerateInterpolator
                        anim.start()
                        return true
                    }
                })
        }
    }

    /**
     * Toggles button location on click between top-left and bottom-right. First we initialize
     * [RelativeLayout.LayoutParams] variable `val params` with the LayoutParams associated with
     * [Button] field [mButton]. Then we branch on the value of our [Boolean] field [mTopLeft]:
     *
     *  * `true`: (the button is currently in the original top left position on the screen) we
     *  remove the [RelativeLayout.ALIGN_PARENT_LEFT] layout rule and the
     *  [RelativeLayout.ALIGN_PARENT_TOP] layout rule from `params`, then add the
     *  [RelativeLayout.ALIGN_PARENT_RIGHT] layout rule and the [RelativeLayout.ALIGN_PARENT_BOTTOM]
     *  layout rule to it.
     *
     *  * `false`: (the button is currently in the bottom right position on the screen) we add the
     *  [RelativeLayout.ALIGN_PARENT_LEFT] layout rule and the [RelativeLayout.ALIGN_PARENT_TOP]
     *  layout rule to `params`, and remove the [RelativeLayout.ALIGN_PARENT_RIGHT] layout rule and
     *  the [RelativeLayout.ALIGN_PARENT_BOTTOM] layout rule from it.
     *
     * Finally we set the layout parameters associated with [mButton] to `params` and toggle
     * the value of [mTopLeft].
     */
    private fun moveButton() {
        val params = mButton!!.layoutParams as RelativeLayout.LayoutParams
        if (mTopLeft) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        }
        mButton!!.layoutParams = params
        mTopLeft = !mTopLeft
    }

    /**
     * We need this setter to translate between the information the animator produces (a new
     * [PathPoint] describing the current animated location) and the information that the button
     * requires (an xy location). The setter will be called by the [ObjectAnimator] given the
     * "buttonLoc" property string. We set the horizontal location of [Button] field [mButton]
     * relative to its left position to the [PathPoint.mX] field of our [PathPoint] parameter
     * [newLoc] and its vertical location relative to its top position to the [PathPoint.mY] field.
     *
     * @param newLoc [PathPoint] describing the current animated location.
     */
    fun setButtonLoc(newLoc: PathPoint) {
        mButton!!.translationX = newLoc.mX
        mButton!!.translationY = newLoc.mY
    }

    companion object {
        /**
         * [TimeInterpolator] we use for the [ObjectAnimator] of our "buttonLoc" property
         */
        private val sDecelerateInterpolator = DecelerateInterpolator()
    }
}
