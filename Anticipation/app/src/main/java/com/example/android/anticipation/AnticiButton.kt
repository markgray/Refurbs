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
@file:Suppress(
    "ReplaceNotNullAssertionWithElvisReturn",
    "UNUSED_ANONYMOUS_PARAMETER",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.anticipation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatButton

/**
 * Custom button which can be deformed by skewing the top left and right, to simulate
 * anticipation and follow-through animation effects. Clicking on the button runs
 * an animation which moves the button left or right, applying the skew effect to the
 * button. The logic of drawing the button with a skew transform is handled in the
 * draw() override.
 */
class AnticiButton : AppCompatButton {
    /**
     * How much we are currently skewed by.
     */
    private var mSkewX = 0f

    /**
     * [ObjectAnimator] for the animation of the "skewX" property when the button is pressed.
     */
    var downAnim: ObjectAnimator? = null

    /**
     * Flag to indicate that we are on the left side of the screen.
     */
    var mOnLeft: Boolean = true

    /**
     * [RectF] which surrounds our view relative to our parent, used to invalidate the proper
     * area of our parent for skewed bounds in our [invalidateSkewedBounds] method
     */
    var mTempRect: RectF = RectF()

    /**
     * Our one argument constructor. We call our super's constructor, then call our `init`
     * method to set our [View.OnTouchListener] and our [View.OnClickListener]. UNUSED
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context!!) {
        init()
    }

    /**
     * This constructor allows a [AppCompatButton] subclass to use its own class-specific base
     * style from a theme attribute when inflating. The attributes defined by the current theme's
     * `defStyleAttr` override base view attributes. We call our super's constructor, then call
     * our `init` method to set our [View.OnTouchListener] and our [View.OnClickListener]. UNUSED
     *
     * @param context The [Context] the Button is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML [AnticiButton] tag that is inflating the view.
     * @param defStyle The resource identifier of an attribute in the current theme
     * whose value is the the resource id of a style. The specified style’s
     * attribute values serve as default values for the button. Set this parameter
     * to 0 to avoid use of default values.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    ) {
        init()
    }

    /**
     * [LayoutInflater] calls this constructor when inflating a [AnticiButton] from XML. We call
     * our super's constructor, then call our `init` method to set our [View.OnTouchListener] and
     * our [View.OnClickListener]. This is the constructor that is used.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML [AnticiButton] tag being used to inflate the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    /**
     * Called by our constructors to set our [View.OnTouchListener] and [View.OnClickListener].
     * We set our `OnTouchListener` to our [OnTouchListener] field [mTouchListener] and set
     * our [View.OnClickListener] to an anonymous class whose `onClick` override calls our
     * method [runClickAnim] to create and perform the animations we do when we are clicked.
     */
    private fun init() {
        setOnTouchListener(mTouchListener)
        setOnClickListener { runClickAnim() }
    }

    /**
     * Manually render this view (and all of its children) to the given [Canvas]. The view must have
     * already done a full layout before this function is called.
     *
     * The skew effect is handled by changing the transform of the Canvas and then calling the usual
     * superclass draw() method. If our field [mSkewX] is not 0 we translate the canvas to the
     * bottom left corner of our view, pre-concat the current matrix with a skew in X of [mSkewX],
     * then translate the canvas back to its original position. Whether we skewed the canvas or not
     * we call our super's implementation of `draw`.
     *
     * @param canvas The [Canvas] to which the View is rendered.
     */
    override fun draw(canvas: Canvas) {
        if (mSkewX != 0f) {
            canvas.translate(/* dx = */ 0f, /* dy = */ height.toFloat())
            canvas.skew(/* sx = */ mSkewX, /* sy = */ 0f)
            canvas.translate(/* dx = */ 0f, /* dy = */ -height.toFloat())
        }
        super.draw(canvas)
    }

    /**
     * Anticipate the future animation by rearing back, away from the direction of travel, this is
     * run when we receive an ACTION_DOWN event in our [View.OnTouchListener]. We initialize our
     * [ObjectAnimator] field [downAnim] with an instance which will animate the "skewX" property
     * of 'this' to .5f if our field [mOnLeft] is true or to -.5f if it is false, set its duration
     * to 2500, set its `TimeInterpolator` to our [DecelerateInterpolator] field [sDecelerator],
     * and start it running.
     */
    private fun runPressAnim() {
        Log.i(TAG, "Right: $right Bottom: $bottom")
        downAnim = ObjectAnimator.ofFloat(
            /* target = */ this,
            /* propertyName = */ "skewX",
            /* ...values = */ if (mOnLeft) .5f else -.5f
        )
        downAnim!!.duration = 2500
        downAnim!!.interpolator = sDecelerator
        downAnim!!.start()
    }

    /**
     * Finish the "anticipation" animation (skew the button back from the direction of travel),
     * animate it to the other side of the screen, then un-skew the button with an Overshoot effect.
     * We initialize [ObjectAnimator] variable `var finishDownAnim` to null. If our [ObjectAnimator]
     * field [downAnim] is not null, and is currently running, we call its `cancel` method to cancel
     * it, set `finishDownAnim` to an instance which animates the "skewX" property of 'this' to .5f
     * if [mOnLeft] is true (our button is on the left of the screen) or to -.5f if it is on the
     * right side, set its duration to 150, and set its `TimeInterpolator` to our
     * [DecelerateInterpolator] field [sQuickDecelerator].
     *
     * Having taken care of any [downAnim] that may have been running (or not running) we initialize
     * [ObjectAnimator] variable `val moveAnim` with an instance which will animate the TRANSLATION_X
     * property of 'this' to 400 if [mOnLeft] is `true`, or to 0 if it is `false`, set its
     * `TimeInterpolator` to our [LinearInterpolator] field [sLinearInterpolator], and set its
     * duration to 150. We initialize [ObjectAnimator] variable `val skewAnim` with an instance which
     * will animate the "skewX" property of 'this' to -.5f if [mOnLeft] is `true`, or to .5f if it
     * is `false`, set its `TimeInterpolator` to [DecelerateInterpolator] field [sQuickDecelerator],
     * and set its duration to 100. We initialize [ObjectAnimator] variable `val wobbleAnim` to an
     * instance which will animate the "skewX" property of 'this' to 0, set its `TimeInterpolator`
     * to our [OvershootInterpolator] field [sOvershooter], and set its duration to 150.
     *
     * We next initialize [AnimatorSet] variable `val set` with a new instance, and set it up to
     * play sequentially `moveAnim`, `skewAnim`, and `wobbleAnim`. If `finishDownAnim` is not null
     * we call the `finishDownAnim` to create a `Builder` which we use to introduce the
     * constraint to `set` that `finishDownAnim` should be played before `moveAnim`.
     * In either case we start `set` running and toggle the value of our field [mOnLeft].
     */
    private fun runClickAnim() {
        // Anticipation
        var finishDownAnim: ObjectAnimator? = null
        if (downAnim != null && downAnim!!.isRunning) {
            // finish the skew animation quickly
            downAnim!!.cancel()
            finishDownAnim = ObjectAnimator.ofFloat(
                /* target = */ this,
                /* propertyName = */ "skewX",
                /* ...values = */ if (mOnLeft) .5f else -.5f
            )
            finishDownAnim.duration = 150
            finishDownAnim.interpolator = sQuickDecelerator
        }

        // Slide. Use LinearInterpolator in this rare situation where we want to start
        // and end fast (no acceleration or deceleration, since we're doing that part
        // during the anticipation and overshoot phases).
        val moveAnim = ObjectAnimator.ofFloat(
            /* target = */ this,
            /* property = */ TRANSLATION_X,
            /* ...values = */ (if (mOnLeft) 400 else 0).toFloat()
        )
        moveAnim.interpolator = sLinearInterpolator
        moveAnim.duration = 150

        // Then overshoot by stopping the movement but skewing the button as if it couldn't
        // all stop at once
        val skewAnim = ObjectAnimator.ofFloat(
            /* target = */ this,
            /* propertyName = */ "skewX",
            /* ...values = */ if (mOnLeft) -.5f else .5f
        )
        skewAnim.interpolator = sQuickDecelerator
        skewAnim.duration = 100
        // and wobble it
        val wobbleAnim = ObjectAnimator.ofFloat(
            /* target = */ this,
            /* propertyName = */ "skewX",
            /* ...values = */ 0f
        )
        wobbleAnim.interpolator = sOvershooter
        wobbleAnim.duration = 150
        val set = AnimatorSet()
        set.playSequentially(/* ...items = */ moveAnim, skewAnim, wobbleAnim)
        if (finishDownAnim != null) {
            set.play(finishDownAnim).before(moveAnim)
        }
        set.start()
        mOnLeft = !mOnLeft
    }

    /**
     * Restore the button to its un-pressed state, called when our `onTouch` override receives
     * an ACTION_CANCEL event (or an ACTION_UP event without the button being pressed). If our
     * [ObjectAnimator] field [downAnim] is not `null` and it is running we call its `cancel`
     * method to cancel the animation. We then initialize [ObjectAnimator] variable `val reverser`
     * with an instance which will animate the "skewX" property of 'this' to 0, set its duration
     * to 200, set its `TimeInterpolator` to [AccelerateInterpolator] field [sAccelerator], and
     * start it running. We then set our field [downAnim] to `null`.
     */
    private fun runCancelAnim() {
        if (downAnim != null && downAnim!!.isRunning) {
            downAnim!!.cancel()
            val reverser = ObjectAnimator.ofFloat(
                /* target = */ this,
                /* propertyName = */ "skewX",
                /* ...values = */ 0f
            )
            reverser.duration = 200
            reverser.interpolator = sAccelerator
            reverser.start()
            downAnim = null
        }
    }

    /**
     * Handle touch events directly since we want to react on down/up events, not just
     * button clicks
     */
    private val mTouchListener = OnTouchListener { _: View, event: MotionEvent ->

        /**
         * Called when a touch event is dispatched to `this` `View`. We switch on the action
         * of our [MotionEvent] parameter [event]:
         *  * ACTION_UP: If the [isPressed] method returns `true` (indicating that the view
         *  is currently in the pressed state) we call the [performClick] method to call
         *  our [View.OnClickListener], call the [setPressed] method with `false` to clear the
         *  pressed state of our view, then break. If the `isPressed` method returned false we
         *  call our [runCancelAnim] method to run the cancel animation.
         *
         *  * ACTION_CANCEL: We call our [runCancelAnim] method to run the cancel animation.
         *
         *  * ACTION_MOVE: We initialize [Float] variable `val x` with the X coordinate of [event]
         *  and [Float] variable `val y` with the Y coordinate of [event], then initialize
         *  [Boolean] variable `val isInside` to `true` if the point (x,y) is within the width and
         *  height of our view, or to `false` if it is outside our view. Then if the [isPressed]
         *  method returns a value that is different than `isInside` we call the [setPressed]
         *  method with `isInside` to set our pressed state to the value of `isInside`.
         *
         *  * ACTION_DOWN: We call the [setPressed] method with `true` to set our pressed state,
         *  and call our [runPressAnim] method to run the "rearing back, away from the direction
         *  of travel animation".
         *
         *  * default: We ignore.
         *
         * We then return `true` to consume the event.
         *
         * @param _ The [View] the touch event has been dispatched to.
         * @param event The [MotionEvent] object containing full information about the event.
         * @return `true` if the listener has consumed the event, `false` otherwise.
         */
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (isPressed) {
                    performClick()
                    isPressed = false
                } else {
                    // Run the cancel animation in either case
                    runCancelAnim()
                }
            }

            MotionEvent.ACTION_CANCEL -> runCancelAnim()

            MotionEvent.ACTION_MOVE -> {
                val x: Float = event.x
                val y: Float = event.y
                val isInside: Boolean = x > 0 && x < width && y > 0 && y < height
                if (isPressed != isInside) {
                    isPressed = isInside
                }
            }

            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                runPressAnim()
            }

            else -> {}
        }
        true
    }

    /**
     * The amount of left/right skew on the button, which determines how far the button leans.
     */
    @Suppress("unused") // It is used as the property that animates the value of `mSkewX`
    var skewX: Float
        /**
         * Getter for our [Float] property [mSkewX]. We just return the current value of our
         * [Float] property [mSkewX].
         *
         * @return the current value of our [Float] property [mSkewX].
         */
        get() = mSkewX
        /**
         * Sets the amount of left/right skew on the button, which determines how far the button
         * leans. If our [Float] parameter `value` is not equal to our [Float] property [mSkewX]
         * we set [mSkewX] to it, call the [invalidate] method to force our button to be redrawn
         * with new skew value, and then call our [invalidateSkewedBounds] to also invalidate the
         * appropriate area of our parent. If our parameter ``value` is already equal to [mSkewX]
         * we do nothing.
         *
         * @param `value` value to set our [Float] property [mSkewX] to.
         */
        set(value) {
            if (value != mSkewX) {
                mSkewX = value
                invalidate() // force button to redraw with new skew value
                invalidateSkewedBounds() // also invalidate appropriate area of parent
            }
        }

    /**
     * Need to invalidate proper area of parent for skewed bounds. If our [Float] property [mSkewX]
     * if not 0f, we initialize [Matrix] variable `val matrix` with a new instance, and set it to
     * skew by minus [mSkewX] in the X dimension, 0 in the Y dimension. We set our [RectF] field
     * [mTempRect] to be a rectangle whose left top corner is at (0,0), and whose right bottom
     * corner is at our view's right and bottom coordinates relative to our parent (this is a
     * rectangle surrounding our un-skewed shape). We then call the [Matrix.mapRect] method of
     * `matrix` to apply its skewing transformation to [mTempRect]. We then offset [mTempRect] by
     * the `left` side of our view plus its X translation in the X direction, and by `the` top side
     * of our view plus its Y translation in the Y direction (at this point [mTempRect] is the area
     * of our parent that we occupy). We used to then retrieve our parent view, and call its
     * [invalidate] method to invalidate the region from the left top of [mTempRect] to the right
     * bottom (rounded up). BUT Note that it is a waste of effort doing this with hardware
     * rendering, and that it is better to just invalidate our entire parent as we do now (silences
     * a warning).
     */
    private fun invalidateSkewedBounds() {
        if (mSkewX != 0f) {
            val matrix = Matrix()
            matrix.setSkew(/* kx = */ -mSkewX, /* ky = */ 0f)
            mTempRect = RectF(
                /* left = */ 0f,
                /* top = */ 0f,
                /* right = */ 0f,
                /* bottom = */ right.toFloat()
            )
            matrix.mapRect(mTempRect)
            mTempRect.offset(
                /* dx = */ left + translationX,
                /* dy = */ top + translationY
            )
            (parent as View).invalidate()
        }
    }

    companion object {
        /**
         * TAG for logging
         */
        const val TAG: String = "AnticiButton"

        /**
         * `TimeInterpolator` for the animation of the TRANSLATION_X property.
         */
        private val sLinearInterpolator = LinearInterpolator()

        /**
         * `TimeInterpolator` for the animation of the "skewX" property when the button is pressed
         * (this animation "anticipates" the future animation by rearing back).
         */
        private val sDecelerator = DecelerateInterpolator(8f)

        /**
         * `TimeInterpolator` for the animation of the "skewX" property when the button press is
         * canceled by the user moving off the button.
         */
        private val sAccelerator = AccelerateInterpolator()

        /**
         * `TimeInterpolator` for the animation of the "skewX" property when the button "wobbles"
         * at the end of its travel across the screen.
         */
        private val sOvershooter = OvershootInterpolator()

        /**
         * `TimeInterpolator` for the animation of the "skewX" property when the button is released
         * before the "skewX" animation of the button press has completed, as well the animation of
         * the "skewX" overshoot at the end of the buttons travel.
         */
        private val sQuickDecelerator = DecelerateInterpolator()
    }
}