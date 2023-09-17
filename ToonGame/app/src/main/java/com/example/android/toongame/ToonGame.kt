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
@file:Suppress("SameParameterValue", "unused", "ReplaceNotNullAssertionWithElvisReturn", "UNUSED_ANONYMOUS_PARAMETER")

package com.example.android.toongame

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button

/**
 * This application shows various cartoon animation techniques in the context of
 * a larger application, to show how such animations might be used to create a more
 * interactive, fun, and engaging experience.
 *
 * This main activity launches a sub-activity when the Play button is clicked. The
 * main action in this master activity is bouncing the Play button in, randomly
 * bouncing it while waiting for input, and animating its press and click behaviors
 * when the user interacts with it.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * https://www.youtube.com/watch?v=8sG3bAPOhyw
 */
class ToonGame : Activity() {
    /**
     * `Button` in our layout with id R.id.startButton ("Play!"), launches `PlayerSetupActivity`
     * when clicked (activity is launched by a `Runnable` when the animation of the button release
     * finishes).
     */
    var mStarter: Button? = null

    /**
     * The `RelativeLayout` in our layout file with id R.id.container, we use it to animate our
     * transition to `PlayerSetupActivity`
     */
    var mContainer: ViewGroup? = null

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we call the `overridePendingTransition` to cancel any pending incoming or outgoing
     * animation (by using 0 as the resource id for both `enterAnim` and `exitAnim`). Then
     * we set our content view to our layout file R.layout.activity_toon_game. We initialize our field
     * `Button mStarter` by finding the view with id R.id.startButton ("Play!"), initialize our
     * field `ViewGroup mContainer` by finding `RelativeLayout` in our layout file with
     * id R.id.container, set the `OnTouchListener` of `mStarter` to `funButtonListener`
     * and set the duration of its `ViewPropertyAnimator` to 100ms.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION") // Needed for SDK older than 34
            overridePendingTransition(0, 0)
        }
        setContentView(R.layout.activity_toon_game)
        mStarter = findViewById(R.id.startButton)
        mContainer = findViewById(R.id.container)
        mStarter!!.setOnTouchListener(funButtonListener)
        mStarter!!.animate().duration = 100
    }

    /**
     * Called after [.onRestoreInstanceState], [.onRestart], or [.onPause], for our
     * activity to start interacting with the user. First we call our super's implementation of
     * `onResume`. We set the X scaling of `ViewGroup mContainer` around the pivot point
     * to 1, the Y scaling to 1, and its alpha to 1. We set the visibility of `Button mStarter`
     * to INVISIBLE. We fetch the `ViewTreeObserver` of `mContainer` (the view tree observer
     * can be used to get notifications when global events, like layout, happen) and add as an
     * `OnPreDrawListener` our field `ViewTreeObserver.OnPreDrawListener mOnPreDrawListener`.
     */
    override fun onResume() {
        super.onResume()
        mContainer!!.scaleX = 1f
        mContainer!!.scaleY = 1f
        mContainer!!.alpha = 1f
        mStarter!!.visibility = View.INVISIBLE
        mContainer!!.viewTreeObserver.addOnPreDrawListener(mOnPreDrawListener)
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but
     * has not (yet) been killed. First we call our super's implementation of `onPause`, then
     * we call the `removeCallbacks` method of our field `Button mStarter` to remove
     * `Runnable mSquishRunnable` from the message queue.
     */
    override fun onPause() {
        super.onPause()
        mStarter!!.removeCallbacks(mSquishRunnable)
    }

    /**
     * `OnTouchListener` for our field `Button mStarter` ("Play!"), performs animation
     * of the button when the event is ACTION_DOWN, resets the pressed state of the button when
     * an ACTION_MOVE event moves outside of the button, and performs animation if an ACTION_UP
     * event occurs while the button is in the pressed state.
     */
    private val funButtonListener = OnTouchListener { v, event ->

        /**
         * Called when a touch event is dispatched to a view. We switch on the action of our parameter
         * `MotionEvent event`:
         *
         *  *
         * ACTION_DOWN: we fetch a `ViewPropertyAnimator` for `Button mStarter`
         * and have it animate the scale of X to .8f, and animate the scale of Y to .8f using
         * our field `DecelerateInterpolator sDecelerator` as its `TimeInterpolator`.
         * We then set the text color of `mStarter` to CYAN, remove from its message
         * queue `Runnable mSquishRunnable`, and set its pressed state to true. We then
         * break.
         *
         *  *
         * ACTION_MOVE: we initialize `float x` with the X coordinate for the first
         * pointer index of our parameter `MotionEvent event` and `float y` with
         * the Y coordinate. We initialize `boolean isInside` to true if `x` is
         * greater than 0 and less than the width of `mStarter` and `y` is greater
         * than 0 and less than the height of `mStarter` (the move event is still inside
         * our button) and to false otherwise (the move event is outside of our button). If
         * the pressed state of `mStarter` is not equal to the value of `isInside`
         * we set its pressed state to `isInside`. We then break. (Note that the user
         * has to touch the bouncing button while it is still in order for the touch to be
         * noticed).
         *
         *  *
         * ACTION_UP: If the pressed state of `Button mStarter` is true, we call its
         * `performClick` method and set its pressed state to false, if it is not pressed
         * we fetch a `ViewPropertyAnimator` for it and have it animate the scale of X
         * to 1, and animate the scale of Y to 1 using our field `AccelerateInterpolator sAccelerator`
         * as its `TimeInterpolator`. We then set the text color of `mStarter` to
         * BLUE and break.
         *
         *
         * For all actions (including those we ignore) we return true to consume the event.
         *
         * @param v     The view the touch event has been dispatched to.
         * @param event The MotionEvent object containing full information about the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        /**
         * Called when a touch event is dispatched to a view. We switch on the action of our parameter
         * `MotionEvent event`:
         *
         *  *
         * ACTION_DOWN: we fetch a `ViewPropertyAnimator` for `Button mStarter`
         * and have it animate the scale of X to .8f, and animate the scale of Y to .8f using
         * our field `DecelerateInterpolator sDecelerator` as its `TimeInterpolator`.
         * We then set the text color of `mStarter` to CYAN, remove from its message
         * queue `Runnable mSquishRunnable`, and set its pressed state to true. We then
         * break.
         *
         *  *
         * ACTION_MOVE: we initialize `float x` with the X coordinate for the first
         * pointer index of our parameter `MotionEvent event` and `float y` with
         * the Y coordinate. We initialize `boolean isInside` to true if `x` is
         * greater than 0 and less than the width of `mStarter` and `y` is greater
         * than 0 and less than the height of `mStarter` (the move event is still inside
         * our button) and to false otherwise (the move event is outside of our button). If
         * the pressed state of `mStarter` is not equal to the value of `isInside`
         * we set its pressed state to `isInside`. We then break. (Note that the user
         * has to touch the bouncing button while it is still in order for the touch to be
         * noticed).
         *
         *  *
         * ACTION_UP: If the pressed state of `Button mStarter` is true, we call its
         * `performClick` method and set its pressed state to false, if it is not pressed
         * we fetch a `ViewPropertyAnimator` for it and have it animate the scale of X
         * to 1, and animate the scale of Y to 1 using our field `AccelerateInterpolator sAccelerator`
         * as its `TimeInterpolator`. We then set the text color of `mStarter` to
         * BLUE and break.
         *
         *
         * For all actions (including those we ignore) we return true to consume the event.
         *
         * @param v     The view the touch event has been dispatched to.
         * @param event The MotionEvent object containing full information about the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStarter!!.animate().scaleX(.8f).scaleY(.8f).interpolator = sDecelerator
                mStarter!!.setTextColor(Color.CYAN)
                mStarter!!.removeCallbacks(mSquishRunnable)
                mStarter!!.isPressed = true
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val isInside = x > 0 && x < mStarter!!.width && y > 0 && y < mStarter!!.height
                if (mStarter!!.isPressed != isInside) {
                    mStarter!!.isPressed = isInside
                }
            }

            MotionEvent.ACTION_UP -> {
                if (mStarter!!.isPressed) {
                    mStarter!!.performClick()
                    mStarter!!.isPressed = false
                } else {
                    mStarter!!.animate().scaleX(1f).scaleY(1f).setInterpolator(sAccelerator)
                }
                mStarter!!.setTextColor(Color.BLUE)
            }
        }
        true
    }

    /**
     * `Runnable` which a call to the `squishyBounce` adds to the message queue of the
     * view it is animating to call itself again after a random delay (500ms to 2500ms) after the
     * the animation ends.
     */
    /**
     * When an object implementing interface `Runnable` is used to create a thread,
     * starting the thread causes the object's `run` method to be called in that
     * separately executing thread. We just call our method `squishyBounce` to animate
     * our button `mStarter` with a TRANSLATION_Y animation from 0 to the height of
     * `ViewGroup mContainer` minus the top of `mStarter` minus the height of
     * `mStarter`, then animate its TRANSLATION_Y back to 0. In addition SCALE_Y will
     * be animated from 1 to 0.5 (squash) and SCALE_X will be animated from 1 to 1.5 (stretch).
     */
    private val mSquishRunnable = Runnable {
        squishyBounce(
            view = mStarter,
            startTY = 0f,
            bottomTY = (mContainer!!.height - mStarter!!.top - mStarter!!.height).toFloat(),
            endTY = 0f,
            squash = .5f,
            stretch = 1.5f
        )
    }

    /**
     * Specified to be called using an android:onClick="play" attribute for the button in our layout
     * with id android:id="@+id/startButton" ("Play!"). Uses a `ViewPropertyAnimator` for our
     * field `mContainer` to animate its X coordinate scale from 1 to .5, its Y coordinate scale
     * from 1 to .5 and its alpha from 1 to 0. Sets the duration of this animation to LONG_DURATION
     * and its `TimeInterpolator` to `LinearInterpolator sLinearInterpolator`. In addition
     * it specifies an action to take place when the animation ends consisting of an anonymous
     * `Runnable` which posts an anonymous `Runnable` to run on the next animation step
     * of `Button mStarter` which creates an `Intent` to launch `PlayerSetupActivity`,
     * starts it running and cancels any activity transition that would otherwise occur when that
     * activity takes over. Finally we remove `Runnable mSquishRunnable` from the queue of
     * our parameter `View view`.
     *
     * @param view View that was clicked.
     */
    fun play(view: View) {
        mContainer!!.animate().scaleX(5f).scaleY(5f).alpha(0f)
            .setDuration(LONG_DURATION)
            .setInterpolator(sLinearInterpolator)
            .withEndAction {
                mStarter!!.postOnAnimation {
                    val intent = Intent(this@ToonGame,
                        PlayerSetupActivity::class.java)
                    startActivity(intent)
                    if (Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                    } else {
                        @Suppress("DEPRECATION") // Needed for SDK older than 34
                        overridePendingTransition(0, 0)
                    }
                }
            }
        view.removeCallbacks(mSquishRunnable)
    }

    /**
     * `OnPreDrawListener` for our field `ViewGroup mContainer` whose `onPreDraw`
     * override removes itself as a `OnPreDrawListener` then posts a 500ms delayed anonymous
     * `Runnable` which "drops" `Button mStarter` from off the top of the screen using
     * our method `squishyBounce`, "squishy bouncing" at the bottom of the screen before moving
     * to its at rest position below the "Welcome!" `TextView` (`squishyBounce` then posts
     * a `Runnable` which calls itself again). The `onPreDraw` override then returns true
     * to proceed with the current drawing pass.
     */
    private val mOnPreDrawListener: OnPreDrawListener = object : OnPreDrawListener {
        /**
         * Callback method to be invoked when the view tree is about to be drawn. At this point, all
         * views in the tree have been measured and given a frame. Clients can use this to adjust
         * their scroll bounds or even to request a new layout before drawing occurs. First we
         * remove ourselves as a `OnPreDrawListener` for `ViewGroup mContainer` then
         * we post a 500ms delayed anonymous `Runnable` which sets the visibility of
         * `Button mStarter` to VISIBLE (it starts out invisible in the layout file),
         * sets its Y coordinate to minus its height (just off the screen), then calls our
         * method `squishyBounce` to animate `mStarter` from its current position
         * to the bottom of `mContainer`, squashing from 1 to .5 in its Y size, stretching
         * from 1 to 1.5 in its X size when it "hits" the bottom then moving to the 0 position
         * specified by its layout file.
         *
         * @return Return true to proceed with the current drawing pass, or false to cancel.
         */
        override fun onPreDraw(): Boolean {
            mContainer!!.viewTreeObserver.removeOnPreDrawListener(this)
            mContainer!!.postDelayed({ // Drop in the button from off the top of the screen
                mStarter!!.visibility = View.VISIBLE
                mStarter!!.y = -mStarter!!.height.toFloat()
                squishyBounce(mStarter,
                    -(mStarter!!.top + mStarter!!.height).toFloat(),
                    (mContainer!!.height - mStarter!!.top -
                        mStarter!!.height).toFloat(),
                    0f, .5f, 1.5f)
            }, 500)
            return true
        }
    }

    /**
     * Performs a complex animation of its parameter `View view` intended to emulate a squishy
     * button dropping from a height before bouncing to land in its proper position. At the end of
     * this animation it posts an anonymous `Runnable` scheduled to run at a random time in
     * future which calls this method again.
     *
     *
     * First we set the x location of the point around which our parameter `View view` is
     * scaled to half of its width and the y location of the point around it is scaled to its
     * height (it will scale relative to the bottom middle of the view). We initialize our variable
     * `PropertyValuesHolder pvhTY` to animate TRANSLATION_Y between our parameters `startTY`
     * to `bottomTY`, `PropertyValuesHolder pvhSX` to animate SCALE_X to .7, to animate
     * SCALE_Y to 1.2, then create `ObjectAnimator downAnim` to animate `view` using
     * `pvhTY`, `pvhSX` and `pvhSY` using `AccelerateInterpolator sAccelerator`
     * as its `TimeInterpolator`.
     *
     *
     * We then set `pvhTY` to a `PropertyValuesHolder` that will animate TRANSLATION_Y
     * from our parameter `bottomTY` to our parameter `endTY`, set `pvhSX` to a
     * `PropertyValuesHolder` that will animate SCALE_X to 1, set `pvhSY` to a
     * `PropertyValuesHolder` that will animate SCALE_Y to 1, then create `ObjectAnimator upAnim`
     * to animate `view` using `pvhTY`, `pvhSX` and `pvhSY` using
     * `DecelerateInterpolator sDecelerator` as its `TimeInterpolator`.
     *
     *
     * We set `pvhSX` to a `PropertyValuesHolder` that will animate SCALE_X to our parameter
     * `stretch`, and `pvhSY` to a `PropertyValuesHolder` that will animate SCALE_Y
     * to our parameter `squash`, then create `ObjectAnimator stretchAnim` to animate
     * `view` using `pvhSX` and `pvhSY`. We set its repeat count to 1, its repeat
     * mode to reverse, and its `TimeInterpolator` to `DecelerateInterpolator sDecelerator`.
     *
     *
     * We then initialize `AnimatorSet set` with a new instance, set it to play sequentially
     * `downAnim`, `stretchAnim` and `stretchAnim`, set its duration to the scaled
     * duration create from SHORT_DURATION by our method `getDuration` and start it playing.
     * Finally we set the `AnimatorListener` of `set` to an `AnimatorListenerAdapter`
     * which overrides the `onAnimationEnd` method in order to post `Runnable mSquishRunnable`
     * to the queue of `view` with a random delay between 500ms and 2500ms (`mSquishRunnable`
     * calls us again when it is run).
     *
     * @param view `View` we are going to animate
     * @param startTY starting TRANSLATION_Y value
     * @param bottomTY bottom TRANSLATION_Y value
     * @param endTY ending TRANSLATION_Y value (resting point after the bounce)
     * @param squash SCALE_Y squashing end value
     * @param stretch SCALE_X stretching end value.
     */
    @Suppress("SameParameterValue")
    private fun squishyBounce(view: View?, startTY: Float, bottomTY: Float,
                              endTY: Float, squash: Float, stretch: Float) {
        view!!.pivotX = (view.width / 2).toFloat()
        view.pivotY = view.height.toFloat()
        var pvhTY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y,
            startTY, bottomTY)
        var pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, .7f)
        var pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        val downAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhTY, pvhSX, pvhSY)
        downAnim.interpolator = sAccelerator
        pvhTY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, bottomTY, endTY)
        pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)
        pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
        val upAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhTY, pvhSX, pvhSY)
        upAnim.interpolator = sDecelerator
        pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, stretch)
        pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, squash)
        val stretchAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY)
        stretchAnim.repeatCount = 1
        stretchAnim.repeatMode = ValueAnimator.REVERSE
        stretchAnim.interpolator = sDecelerator
        val set = AnimatorSet()
        set.playSequentially(downAnim, stretchAnim, upAnim)
        set.duration = getDuration(SHORT_DURATION)
        set.start()
        set.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. We call the `postDelayed` method of `view`
             * to cause `Runnable mSquishRunnable` to be added to its message queue, to be run
             * after a random time between 500ms and 2500ms (it will be run on the user interface thread).
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                view.postDelayed(mSquishRunnable, (500 + Math.random() * 2000).toLong())
            }
        })
    }

    companion object {
        /**
         * `AccelerateInterpolator` instance we use.
         */
        private val sAccelerator = AccelerateInterpolator()

        /**
         * `DecelerateInterpolator` instance we use.
         */
        private val sDecelerator = DecelerateInterpolator()

        /**
         * `LinearInterpolator` instance we use.
         */
        private val sLinearInterpolator = LinearInterpolator()

        /**
         * The animation duration we use for faster animations
         */
        var SHORT_DURATION: Long = 100

        /**
         * The animation duration we use for medium speed animations
         */
        var MEDIUM_DURATION: Long = 200

        /**
         * The animation duration we use for normal speed animations
         */
        var REGULAR_DURATION: Long = 300

        /**
         * The animation duration we use for long running animations
         */
        var LONG_DURATION: Long = 500

        /**
         * Scale factor used by our `getDuration` method to uniformly scale the duration constants.
         */
        private const val sDurationScale = 1f

        /**
         * Returns our parameter `baseDuration` multiplied by our field `float sDurationScale`.
         *
         * @param baseDuration base duration to scale.
         * @return our parameter `baseDuration` multiplied by our field `float sDurationScale`
         */
        fun getDuration(baseDuration: Long): Long {
            return (baseDuration * sDurationScale).toLong()
        }
    }
}