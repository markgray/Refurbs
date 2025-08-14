/*
 * Copyright (C) 2007 The Android Open Source Project
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
@file:Suppress("UNUSED_PARAMETER", "ReplaceJavaStaticMethodWithKotlinAnalog", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate",
    "RedundantSuppression"
)

package com.example.android.lunarlander

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import androidx.core.graphics.scale

/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 *
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 *
 * Perform inflation from XML. First we call our super's constructor, then in our `init` block we
 * initialize our [SurfaceHolder] variable `holder` with the [SurfaceHolder] of our [SurfaceView]
 * and add 'this' as a `Callback` for it (we implement [SurfaceHolder.Callback]). Then we initialize
 * our [LunarThread] field [thread] with an anonymous class whose [Handler.handleMessage] override
 * sets the visibility of [TextView] field [mStatusText] to the value stored in the data [Bundle]
 * of its [Message] parameter `m` under the key "viz", and sets the text of [TextView] field
 * [mStatusText] to the value stored in the data [Bundle] under the key "text". We then enable the
 * focus of our [LunarView] so that we get key events.
 *
 * @param context The Context the view is running in
 * @param attrs   The attributes of the XML tag that is inflating the view.
 */
@SuppressLint("HandlerLeak")
internal class LunarView(
    context: Context,
    attrs: AttributeSet?
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    /**
     * Our animation thread constructor.
     *
     * @param mSurfaceHolder the [SurfaceHolder] for the [SurfaceView] of the [LunarView]
     * which is constructing us, which is the surface manager object we interact with
     * @param context       [Context] the [LunarView] is running in, used to access resources.
     * @param handler       [Handler] we can send messages to in order to interact with the
     * status [TextView]
     */
    internal inner class LunarThread(
        private val mSurfaceHolder: SurfaceHolder,
        context: Context,
        handler: Handler
    ) : Thread() {

        // Member (state) fields

        /**
         * The drawable to use as the background of the animation canvas
         */
        private var mBackgroundImage: Bitmap

        /**
         * Current height of the surface/canvas.
         *
         * @see [setSurfaceSize]
         */
        private var mCanvasHeight: Int = 1

        /**
         * Current width of the surface/canvas.
         *
         * @see [setSurfaceSize]
         */
        private var mCanvasWidth: Int = 1

        /**
         * What to draw for the Lander when it has crashed
         */
        private val mCrashedImage: Drawable

        /**
         * Current difficulty -- amount of fuel, allowed angle, etc. Default is [DIFFICULTY_MEDIUM].
         */
        private var mDifficulty: Int

        /**
         * Velocity dx.
         */
        private var mDX: Double

        /**
         * Velocity dy.
         */
        private var mDY: Double

        /**
         * Is the engine burning?
         */
        private var mEngineFiring: Boolean

        /**
         * What to draw for the Lander when the engine is firing
         */
        private val mFiringImage: Drawable

        /**
         * Fuel remaining
         */
        private var mFuel: Double

        /**
         * Allowed angle.
         */
        private var mGoalAngle: Int = 0

        /**
         * Allowed speed.
         */
        private var mGoalSpeed: Int = 0

        /**
         * Width of the landing pad.
         */
        private var mGoalWidth: Int = 0

        /**
         * X of the landing pad.
         */
        private var mGoalX: Int = 0

        /**
         * Message handler used by thread to interact with TextView
         */
        private val mHandler: Handler

        /**
         * Lander heading in degrees, with 0 up, 90 right. Kept in the range 0..360.
         */
        private var mHeading: Double

        /**
         * Pixel height of lander image.
         */
        private var mLanderHeight: Int

        /**
         * What to draw for the Lander in its normal state
         */
        private val mLanderImage: Drawable

        /**
         * Pixel width of lander image.
         */
        private var mLanderWidth: Int

        /**
         * Used to figure out elapsed time between frames
         */
        private var mLastTime: Long = 0

        /**
         * [Paint] to draw the lines on screen.
         */
        private val mLinePaint: Paint

        /**
         * "Bad" speed-too-high variant of the line color.
         */
        private val mLinePaintBad: Paint

        /**
         * The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN
         */
        var mMode: Int = 0

        /**
         * Currently rotating, -1 left, 0 none, 1 right.
         */
        private var mRotating: Int = 0

        /**
         * Indicate whether the surface has been created & is ready to draw
         */
        private var mRun: Boolean = false

        /**
         * Lock to synchronise access to the `boolean mRun` field.
         */
        private val mRunLock = Any()

        /**
         * Scratch rect object.
         */
        private val mScratchRect: RectF

        /**
         * Number of wins in a row.
         */
        private var mWinsInARow: Int

        /**
         * X of lander center.
         */
        private var mX: Double

        /**
         * Y of lander center.
         */
        private var mY: Double

        /**
         * First we save our parameters: `Context` parmeter `context`, and `Handler` parmeter `handler`
         * in our fields `mContext`, and `mHandler` respectively. Then we initialize `Resources`
         * variable `val res` with a `Resources` instance for the application's package and use it
         * to load the png with resource id `R.drawable.lander_plain` into our `Drawable` field
         * `mLanderImage`, the png with resource id R.drawable.lander_firing into our field Drawable
         * mFiringImage, and the png with resource id R.drawable.lander_crashed into our field
         * Drawable mCrashedImage. We then load our field Bitmap mBackgroundImage by decoding the
         * png with resource id R.drawable.earthrise. We initialize our field int mLanderWidth with
         * the width of mLanderImage and our field int mLanderHeight with its height. We initialize
         * Paint mLinePaint with a new instance, enable its anti-alias flag, and set its color to
         * green. We initialize Paint mLinePaintBad with a new instance, enable its anti-alias flag,
         * and set its color to a greenish red. We set our field int mWinsInARow to 0, and set our
         * field int mDifficulty to DIFFICULTY_MEDIUM. We initialize double mX to mLanderWidth,
         * double mY to 2 times mLanderHeight, double mFuel to PHYS_FUEL_INIT, double mDX to 0,
         * double mDY to 0, double mHeading to 0, and boolean mEngineFiring to true.
         */
        init {
            // get handles to some important objects
            mContext = context
            mHandler = handler
            val res = context.resources
            // cache handles to our key sprites & other drawables
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLanderImage = res.getDrawable(R.drawable.lander_plain, null)
                mFiringImage = res.getDrawable(R.drawable.lander_firing, null)
                mCrashedImage = res.getDrawable(R.drawable.lander_crashed, null)
            } else {
                @Suppress("DEPRECATION") // Needed for SDK less than LOLLIPOP
                mLanderImage = res.getDrawable(R.drawable.lander_plain)
                @Suppress("DEPRECATION") // Needed for SDK less than LOLLIPOP
                mFiringImage = res.getDrawable(R.drawable.lander_firing)
                @Suppress("DEPRECATION") // Needed for SDK less than LOLLIPOP
                mCrashedImage = res.getDrawable(R.drawable.lander_crashed)
            }


            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this way
            mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.earthrise)

            // Use the regular lander image as the model size for all sprites
            mLanderWidth = mLanderImage.intrinsicWidth
            mLanderHeight = mLanderImage.intrinsicHeight

            // Initialize paints for speedometer
            mLinePaint = Paint()
            mLinePaint.isAntiAlias = true
            mLinePaint.setARGB(255, 0, 255, 0)
            mLinePaintBad = Paint()
            mLinePaintBad.isAntiAlias = true
            mLinePaintBad.setARGB(255, 120, 180, 0)
            mScratchRect = RectF(0f, 0f, 0f, 0f)
            mWinsInARow = 0
            mDifficulty = DIFFICULTY_MEDIUM

            // initial show-up of lander (not yet playing)
            mX = mLanderWidth.toDouble()
            mY = (mLanderHeight * 2).toDouble()
            mFuel = PHYS_FUEL_INIT.toDouble()
            mDX = 0.0
            mDY = 0.0
            mHeading = 0.0
            mEngineFiring = true
        }

        /**
         * Starts the game, setting parameters for the current difficulty. In a block that is synchronized
         * on our [SurfaceHolder] field  mSurfaceHolder` we:
         *
         *  * Set our [Double] field [mFuel] to [PHYS_FUEL_INIT], set our [Boolean] field [mEngineFiring]
         *  to `false`, set our [Int] field [mGoalWidth] to [mLanderWidth] times [TARGET_WIDTH], set
         *  our [Int] field [mGoalSpeed] to [TARGET_SPEED], set our [Int] field [mGoalAngle] to
         *  [TARGET_ANGLE], and initialize our [Int] variable `var speedInit` to [PHYS_SPEED_INIT]
         *  (these are all the values used for [DIFFICULTY_MEDIUM]). Now we branch on the value of
         *  our [Int] field [mDifficulty]:
         *
         *  * [DIFFICULTY_EASY]: we multiply [Double] field [mFuel] by 3/2, multiply [Int] field
         *  [mGoalWidth] by 4/3, multiply [Int] field [mGoalSpeed] by by 3/2, multiply [Int] field
         *  [mGoalAngle] by 4/3, and multiply [Int] variable `speedInit` by 3/4
         *
         *  * [DIFFICULTY_HARD]: we multiply [Double] field [mFuel] by 7/8, multiply [Int] field
         *  [mGoalWidth] by 3/4, multiply [Int] field [mGoalSpeed] by by 7/8, and multiply [Int]
         *  variable `speedInit` by 4/3
         *
         *  * We initialize our [Double] field [mX] to half of our [Int] field [mCanvasWidth], and
         *  initialize our [Double] field [mY] to our [Int] field [mCanvasHeight] minus half of our
         *  [Int] field [mLanderHeight] (this is the initial location for the lander sprite).
         *
         *  * We now start the rocket ship moving with a little random motion by setting our [Double]
         *  field [mDY] (the velocity in the Y direction) to a random number between 0 and 1.0 times
         *  minus `speedInit`, [Double] field [mDX] (the velocity in the X direction) to a random
         *  number between 0 and 1.0 times 2 times `speedInit` and subtract `speedInit` from that
         *  value, and we set our [Double] field [mHeading] to 0.
         *
         *  * Then we loop setting our [Int] field [mGoalX] to a random number between 0 and 1.0
         *  times the quantity [Int] field [mCanvasWidth] minus [Int] field [mGoalWidth] until this
         *  value is farther than [Int] field [mCanvasHeight] divided by 6 from the center.
         *
         *  * Finally we initialize our [Long] field [mLastTime] to the current system time in
         *  milliseconds plus 100, and call our [setState] method to set the game state to
         *  [STATE_RUNNING].
         */
        fun doStart() {
            synchronized(mSurfaceHolder) {

                // First set the game for Medium difficulty
                mFuel = PHYS_FUEL_INIT.toDouble()
                mEngineFiring = false
                mGoalWidth = (mLanderWidth * TARGET_WIDTH).toInt()
                mGoalSpeed = TARGET_SPEED
                mGoalAngle = TARGET_ANGLE
                var speedInit: Int = PHYS_SPEED_INIT

                // Adjust difficulty params for EASY/HARD
                if (mDifficulty == DIFFICULTY_EASY) {
                    mFuel = mFuel * 3 / 2
                    mGoalWidth = mGoalWidth * 4 / 3
                    mGoalSpeed = mGoalSpeed * 3 / 2
                    mGoalAngle = mGoalAngle * 4 / 3
                    speedInit = speedInit * 3 / 4
                } else if (mDifficulty == DIFFICULTY_HARD) {
                    mFuel = mFuel * 7 / 8
                    mGoalWidth = mGoalWidth * 3 / 4
                    mGoalSpeed = mGoalSpeed * 7 / 8
                    speedInit = speedInit * 4 / 3
                }

                // pick a convenient initial location for the lander sprite
                mX = (mCanvasWidth shr 1).toDouble()
                mY = (mCanvasHeight - (mLanderHeight shr 1)).toDouble()

                // start with a little random motion
                mDY = Math.random() * -speedInit
                mDX = Math.random() * 2 * speedInit - speedInit
                mHeading = 0.0

                // Figure initial spot for landing, not too near center
                do {
                    mGoalX = (Math.random() * (mCanvasWidth - mGoalWidth)).toInt()
                } while (Math.abs(mGoalX - (mX - (mLanderWidth shr 1))) <= mCanvasHeight / 6f)
                mLastTime = System.currentTimeMillis() + 100
                setState(STATE_RUNNING)
            }
        }

        /**
         * Pauses the physics update & animation. In a block that is synchronized on our
         * [SurfaceHolder] field [mSurfaceHolder], if the current game state in [Int] field
         * [mMode] is [STATE_RUNNING] we call our [setState] method to set the game state to
         * [STATE_PAUSE].
         */
        fun pause() {
            synchronized(mSurfaceHolder) { if (mMode == STATE_RUNNING) setState(STATE_PAUSE) }
        }

        /**
         * Restores game state from the [Bundle] parameter [savedState]. Typically called when the
         * Activity is being restored after having been previously destroyed. In a block that is
         * synchronized on our [SurfaceHolder] field [mSurfaceHolder] we:
         *
         *  * Call our [setState] method to set the game state to [STATE_PAUSE].
         *
         *  * Set our [Int] field [mRotating] to 0, and our [Boolean] field [mEngineFiring] to `false`.
         *
         *  * Load our [Int] field [mDifficulty] with the value stored under the key [KEY_DIFFICULTY]
         *  in our [Bundle] parameter [savedState]
         *
         *  * Load our [Double] field [mX] with the value stored under the key [KEY_X] in our
         *  [Bundle] parameter [savedState]
         *
         *  * Load our [Double] field [mY] with the value stored under the key [KEY_Y] in our
         *  [Bundle] parameter [savedState]
         *
         *  * Load our [Double] field [mDX] with the value stored under the key [KEY_DX] in our
         *  [Bundle] parameter [savedState]
         *
         *  * Load our [Double] field [mDY] with the value stored under the key [KEY_DY] in our
         *  [Bundle] parameter [savedState]
         *
         *  * Load our [Double] field [mHeading] with the value stored under the key [KEY_HEADING]
         *  in our [Bundle] parameter  [savedState]
         *
         *  * Load our [Int] field [mLanderWidth] with the value stored under the key [KEY_LANDER_WIDTH]
         *  in our [Bundle] parameter [savedState]
         *
         *  * Load our [Int] field [mLanderHeight] with the value stored under the key [KEY_LANDER_HEIGHT]
         *  in our [Bundle] parameter [savedState]
         *
         *  * Load our [Int] field [mGoalX] with the value stored under the key [KEY_GOAL_X] in our
         *  [Bundle] parameter [savedState]
         *
         *  * Load our [Int] field [mGoalSpeed] with the value stored under the key [KEY_GOAL_SPEED]
         *  in our [Bundle] parameter [savedState]
         *
         *  * Load our [Int] field [mGoalWidth] with the value stored under the key [KEY_GOAL_WIDTH]
         *  in our [Bundle] parameter [savedState]
         *
         *  * Load our [Int] field [mWinsInARow] with the value stored under the key [KEY_WINS] in
         *  our [Bundle] parameter [savedState]
         *
         *  * And Load our [Double] field [mFuel] with the value stored under the key [KEY_FUEL] in
         *  our [Bundle] parameter [savedState]
         *
         * @param savedState [Bundle] containing the game state
         */
        @Synchronized
        fun restoreState(savedState: Bundle) {
            synchronized(mSurfaceHolder) {
                setState(STATE_PAUSE)
                mRotating = 0
                mEngineFiring = false
                mDifficulty = savedState.getInt(KEY_DIFFICULTY)
                mX = savedState.getDouble(KEY_X)
                mY = savedState.getDouble(KEY_Y)
                mDX = savedState.getDouble(KEY_DX)
                mDY = savedState.getDouble(KEY_DY)
                mHeading = savedState.getDouble(KEY_HEADING)
                mLanderWidth = savedState.getInt(KEY_LANDER_WIDTH)
                mLanderHeight = savedState.getInt(KEY_LANDER_HEIGHT)
                mGoalX = savedState.getInt(KEY_GOAL_X)
                mGoalSpeed = savedState.getInt(KEY_GOAL_SPEED)
                mGoalAngle = savedState.getInt(KEY_GOAL_ANGLE)
                mGoalWidth = savedState.getInt(KEY_GOAL_WIDTH)
                mWinsInARow = savedState.getInt(KEY_WINS)
                mFuel = savedState.getDouble(KEY_FUEL)
            }
        }

        /**
         * Called when our [start] method is called in our [surfaceCreated] override, this method
         * calls our [updatePhysics] method to update the position, speed and fuel of the rocket if
         * the game is in the [STATE_RUNNING] state, it then calls our [doDraw] method whether we
         * are running or not to draw to the surface. To do all this we loop as long as our [Boolean]
         * field [mRun] is `true`:
         *
         *  * First we initialize our [Canvas] variable `var c` to null.
         *
         *  * Then wrapped in a try block whose finally block calls the
         *  [SurfaceHolder.unlockCanvasAndPost] method of our [SurfaceHolder]
         *  field [mSurfaceHolder] with `c` if it is not `null` to make sure
         *  our Surface is not left in an inconsistent state we ...
         *
         *  * Set `c` to the [Canvas] returned when the [SurfaceHolder.lockCanvas] method of
         *  [mSurfaceHolder] is called to start editing the pixels in the surface (`c` can then
         *  be used to draw on the surface).
         *
         *  * In a block synchronized on [mSurfaceHolder] if our [Int] field [mMode] is in
         *  [STATE_RUNNING] (the game is running) we call our [updatePhysics] method to update
         *  the physics of the game, and then in a block synchronized on our field [mRunLock]
         *  if our field [mRun] is still `true` we call our method [doDraw] to draw the game to
         *  [Canvas] variable `c`.
         */
        override fun run() {
            while (mRun) {
                var c: Canvas? = null
                try {
                    c = mSurfaceHolder.lockCanvas(null)
                    synchronized(mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics()
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized(mRunLock) { if (mRun) doDraw(c) }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c)
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the Activity is being
         * suspended. In a block synchronized on our [SurfaceHolder] field [mSurfaceHolder] we:
         *
         *  * First make sure that our [Bundle] parameter [outState] is not `null`, doing nothing more
         *  if it is.
         *
         *  * We store our [Int] field [mDifficulty] under the key [KEY_DIFFICULTY] in [outState]
         *
         *  * We store our [Double] field [mX] under the key [KEY_X] in [outState]
         *
         *  * We store our [Double] field [mY] under the key [KEY_Y] in [outState]
         *
         *  * We store our [Double] field [mDX] under the key [KEY_DX] in [outState]
         *
         *  * We store our [Double] field [mDY] under the key [KEY_DY] in [outState]
         *
         *  * We store our [Double] field [mHeading] under the key [KEY_HEADING] in [outState]
         *
         *  * We store our [Int] field [mLanderWidth] under the key [KEY_LANDER_WIDTH] in [outState]
         *
         *  * We store our [Int] field [mLanderHeight] under the key [KEY_LANDER_HEIGHT] in [outState]
         *
         *  * We store our [Int] field [mGoalX] under the key [KEY_GOAL_X] in [outState]
         *
         *  * We store our [Int] field [mGoalSpeed] under the key [KEY_GOAL_SPEED] in [outState]
         *
         *  * We store our [Int] field [mGoalAngle] under the key [KEY_GOAL_ANGLE] in [outState]
         *
         *  * We store our [Int] field [mGoalWidth] under the key [KEY_GOAL_WIDTH] in [outState]
         *
         *  * We store our [Int] field [mWinsInARow] under the key [KEY_WINS] in [outState]
         *
         *  * and we store our [Double] field [mFuel] under the key [KEY_FUEL] in [outState]
         *
         * @param outState [Bundle] to store the game state variables in
         */
        fun saveState(outState: Bundle?) {
            synchronized(mSurfaceHolder) {
                if (outState != null) {
                    outState.putInt(KEY_DIFFICULTY, mDifficulty)
                    outState.putDouble(KEY_X, mX)
                    outState.putDouble(KEY_Y, mY)
                    outState.putDouble(KEY_DX, mDX)
                    outState.putDouble(KEY_DY, mDY)
                    outState.putDouble(KEY_HEADING, mHeading)
                    outState.putInt(KEY_LANDER_WIDTH, mLanderWidth)
                    outState.putInt(KEY_LANDER_HEIGHT, mLanderHeight)
                    outState.putInt(KEY_GOAL_X, mGoalX)
                    outState.putInt(KEY_GOAL_SPEED, mGoalSpeed)
                    outState.putInt(KEY_GOAL_ANGLE, mGoalAngle)
                    outState.putInt(KEY_GOAL_WIDTH, mGoalWidth)
                    outState.putInt(KEY_WINS, mWinsInARow)
                    outState.putDouble(KEY_FUEL, mFuel)
                }
            }
        }

        /**
         * Sets the current difficulty. In a block synchronized on our [SurfaceHolder] field
         * [mSurfaceHolder] we set our [Int] field [mDifficulty] to our [Int] parameter [difficulty].
         *
         * @param difficulty new difficulty to use.
         */
        fun setDifficulty(difficulty: Int) {
            synchronized(mSurfaceHolder) { mDifficulty = difficulty }
        }

        /**
         * Sets if the engine is currently firing. In a block synchronized on our [SurfaceHolder]
         * field [mSurfaceHolder] we set our [Boolean] field [mEngineFiring] to our [Boolean]
         * parameter [firing].
         *
         * @param firing `true` to start the rocket firing, `false` to stop it.
         */
        fun setFiring(firing: Boolean) {
            synchronized(mSurfaceHolder) { mEngineFiring = firing }
        }

        /**
         * Used to signal the thread whether it should be running or not. Passing `true` allows the
         * thread to run; passing `false` will shut it down if it's already running. Calling start()
         * after this was most recently called with `false` will result in an immediate shutdown.
         * In a block synchronized on our field [Any] field [mRunLock] we set our [Boolean] field
         * [mRun] to our [Boolean] parameter [b].
         *
         * @param b `true` to run, `false` to shut down
         */
        fun setRunning(b: Boolean) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized(mRunLock) { mRun = b }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the failure state, in the
         * victory state, etc. In a block synchronized on our [SurfaceHolder] field [mSurfaceHolder]
         * we call our method [setState] with our [Int] parameter [mode] as its `mode` argument and
         * with `null` for the its `message` argument.
         *
         * @param mode one of the STATE_* constants
         */
        fun setState(mode: Int) {
            synchronized(mSurfaceHolder) { setState(mode = mode, message = null) }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the failure state, in the
         * victory state, etc. In a block synchronized on our [SurfaceHolder] field [mSurfaceHolder]
         * we set our [Int] field [mMode] to our [Int] parameter [mode]. We then branch on the
         * value of this new mode:
         *
         *  * [STATE_RUNNING]: We initialize our [Message] variable `val msg` with an instance from
         *  the global message pool initialized to target [Handler] field [mHandler]. We then
         *  initialize our [Bundle] variable `val b` with a new instance, store an empty string
         *  in it under the key "text", store the [Int] const [View.INVISIBLE] under the key "viz",
         *  and set the data of `msg` to `b`. We then push `msg` onto the end of the message queue
         *  of [mHandler] after all pending messages before the current time (It will be received in
         *  [Handler.handleMessage], in the thread attached to this handler).
         *
         *  * Any other state: We set our [Int] field [mRotating] to 0 (stop rotating), set our
         *  [Boolean] field [mEngineFiring] to `false` (stop the rocket engine), initialize our
         *  [Resources] variable `val res` with a [Resources] instance for the application's package,
         *  and initialize [CharSequence] variable `var str` to the empty string. We then set the
         *  value of `str` based on the value of our game state `mMode`:
         *
         *  * [STATE_READY]: We set `str` to the string with resource id `R.string.mode_ready`
         *  ("Lunar Lander Press Up To Play")
         *
         *  * [STATE_PAUSE]: We set `str` to the string with resource id `R.string.mode_pause`
         *  ("Paused Press Up To Play")
         *
         *  * [STATE_LOSE]: We set `str` to the string with resource id `R.string.mode_lose`
         *  ("Game Over Press Up To Play")
         *
         *  * [STATE_WIN]: We set `str` to a string formed by concatenating the string with resource
         *  id `R.string.mode_win_prefix` followed by the string value of our [Int] field [mWinsInARow]
         *  followed by the string with resource id `R.string.mode_win_suffix` ("Success! [mWinsInARow]
         *  in a row Press Up To Play")
         *
         *
         *  * If our [CharSequence] parameter [message] is not `null` we set `str` to the string
         *  created by concatenating [message] followed by a newline character, followed by `str`.
         *
         *  * If the new value of our [Int] field [mMode] is [STATE_LOSE], we set our [Int] field
         *  [mWinsInARow] to 0.
         *
         *  * We initialize our [Message] variable `val msg` with an instance from the global message
         *  pool initialized to target [Handler] field [mHandler]. We then initialize our [Bundle]
         *  variable `val b` with a new instance, store `str` in it under the key "text", store the
         *  [Int] const [View.VISIBLE] under the key "viz", and set the data of `msg` to `b`
         *
         *  * We then push `msg` onto the end of the message queue of [mHandler] after all pending
         *  messages before the current time (It will be received in [Handler.handleMessage], in the
         *  thread attached to this handler).
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        fun setState(mode: Int, message: CharSequence?) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized(mSurfaceHolder) {
                mMode = mode
                if (mMode == STATE_RUNNING) {
                    val msg = mHandler.obtainMessage()
                    val b = Bundle()
                    b.putString("text", "")
                    b.putInt("viz", INVISIBLE)
                    msg.data = b
                    mHandler.sendMessage(msg)
                } else {
                    mRotating = 0
                    mEngineFiring = false
                    val res = mContext!!.resources
                    var str: CharSequence = ""
                    when (mMode) {
                        STATE_READY -> str = res.getText(R.string.mode_ready)
                        STATE_PAUSE -> str = res.getText(R.string.mode_pause)
                        STATE_LOSE -> str = res.getText(R.string.mode_lose)
                        STATE_WIN -> str = (res.getString(R.string.mode_win_prefix)
                            + mWinsInARow + " "
                            + res.getString(R.string.mode_win_suffix))
                    }
                    if (message != null) {
                        str = """
                        $message
                        $str
                        """.trimIndent()
                    }
                    if (mMode == STATE_LOSE) mWinsInARow = 0
                    val msg = mHandler.obtainMessage()
                    val b = Bundle()
                    b.putString("text", str.toString())
                    b.putInt("viz", VISIBLE)
                    msg.data = b
                    mHandler.sendMessage(msg)
                }
            }
        }

        /**
         * Callback invoked when the surface dimensions change. In a block synchronized on our
         * [SurfaceHolder] field [mSurfaceHolder] we store our [Int] parameter [width] in our [Int]
         * field [mCanvasWidth], and our [Int] parameter [height] in our [Int] field [mCanvasHeight].
         * Finally we resize our [Bitmap] field [mBackgroundImage] to be `width` by `height`.
         *
         * @param width  new width of our surface
         * @param height new height of our surface
         */
        fun setSurfaceSize(width: Int, height: Int) {
            // synchronized to make sure these all change atomically
            synchronized(mSurfaceHolder) {
                mCanvasWidth = width
                mCanvasHeight = height

                // don't forget to resize the background image
                mBackgroundImage = mBackgroundImage.scale(width = width, height = height)
            }
        }

        /**
         * Resumes from a pause. In a block synchronized on our [SurfaceHolder] field [mSurfaceHolder]
         * we set our [Long] field [mLastTime] to the current system time in milliseconds plus 100.
         * After exiting the synchronized block we call our method [setState] to set the game state
         * to [STATE_RUNNING].
         */
        fun unpause() {
            // Move the real time clock up to now
            synchronized(mSurfaceHolder) { mLastTime = System.currentTimeMillis() + 100 }
            setState(STATE_RUNNING)
        }

        /**
         * Handles a key-down event. In a block synchronized on our [SurfaceHolder] field
         * [mSurfaceHolder] we initialize our [Boolean] variable `var okStart` to false. Then we set
         * it to `true` if the value of our [Int] parameter [keyCode] is [KeyEvent.KEYCODE_DPAD_UP],
         * [KeyEvent.KEYCODE_DPAD_DOWN], or [KeyEvent.KEYCODE_S]. We then branch based on the
         * following conditions:
         *
         *  * `okStart` is true and our [Int] field [mMode] is [STATE_READY], [STATE_LOSE], or
         *  [STATE_WIN]: (ready-to-start -> start) we call our method [doStart] and return `true`
         *  to consume the event.
         *
         *  * Our [Int] field [mMode] is [STATE_PAUSE] and `okStart` is `true`: (paused -> running)
         *  we call our method [unpause] to resume the paused game and return `true` to consume the
         *  event.
         *
         *  * Our [Int] field [mMode] is [STATE_RUNNING]: (game is running) We branch on the value
         *  of our [Int] parameter [keyCode]:
         *
         *  - [KeyEvent.KEYCODE_DPAD_CENTER] or [KeyEvent.KEYCODE_SPACE]: (center/space -> fire) We
         *  call our method [setFiring] with `true` to start the rocket engine and return `true` to
         *  consume the event.
         *
         *  - [KeyEvent.KEYCODE_DPAD_LEFT] or [KeyEvent.KEYCODE_Q]: (left/q -> left) We set our [Int]
         *  field `[mRotating] to -1 and return `true` to consume the event.
         *
         *  * [KeyEvent.KEYCODE_DPAD_RIGHT] or [KeyEvent.KEYCODE_W]: (right/w -> right) We set our
         *  [Int] field [mRotating] to 1 and return `true` to consume the event.
         *
         *  * [KeyEvent.KEYCODE_DPAD_UP]: (up -> pause) We call our method [pause] to pause the game
         *  and return `true` to consume the event.
         *
         * If the [Int] parameter [keyCode] is not one we are interested in at this point in the
         * game we return `false` to the caller to allow the event to be handled by the next
         * receiver.
         *
         * @param keyCode the key that was pressed
         * @param ignoredMsg     the original event object
         * @return `true` if we consumed the event, `false` if the key is not one we use.
         */
        @Suppress("UNUSED_PARAMETER")
        fun doKeyDown(keyCode: Int, ignoredMsg: KeyEvent?): Boolean {
            synchronized(mSurfaceHolder) {
                var okStart = keyCode == KeyEvent.KEYCODE_DPAD_UP
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true
                if (keyCode == KeyEvent.KEYCODE_S) okStart = true
                if (okStart
                    && (mMode == STATE_READY || mMode == STATE_LOSE || mMode == STATE_WIN)) {
                    // ready-to-start -> start
                    doStart()
                    return true
                } else if (mMode == STATE_PAUSE && okStart) {
                    // paused -> running
                    unpause()
                    return true
                } else if (mMode == STATE_RUNNING) {
                    // center/space -> fire
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
                            setFiring(true)
                            return true
                            // left/q -> left
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_Q -> {
                            mRotating = -1
                            return true
                            // right/w -> right
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_W -> {
                            mRotating = 1
                            return true
                            // up -> pause
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            pause()
                            return true
                        }
                    }
                }
                return false
            }
        }

        /**
         * Handles a key-up event. First we set our [Boolean] variable `var handled` to `false`, then
         * in a block synchronized on our [SurfaceHolder] field [mSurfaceHolder] if our [Int] field
         * [mMode] is [STATE_RUNNING] (game is running) we branch on the value of our [Int] parameter
         * [keyCode]:
         *
         *  * [KeyEvent.KEYCODE_DPAD_CENTER] or [KeyEvent.KEYCODE_SPACE]: We call our method
         *  [setFiring] with `false` to stop the rocket engine firing, and set `handled` to `true`.
         *
         *  * [KeyEvent.KEYCODE_DPAD_LEFT], [KeyEvent.KEYCODE_Q], [KeyEvent.KEYCODE_DPAD_RIGHT] or
         *  [KeyEvent.KEYCODE_W]: We set our [Int] field [mRotating] to 0, and set `handled` to `true`.
         *
         * Having exited the synchronized block we return `handled` to the caller.
         *
         * @param keyCode the key that was pressed
         * @param ignoredMsg     the original event object
         * @return `true` if the key was handled and consumed, or else `false`
         */
        @Suppress("UNUSED_PARAMETER")
        fun doKeyUp(keyCode: Int, ignoredMsg: KeyEvent?): Boolean {
            var handled = false
            synchronized(mSurfaceHolder) {
                if (mMode == STATE_RUNNING) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_SPACE) {
                        setFiring(false)
                        handled = true
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_Q
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                        || keyCode == KeyEvent.KEYCODE_W) {
                        mRotating = 0
                        handled = true
                    }
                }
            }
            return handled
        }

        /**
         * Draws the ship, fuel/speed bars, and background to the provided [Canvas]. First we have
         * our [Canvas] parameter [canvas] draw the background image in our [Bitmap] field
         * [mBackgroundImage] on itself at (0,0). Then we initialize our [Int] variable `val yTop`
         * (the top Y coordinate for the drawing of the rocket ship) to our [Int] field [mCanvasHeight]
         * minus the quantity [Double] field [mY] (the Y location of the ship) plus half of the lander
         * height [Int] field [mLanderHeight]. We initialize our [Int] variable `val xLeft` (the left
         * X coordinate for the drawing of the rocket ship) to our field [mX] (the X location of the
         * ship) minus half of the width of the ship [Int] field [mLanderWidth]. We then draw the
         * fuel gauge by initializing our [Int] variable `val fuelWidth` to the width of the bar
         * given by const [UI_BAR] (100)  times the fraction of fuel remaining ([Double] field [mFuel]
         * divided by [PHYS_FUEL_MAX] (100)). We set our scratch rectangle [RectF] field [mScratchRect]
         * to have its left top corner at (4,4), and its right bottom corner at 4 plus `fuelWidth`
         * for the X coordinate and 4 plus [UI_BAR_HEIGHT] for the Y coordinate, then have our [Canvas]
         * parameter [canvas] draw [mScratchRect] on itself using [Paint] field [mLinePaint] as the
         * [Paint]. We next want to draw the speed gauge with a two-tone effect and to do this we
         * initialize [Double] variable `val speed` to the square root of [mDX] squared plus [mDY]
         * squared (the length of the speed vector given the X velocity and Y velocity), then
         * initialize [Int] variable `val speedWidth` to `speed` divided by [PHYS_SPEED_MAX] (120)
         * times the size of our UI bar [UI_BAR] (100). If `speed` is less than or equal to [Int]
         * field [mGoalSpeed] we set the rectangle [mScratchRect] to have its upper left corner at 4
         * plus [UI_BAR] plus 4 for the X coordinate, 4 for the Y coordinate and its bottom right
         * corner at 4 plus [UI_BAR] plus 4 plus `speedWidth` for the X coordinate and 4 plus
         * [UI_BAR_HEIGHT] for the Y coordinate. We then have [canvas] draw [mScratchRect] on itself
         * using [mLinePaint] as the [Paint]. If on the other hand `speed` is greater than [mGoalSpeed]
         * we want to draw the bad color in back, with the good color in front of it, so we set the
         * rectangle [mScratchRect] to have its upper left corner at 4 plus [UI_BAR] plus 4 for the
         * X coordinate, 4 for the Y coordinate and its bottom right corner at 4 plus [UI_BAR] plus
         * 4 plus `speedWidth` for the X coordinate and 4 plus [UI_BAR_HEIGHT] for the Y coordinate.
         * We then have [canvas] draw [mScratchRect] on itself using [mLinePaintBad] as the paint.
         * We then initialize [Int] variable `val goalWidth` to be [mGoalSpeed] divided by
         * [PHYS_SPEED_MAX] times [UI_BAR], and set the rectangle [mScratchRect] to have its upper
         * left corner at 4 plus [UI_BAR] plus 4 for the X coordinate, 4 for the Y coordinate and its
         * bottom right corner at 4 plus [UI_BAR] plus 4 plus `goalWidth` for the X coordinate and 4
         * plus [UI_BAR_HEIGHT] for the Y coordinate. We then have [canvas] draw [mScratchRect] on
         * itself using [mLinePaint] as the [Paint].
         *
         * We now want to draw the landing pad, so we have [canvas] draw a line on itself from
         * ([mGoalX], 1+[mGoalX]-[TARGET_PAD_HEIGHT]) to
         * ([mGoalX]+[mGoalWidth], 1+[mCanvasHeight]-[TARGET_PAD_HEIGHT]) using
         * [mLinePaint] as the paint.
         *
         * Now we want to draw the ship with its current rotation so we have [canvas] save the
         * current matrix and clip onto a private stack, then rotate [canvas] by [mHeading] degrees
         * around the point ([mX], [mCanvasHeight]-[mY]). If the game state in [Int] field [mMode]
         * is [STATE_LOSE] we set the bounding rectangle of [mCrashedImage] to a rectangle whose
         * left top corner is (`xLeft`, `yTop`) and whose right bottom corner is
         * (`xLeft`+[mLanderWidth], `yTop`+[mLanderHeight]). We then have [mCrashedImage] draw
         * itself on [canvas]. If on the other hand [mEngineFiring] is firing we set the bounding
         * rectangle of [mFiringImage] to a rectangle whose left top corner is (`xLeft`, `yTop`) and
         * whose right bottom corner is (`xLeft`+[mLanderWidth], `yTop`+[mLanderHeight]). We then
         * have [mFiringImage] draw itself on [canvas]. If neither of these are `true` we set the
         * bounding rectangle of [mLanderImage] to a rectangle whose left top corner is
         * (`xLeft`, `yTop`) and whose right bottom corner is
         * (`xLeft`+[mLanderWidth], `yTop`+[mLanderHeight]). We then have [mLanderImage] draw itself
         * on [canvas].
         *
         * Finally we have [canvas] restore its state to the values it placed on its stack.
         *
         * @param canvas the [Canvas] we are to draw to.
         */
        private fun doDraw(canvas: Canvas?) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas!!.drawBitmap(mBackgroundImage, 0f, 0f, null)
            val yTop: Int = mCanvasHeight - (mY.toInt() + mLanderHeight / 2)
            val xLeft: Int = mX.toInt() - mLanderWidth / 2

            // Draw the fuel gauge
            val fuelWidth: Int = (UI_BAR * mFuel / PHYS_FUEL_MAX).toInt()
            mScratchRect.set(
                /* left = */ 4f,
                /* top = */ 4f,
                /* right = */ (4 + fuelWidth).toFloat(),
                /* bottom = */ (4 + UI_BAR_HEIGHT).toFloat()
            )
            canvas.drawRect(mScratchRect, mLinePaint)

            // Draw the speed gauge, with a two-tone effect
            val speed: Double = Math.hypot(mDX, mDY)
            val speedWidth: Int = (UI_BAR * speed / PHYS_SPEED_MAX).toInt()
            if (speed <= mGoalSpeed) {
                mScratchRect.set(
                    /* left = */ (4 + UI_BAR + 4).toFloat(),
                    /* top = */ 4f,
                    /* right = */ (4 + UI_BAR + 4 + speedWidth).toFloat(),
                    /* bottom = */ (4 + UI_BAR_HEIGHT).toFloat()
                )
                canvas.drawRect(mScratchRect, mLinePaint)
            } else {
                // Draw the bad color in back, with the good color in front of
                // it
                mScratchRect.set(
                    /* left = */ (4 + UI_BAR + 4).toFloat(),
                    /* top = */ 4f,
                    /* right = */ (4 + UI_BAR + 4 + speedWidth).toFloat(),
                    /* bottom = */ (4 + UI_BAR_HEIGHT).toFloat()
                )
                canvas.drawRect(mScratchRect, mLinePaintBad)
                val goalWidth: Int = UI_BAR * mGoalSpeed / PHYS_SPEED_MAX
                mScratchRect.set(
                    /* left = */ (4 + UI_BAR + 4).toFloat(),
                    /* top = */ 4f,
                    /* right = */ (4 + UI_BAR + 4 + goalWidth).toFloat(),
                    /* bottom = */ (4 + UI_BAR_HEIGHT).toFloat()
                )
                canvas.drawRect(mScratchRect, mLinePaint)
            }

            // Draw the landing pad
            canvas.drawLine(
                /* startX = */ mGoalX.toFloat(),
                /* startY = */ (1 + mCanvasHeight - TARGET_PAD_HEIGHT).toFloat(),
                /* stopX = */ (mGoalX + mGoalWidth).toFloat(),
                /* stopY = */ (1 + mCanvasHeight - TARGET_PAD_HEIGHT).toFloat(),
                /* paint = */ mLinePaint
            )


            // Draw the ship with its current rotation
            @SuppressLint("UseKtx") // TODO: Replace with Canvas.withRotation someday
            canvas.save()
            canvas.rotate(
                /* degrees = */ mHeading.toFloat(),
                /* px = */ mX.toFloat(),
                /* py = */ mCanvasHeight - mY.toFloat()
            )
            if (mMode == STATE_LOSE) {
                mCrashedImage.setBounds(
                    /* left = */ xLeft,
                    /* top = */ yTop,
                    /* right = */ xLeft + mLanderWidth,
                    /* bottom = */ yTop + mLanderHeight
                )
                mCrashedImage.draw(canvas)
            } else if (mEngineFiring) {
                mFiringImage.setBounds(
                    /* left = */ xLeft,
                    /* top = */ yTop,
                    /* right = */ xLeft + mLanderWidth,
                    /* bottom = */ yTop + mLanderHeight
                )
                mFiringImage.draw(canvas)
            } else {
                mLanderImage.setBounds(
                    /* left = */ xLeft,
                    /* top = */ yTop,
                    /* right = */ xLeft + mLanderWidth,
                    /* bottom = */ yTop + mLanderHeight
                )
                mLanderImage.draw(canvas)
            }
            canvas.restore()
        }

        /**
         * Figures the lander state (x, y, fuel, ...) based on the passage of realtime. Does not
         * [invalidate]. Called at the start of [draw]. Detects the end-of-game and sets the UI to
         * the next state. First we initialize [Long] variable `val now` to the current system
         * time in milliseconds, and if [Long] field [mLastTime] is greater than this we return
         * having done nothing (our game start sets [mLastTime] to 100ms in the future to delay the
         * start of the game). Otherwise we initialize [Double] variable `val elapsed` to the quantity
         * `now` minus [mLastTime] all divided by 1000.0 (the time that has elapsed since [mLastTime]
         * in seconds). If our [Int] field [mRotating] is not zero we want to update our heading so
         * we add [mRotating] times [PHYS_SLEW_SEC], times `elapsed` to our heading [Double] field
         * [mHeading], and if it is less than 0 we normalize it by adding 360 degrees or if it is
         * greater than or equal to 360 we normalize it by subtracting 360 degrees.
         *
         * We next initialize our base accelerations [Double] variable `var ddx` (0 for x), and
         * [Double] variable `var ddy` (gravity for y: minus [PHYS_DOWN_ACCEL_SEC] times `elapsed`).
         * If [mEngineFiring] is `true` (the rocket engine is firing) we need to adjust these so we
         * initialize [Double] variable `var elapsedFiring` to `elapsed` and initialize [Double]
         * variable `var fuelUsed` to `elapsedFiring` times [PHYS_FUEL_SEC]. If `fuelUsed` (the
         * amount of fuel we used) is greater than [Double] field [mFuel] (the amount of fuel we
         * had left) we have to adjust for the fact we ran out of fuel partway through the time
         * that has elapsed, and if so we set `elapsedFiring` to [mFuel] divided by `fuelUsed`
         * times `elapsed` (the amount of time our fuel lasted during this time period), and set
         * `fuelUsed` to [mFuel]. We then set [mEngineFiring] to `false`. Having corrected the
         * `fuelUsed` if needed we subtract it from [mFuel] then initialize [Double] variable
         * `val accel` to [PHYS_FIRE_ACCEL_SEC] times `elapsedFiring` (acceleration from the
         * engine), and initialize [Double] variable `val radians` to 2 times pi times [Double]
         * field [mHeading] divided by 360 (our heading in radians). We then set `ddx` to the sin
         * of `radians` times `accel` and `ddy` to the cosine of `radians` times `accel`.
         *
         * Having updated `ddx` and `ddy` if the rocket engine was firing, we initialize [Double]
         * variable `val dxOld` to [Double] field [mDX] and [Double] variable `val dyOld` to [Double]
         * field [mDY], add `ddx` to [mDX] and `ddy` to [mDY] (the speeds for the end of the period)
         * then update the position ([mX], [mY]) based on average speed during the period (ie. add
         * `elapsed` times the quantity [mDX] plus `dxOld` all divided by 2 to [mX], and add `elapsed`
         * times the quantity [mDY] plus `dyOld` all divided by 2 to to [mY]). We then set our [Long]
         * field [mLastTime] to `now`.
         *
         * We now need to evaluate whether we have landed, so to do this we initialize [Double]
         * variable `val yLowerBound` to [TARGET_PAD_HEIGHT] (8) plus [Int] field [mLanderHeight]
         * divided by 2 minus [TARGET_BOTTOM_PADDING] (17) and if [mY] is now less than or equal to
         * `yLowerBound` the Eagle has landed and we need to see if we won or lost (if it is still
         * greater the game goes on and our method is done). To determine if we won or lost we set
         * [mY] to `yLowerBound`, initialize [Int] variable `var result` to [STATE_LOSE], [CharSequence]
         * variable `var message` to the empty string, [Resources] variable `val res` to a [Resources]
         * instance for the application's package, [Double] variable `val speed` to the magnitude of
         * the vector ([mDX],[mDY]), and [Boolean] variable `val onGoal` to the value of the inequality
         * [mGoalX] is less than or equal to [mX] minus half of [mLanderWidth], and [mX] plus half of
         * [mLanderWidth] is less than or equal to [mGoalX] plus [mGoalWidth] (when true we are on
         * the landing pad). Next we check to see if we have achieved the oddball "Hyperspace" win
         * (upside down, going fast) which happens when `onGoal` is `true` and the absolute value of
         * [Double] field [mHeading] minus 180 is less than [Int] field [mGoalAngle] and `speed` is
         * greater than [PHYS_SPEED_HYPERSPACE], in which case we increment [Int] field [mWinsInARow]
         * call our [doStart] method to restart the game back at the top and then we return.
         *
         * In all other cases we need to evaluate whether we have failed to achieve our goals, so we
         * proceed to branch evaluating each in turn:
         *
         *  * `onGoal` is `false`: (we have missed the landing pad) We set `message` to the string
         *  with resource id `R.string.message_off_pad` ("Off Landing Pad")
         *
         *  * [Double] field [mHeading] is greater than [Int] field [mGoalAngle] or less than 360
         *  minus [mGoalAngle] we set `message` to the string with resource id `R.string.message_bad_angle`
         *  ("Bad Angle").
         *
         *  * `speed` is greater than [Int] field [mGoalSpeed] we set `message` to the string with
         *  resource id `R.string.message_too_fast` ("Too Fast").
         *
         *  * If we succeed in avoiding loss due to the above tests we set `result` to [STATE_WIN]
         *  and increment [Int] field [mWinsInARow].
         *
         * Finally we call our [setState] method to set the game state to `result` and display
         * `message` (if we lost that is, it is still the empty string if we won).
         */
        private fun updatePhysics() {
            val now: Long = System.currentTimeMillis()

            // Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) return
            val elapsed: Double = (now - mLastTime) / 1000.0

            // mRotating -- update heading
            if (mRotating != 0) {
                mHeading += mRotating * (PHYS_SLEW_SEC * elapsed)

                // Bring things back into the range 0..360
                if (mHeading < 0) mHeading += 360.0 else if (mHeading >= 360) mHeading -= 360.0
            }

            // Base accelerations -- 0 for x, gravity for y
            var ddx = 0.0
            var ddy: Double = -PHYS_DOWN_ACCEL_SEC * elapsed
            if (mEngineFiring) {
                // taking 0 as up, 90 as to the right
                // cos(deg) is ddy component, sin(deg) is ddx component
                var elapsedFiring = elapsed
                var fuelUsed = elapsedFiring * PHYS_FUEL_SEC

                // tricky case where we run out of fuel partway through the
                // time that has elapsed
                if (fuelUsed > mFuel) {
                    elapsedFiring = mFuel / fuelUsed * elapsed
                    fuelUsed = mFuel

                    // Oddball case where we adjust the "control" from here
                    mEngineFiring = false
                }
                mFuel -= fuelUsed

                // have this much acceleration from the engine
                val accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring
                val radians = 2 * Math.PI * mHeading / 360
                ddx = Math.sin(radians) * accel
                ddy += Math.cos(radians) * accel
            }
            val dxOld: Double = mDX
            val dyOld: Double = mDY

            // figure speeds for the end of the period
            mDX += ddx
            mDY += ddy

            // figure position based on average speed during the period
            mX += elapsed * (mDX + dxOld) / 2
            mY += elapsed * (mDY + dyOld) / 2
            mLastTime = now

            // Evaluate if we have landed ... stop the game
            val yLowerBound: Double = (
                TARGET_PAD_HEIGHT + (mLanderHeight shr 1) - TARGET_BOTTOM_PADDING
                ).toDouble()
            if (mY <= yLowerBound) {
                mY = yLowerBound
                var result: Int = STATE_LOSE
                var message: CharSequence? = ""
                val res: Resources = mContext!!.resources
                val speed: Double = Math.hypot(mDX, mDY)
                val onGoal: Boolean = (mGoalX <= mX - mLanderWidth / 2f
                    && mX + mLanderWidth / 2f <= mGoalX + mGoalWidth)

                // "Hyperspace" win -- upside down, going fast,
                // puts you back at the top.
                if (onGoal && Math.abs(mHeading - 180) < mGoalAngle && speed > PHYS_SPEED_HYPERSPACE) {
                    mWinsInARow++
                    doStart()
                    return
                    // Oddball case: this case does a return, all other cases
                    // fall through to setMode() below.
                } else if (!onGoal) {
                    message = res.getText(R.string.message_off_pad)
                } else if (!(mHeading <= mGoalAngle || mHeading >= 360 - mGoalAngle)) {
                    message = res.getText(R.string.message_bad_angle)
                } else if (speed > mGoalSpeed) {
                    message = res.getText(R.string.message_too_fast)
                } else {
                    result = STATE_WIN
                    mWinsInARow++
                }
                setState(result, message)
            }
        }


    }

    /**
     * Handle to the application context, used to e.g. fetch Drawables.
     */
    private var mContext: Context? = null

    /**
     * Pointer to the text view to display "Paused.." etc.
     */
    private var mStatusText: TextView? = null

    /**
     * The thread that actually draws the animation
     */
    val thread: LunarThread

    /**
     * We initialize our `SurfaceHolder` variable `holder` with the `SurfaceHolder` of our `SurfaceView`
     * and add 'this' as a Callback for it (we implement `SurfaceHolder.Callback`). Then we initialize
     * our `LunarThread` field `thread` with an anonymous class whose `Handler.handleMessage` override
     * sets the visibility of `TextView` field `mStatusText` to the value stored in the data Bundle of
     * its Message parameter `m` under the key "viz", and sets the text of `TextView` field `mStatusText`
     * to the value stored in the data Bundle under the key "text". We then enable the focus of our
     * LunarView so that we get key events.
     */
    init {

        // register our interest in hearing about changes to our surface
        val holder: SurfaceHolder = holder
        holder.addCallback(this)

        // create thread only; it's started in surfaceCreated()
        thread = LunarThread(holder, context, object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(m: Message) {
                mStatusText!!.visibility = m.data.getInt("viz")
                mStatusText!!.text = m.data.getString("text")
            }
        })
        isFocusable = true // make sure we get key events
    }

    /**
     * Standard override to get key-press events. We just return the value returned by the
     * [LunarThread.doKeyDown] method of our [LunarThread] field [thread].
     *
     * @param keyCode a key code that represents the button pressed, from
     * [android.view.KeyEvent]
     * @param msg     the KeyEvent object that defines the button action
     * @return true to consume the event, false to pass it on.
     */
    override fun onKeyDown(keyCode: Int, msg: KeyEvent): Boolean {
        return thread.doKeyDown(keyCode, msg)
    }

    /**
     * Standard override for key-up. We actually care about these, so we can turn off the engine or
     * stop rotating. We just return the value returned by the [LunarThread.doKeyUp] method of our
     * [LunarThread] field [thread].
     *
     * @param keyCode A key code that represents the button pressed, from
     * [android.view.KeyEvent].
     * @param msg     The KeyEvent object that defines the button action.
     * @return true to consume the event, false to pass it on.
     */
    override fun onKeyUp(keyCode: Int, msg: KeyEvent): Boolean {
        return thread.doKeyUp(keyCode, msg)
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on focus lost. e.g. user
     * switches to take a call. If our [Boolean] parameter [hasWindowFocus] is `false` (we have lost
     * focus) we call the [LunarThread.pause] method of our [LunarThread] field [thread] to pause the
     * game.
     *
     * @param hasWindowFocus `true` if the window containing this view now has
     * focus, `false` otherwise.
     */
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus) thread.pause()
    }

    /**
     * Installs a pointer to the [TextView] field [mStatusText] used for messages.
     *
     * @param textView the [TextView] to use for our [TextView] field [mStatusText]
     */
    fun setTextView(textView: TextView?) {
        mStatusText = textView
    }

    /**
     * This is called immediately after any structural changes (format or size) have been made
     * to the surface. Callback invoked when the surface dimensions change. We just call the
     * [LunarThread.setSurfaceSize] method of our [LunarThread] field [thread] to have it set
     * its width to our [Int] parameter [width] and its height to our [Int] parameter [height].
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new [PixelFormat] of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        thread.setSurfaceSize(width, height)
    }

    /**
     * Callback invoked when the Surface has been created and is ready to be used. We first call the
     * [LunarThread.setRunning] method of our [LunarThread] field [thread]  with `true` to signal
     * the thread that it should be running (sets its [Boolean] field [LunarThread.mRun] to `true`)
     * then call its [LunarThread.start] method to have its [LunarThread.run] override called to
     * actually start it running.
     *
     * @param holder The [SurfaceHolder] whose surface is being created.
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true)
        thread.start()
    }

    /**
     * Callback invoked when the Surface has been destroyed and must no longer be touched.
     * WARNING: after this method returns, the Surface/Canvas must never be touched again!
     * We initialize our [Boolean] variable `var retry` to `true` then call the
     * [LunarThread.setRunning] method of our [LunarThread] field [thread] with `false` to signal
     * the thread that it should stop running (sets its [Boolean] field [LunarThread.mRun] to
     * `false`). Then while our variable `retry` remains `true` we loop executing a try block
     * intended to catch and log [InterruptedException] calling the [LunarThread.join] method of
     * our [LunarThread] field [thread], setting `retry` to `false` only when the [LunarThread.join]
     * method returns having waited for the thread to die without being interrupted.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        var retry = true
        thread.setRunning(false)
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (e: InterruptedException) {
                Log.i(TAG, "surfaceDestroyed was interrupted")
            }
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG = "LunarView"

        //Difficulty setting constants

        /**
         * Easy difficulty: our fuel is multiplied by 3/2, our goals are larger, and our speed is slower.
         */
        const val DIFFICULTY_EASY = 0

        /**
         * Hard difficulty: our fuel is multiplied by 7/8, our goals are smaller, and our speed is faster.
         */
        const val DIFFICULTY_HARD = 1

        /**
         * Medium difficulty, the default: fuel, goals and speed are the default values.
         */
        const val DIFFICULTY_MEDIUM = 2

        // Physics constants

        /**
         * Downward acceleration due to gravity.
         */
        const val PHYS_DOWN_ACCEL_SEC = 35

        /**
         * Acceleration due to the engine firing.
         */
        const val PHYS_FIRE_ACCEL_SEC = 80

        /**
         * Initial amount of fuel available.
         */
        const val PHYS_FUEL_INIT = 60

        /**
         * Maximum amount of fuel displayed by our "fuel gage".
         */
        const val PHYS_FUEL_MAX = 100

        /**
         * Amount of fuel burned per second when the engine is firing.
         */
        const val PHYS_FUEL_SEC = 10

        /**
         * Rotation speed in degrees/second.
         */
        const val PHYS_SLEW_SEC = 120 // degrees/second rotate

        /**
         * "Hyperspace" speed -- upside down, going faster than this puts you back at the top.
         */
        const val PHYS_SPEED_HYPERSPACE = 180

        /**
         * Initial speed when starting.
         */
        const val PHYS_SPEED_INIT = 30

        /**
         * Maximum speed for our speed gauge.
         */
        const val PHYS_SPEED_MAX = 120

        // State-tracking constants

        /**
         * The player has lost the game.
         */
        const val STATE_LOSE = 1

        /**
         * The game is paused.
         */
        const val STATE_PAUSE = 2

        /**
         * The game is ready to start running.
         */
        const val STATE_READY = 3

        /**
         * The game is running.
         */
        const val STATE_RUNNING = 4

        /**
         * The player has won the game.
         */
        const val STATE_WIN = 5

        // Goal condition constants

        /**
         * Any angle greater than this is a "Crash".
         */
        const val TARGET_ANGLE = 18 // > this angle means crash

        /**
         * Number of pixels below the landing gear, used to determine if we have landed.
         */
        const val TARGET_BOTTOM_PADDING = 17 // px below gear

        /**
         * How high are we above the ground, used to determine if we have landed.
         */
        const val TARGET_PAD_HEIGHT = 8 // how high above ground

        /**
         * Target speed, any speed greater than this when landed is a "crash".
         */
        const val TARGET_SPEED = 28 // > this speed means crash

        /**
         * Width of our landing target.
         */
        const val TARGET_WIDTH = 1.6 // width of target

        // UI constants (i.e. the speed & fuel bars)

        /**
         * Width of the bar(s) in the X direction.
         */
        const val UI_BAR = 100 // width of the bar(s)

        /**
         * Height of the bar(s) in the Y direction.
         */
        const val UI_BAR_HEIGHT = 10 // height of the bar(s)

        // Keys used for storing our state in the Bundle passed saveState, later restored in restoreState

        /**
         * Key used to store our field `int mDifficulty` (level of difficulty)
         */
        private const val KEY_DIFFICULTY = "mDifficulty"

        /**
         * Key used to store our field `double mDX` (Velocity in the X direction)
         */
        private const val KEY_DX = "mDX"

        /**
         * Key used to store our field `double mDY` (Velocity in the Y direction)
         */
        private const val KEY_DY = "mDY"

        /**
         * Key used to store our field `double mFuel` (Fuel remaining)
         */
        private const val KEY_FUEL = "mFuel"

        /**
         * Key used to store our field `int mGoalAngle` (Goal for the lander heading)
         */
        private const val KEY_GOAL_ANGLE = "mGoalAngle"

        /**
         * Key used to store our field `int mGoalSpeed` (Goal for the speed when landed)
         */
        private const val KEY_GOAL_SPEED = "mGoalSpeed"

        /**
         * Key used to store our field `int mGoalWidth` (Width of the landing pad)
         */
        private const val KEY_GOAL_WIDTH = "mGoalWidth"

        /**
         * Key used to store our field `int mGoalX` (X of the landing pad)
         */
        private const val KEY_GOAL_X = "mGoalX"

        /**
         * Key used to store our field `double mHeading` (Lander heading in degrees)
         */
        private const val KEY_HEADING = "mHeading"

        /**
         * Key used to store our field `int mLanderHeight` (Pixel height of lander image)
         */
        private const val KEY_LANDER_HEIGHT = "mLanderHeight"

        /**
         * Key used to store our field `int mLanderWidth` (Pixel width of lander image)
         */
        private const val KEY_LANDER_WIDTH = "mLanderWidth"

        /**
         * Key used to store our field `int mWinsInARow` (Number of wins in a row)
         */
        private const val KEY_WINS = "mWinsInARow"

        /**
         * Key used to store our field `double mX` (X of lander center)
         */
        private const val KEY_X = "mX"

        /**
         * Key used to store our field `double mY` (Y of lander center)
         */
        private const val KEY_Y = "mY"
    }
}
