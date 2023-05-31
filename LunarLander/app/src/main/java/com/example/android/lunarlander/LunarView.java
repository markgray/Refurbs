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

package com.example.android.lunarlander;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;


/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * <p>
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class LunarView extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * TAG used for logging.
     */
    static final String TAG = "LunarView";

    /**
     * Our animation thread.
     */
    @SuppressWarnings("WeakerAccess")
    class LunarThread extends Thread {

        //Difficulty setting constants
        /**
         * Easy difficulty: our fuel is multiplied by 3/2, our goals are larger, and our speed is slower.
         */
        public static final int DIFFICULTY_EASY = 0;
        /**
         * Hard difficulty: our fuel is multiplied by 7/8, our goals are smaller, and our speed is faster.
         */
        public static final int DIFFICULTY_HARD = 1;
        /**
         * Medium difficulty, the default: fuel, goals and speed are the default values.
         */
        public static final int DIFFICULTY_MEDIUM = 2;

        // Physics constants
        /**
         * Downward acceleration due to gravity.
         */
        public static final int PHYS_DOWN_ACCEL_SEC = 35;
        /**
         * Acceleration due to the engine firing.
         */
        public static final int PHYS_FIRE_ACCEL_SEC = 80;
        /**
         * Initial amount of fuel available.
         */
        public static final int PHYS_FUEL_INIT = 60;
        /**
         * Maximum amount of fuel displayed by our "fuel gage".
         */
        public static final int PHYS_FUEL_MAX = 100;
        /**
         * Amount of fuel burned per second when the engine is firing.
         */
        public static final int PHYS_FUEL_SEC = 10;
        /**
         * Rotation speed in degrees/second.
         */
        public static final int PHYS_SLEW_SEC = 120; // degrees/second rotate
        /**
         * "Hyperspace" speed -- upside down, going faster than this puts you back at the top.
         */
        public static final int PHYS_SPEED_HYPERSPACE = 180;
        /**
         * Initial speed when starting.
         */
        public static final int PHYS_SPEED_INIT = 30;
        /**
         * Maximum speed for our speed gauge.
         */
        public static final int PHYS_SPEED_MAX = 120;

        // State-tracking constants
        /**
         * The player has lost the game.
         */
        public static final int STATE_LOSE = 1;
        /**
         * The game is paused.
         */
        public static final int STATE_PAUSE = 2;
        /**
         * The game is ready to start running.
         */
        public static final int STATE_READY = 3;
        /**
         * The game is running.
         */
        public static final int STATE_RUNNING = 4;
        /**
         * The player has won the game.
         */
        public static final int STATE_WIN = 5;

        // Goal condition constants
        /**
         * Any angle greater than this is a "Crash".
         */
        public static final int TARGET_ANGLE = 18; // > this angle means crash
        /**
         * Number of pixels below the landing gear, used to determine if we have landed.
         */
        public static final int TARGET_BOTTOM_PADDING = 17; // px below gear
        /**
         * How high are we above the ground, used to determine if we have landed.
         */
        public static final int TARGET_PAD_HEIGHT = 8; // how high above ground
        /**
         * Target speed, any speed greater than this when landed is a "crash".
         */
        public static final int TARGET_SPEED = 28; // > this speed means crash
        /**
         * Width of our landing target.
         */
        public static final double TARGET_WIDTH = 1.6; // width of target

        // UI constants (i.e. the speed & fuel bars)
        /**
         * Width of the bar(s) in the X direction.
         */
        public static final int UI_BAR = 100; // width of the bar(s)
        /**
         * Height of the bar(s) in the Y direction.
         */
        public static final int UI_BAR_HEIGHT = 10; // height of the bar(s)

        // Keys used for storing our state in the Bundle passed saveState, later restored in restoreState
        /**
         * Key used to store our field {@code int mDifficulty} (level of difficulty)
         */
        private static final String KEY_DIFFICULTY = "mDifficulty";

        /**
         * Key used to store our field {@code double mDX} (Velocity in the X direction)
         */
        private static final String KEY_DX = "mDX";
        /**
         * Key used to store our field {@code double mDY} (Velocity in the Y direction)
         */
        private static final String KEY_DY = "mDY";
        /**
         * Key used to store our field {@code double mFuel} (Fuel remaining)
         */
        private static final String KEY_FUEL = "mFuel";

        /**
         * Key used to store our field {@code int mGoalAngle} (Goal for the lander heading)
         */
        private static final String KEY_GOAL_ANGLE = "mGoalAngle";
        /**
         * Key used to store our field {@code int mGoalSpeed} (Goal for the speed when landed)
         */
        private static final String KEY_GOAL_SPEED = "mGoalSpeed";
        /**
         * Key used to store our field {@code int mGoalWidth} (Width of the landing pad)
         */
        private static final String KEY_GOAL_WIDTH = "mGoalWidth";
        /**
         * Key used to store our field {@code int mGoalX} (X of the landing pad)
         */
        private static final String KEY_GOAL_X = "mGoalX";

        /**
         * Key used to store our field {@code double mHeading} (Lander heading in degrees)
         */
        private static final String KEY_HEADING = "mHeading";
        /**
         * Key used to store our field {@code int mLanderHeight} (Pixel height of lander image)
         */
        private static final String KEY_LANDER_HEIGHT = "mLanderHeight";
        /**
         * Key used to store our field {@code int mLanderWidth} (Pixel width of lander image)
         */
        private static final String KEY_LANDER_WIDTH = "mLanderWidth";
        /**
         * Key used to store our field {@code int mWinsInARow} (Number of wins in a row)
         */
        private static final String KEY_WINS = "mWinsInARow";

        /**
         * Key used to store our field {@code double mX} (X of lander center)
         */
        private static final String KEY_X = "mX";
        /**
         * Key used to store our field {@code double mY} (Y of lander center)
         */
        private static final String KEY_Y = "mY";

        // Member (state) fields
        /**
         * The drawable to use as the background of the animation canvas
         */
        private Bitmap mBackgroundImage;

        /**
         * Current height of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasHeight = 1;

        /**
         * Current width of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasWidth = 1;

        /**
         * What to draw for the Lander when it has crashed
         */
        private Drawable mCrashedImage;

        /**
         * Current difficulty -- amount of fuel, allowed angle, etc. Default is MEDIUM.
         */
        private int mDifficulty;

        /**
         * Velocity dx.
         */
        private double mDX;

        /**
         * Velocity dy.
         */
        private double mDY;

        /**
         * Is the engine burning?
         */
        private boolean mEngineFiring;

        /**
         * What to draw for the Lander when the engine is firing
         */
        private Drawable mFiringImage;

        /**
         * Fuel remaining
         */
        private double mFuel;

        /**
         * Allowed angle.
         */
        private int mGoalAngle;

        /**
         * Allowed speed.
         */
        private int mGoalSpeed;

        /**
         * Width of the landing pad.
         */
        private int mGoalWidth;

        /**
         * X of the landing pad.
         */
        private int mGoalX;

        /**
         * Message handler used by thread to interact with TextView
         */
        private Handler mHandler;

        /**
         * Lander heading in degrees, with 0 up, 90 right. Kept in the range 0..360.
         */
        private double mHeading;

        /**
         * Pixel height of lander image.
         */
        private int mLanderHeight;

        /**
         * What to draw for the Lander in its normal state
         */
        private Drawable mLanderImage;

        /**
         * Pixel width of lander image.
         */
        private int mLanderWidth;

        /**
         * Used to figure out elapsed time between frames
         */
        private long mLastTime;

        /**
         * Paint to draw the lines on screen.
         */
        private Paint mLinePaint;

        /**
         * "Bad" speed-too-high variant of the line color.
         */
        private Paint mLinePaintBad;

        /**
         * The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN
         */
        public int mMode;

        /**
         * Currently rotating, -1 left, 0 none, 1 right.
         */
        private int mRotating;

        /**
         * Indicate whether the surface has been created & is ready to draw
         */
        private boolean mRun = false;

        /**
         * Lock to synchronise access to the {@code boolean mRun} field.
         */
        private final Object mRunLock = new Object();

        /**
         * Scratch rect object.
         */
        private RectF mScratchRect;

        /**
         * Handle to the surface manager object we interact with
         */
        private final SurfaceHolder mSurfaceHolder;

        /**
         * Number of wins in a row.
         */
        private int mWinsInARow;

        /**
         * X of lander center.
         */
        private double mX;

        /**
         * Y of lander center.
         */
        private double mY;

        /**
         * Our constructor. First we save our three parameters {@code SurfaceHolder surfaceHolder},
         * {@code Context context}, and {@code Handler handler} in our fields {@code mSurfaceHolder},
         * {@code mContext}, and {@code mHandler} respectively. Then we initialize {@code Resources res}
         * with a {@code Resources} instance for the application's package and use it to load the png
         * with resource id R.drawable.lander_plain into our field {@code Drawable mLanderImage}, the
         * png with resource id R.drawable.lander_firing into our field {@code Drawable mFiringImage},
         * and the png with resource id R.drawable.lander_crashed into our field {@code Drawable mCrashedImage}.
         * We then load our field {@code Bitmap mBackgroundImage} by decoding the png with resource id
         * R.drawable.earthrise. We initialize our field {@code int mLanderWidth} with the width of
         * {@code mLanderImage} and our field {@code int mLanderHeight} with its height. We initialize
         * {@code Paint mLinePaint} with a new instance, enable its anti-alias flag, and set its color
         * to green. We initialize {@code Paint mLinePaintBad} with a new instance, enable its anti-alias flag,
         * and set its color to a greenish red. We set our field {@code int mWinsInARow} to 0, and set
         * our field {@code int mDifficulty} to DIFFICULTY_MEDIUM. We initialize {@code double mX} to
         * {@code mLanderWidth}, {@code double mY} to 2 times {@code mLanderHeight}, {@code double mFuel}
         * to PHYS_FUEL_INIT, {@code double mDX} to 0, {@code double mDY} to 0, {@code double mHeading}
         * to 0, and {@code boolean mEngineFiring} to true.
         *
         * @param surfaceHolder {@code SurfaceHolder} for the {@code SurfaceView} of the {@code LunarView}
         *                      which is constructing us.
         * @param context       {@code Context} the {@code LunarView} is running in, used to access resources.
         * @param handler       {@code Handler} we can send messages to in order to interact with the
         *                      status {@code TextView}
         */
        public LunarThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            mHandler = handler;

            Resources res = context.getResources();
            // cache handles to our key sprites & other drawables
            mLanderImage = res.getDrawable(R.drawable.lander_plain);
            mFiringImage = res.getDrawable(R.drawable.lander_firing);
            mCrashedImage = res.getDrawable(R.drawable.lander_crashed);

            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this way
            mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.earthrise);

            // Use the regular lander image as the model size for all sprites
            mLanderWidth = mLanderImage.getIntrinsicWidth();
            mLanderHeight = mLanderImage.getIntrinsicHeight();

            // Initialize paints for speedometer
            mLinePaint = new Paint();
            mLinePaint.setAntiAlias(true);
            mLinePaint.setARGB(255, 0, 255, 0);

            mLinePaintBad = new Paint();
            mLinePaintBad.setAntiAlias(true);
            mLinePaintBad.setARGB(255, 120, 180, 0);

            mScratchRect = new RectF(0, 0, 0, 0);

            mWinsInARow = 0;
            mDifficulty = DIFFICULTY_MEDIUM;

            // initial show-up of lander (not yet playing)
            mX = mLanderWidth;
            mY = mLanderHeight * 2;
            mFuel = PHYS_FUEL_INIT;
            mDX = 0;
            mDY = 0;
            mHeading = 0;
            mEngineFiring = true;
        }

        /**
         * Starts the game, setting parameters for the current difficulty. In a block that is synchronized
         * on our field {@code SurfaceHolder mSurfaceHolder} we:
         * <ul>
         *     <li>
         *         Set our field {@code double mFuel} to PHYS_FUEL_INIT, set our field {@code boolean mEngineFiring}
         *         to false, set our field {@code int mGoalWidth} to {@code mLanderWidth} times TARGET_WIDTH,
         *         set our field {@code int mGoalSpeed} to TARGET_SPEED, set our field {@code int mGoalAngle}
         *         to TARGET_ANGLE, and initialize our variable {@code int speedInit} to PHYS_SPEED_INIT
         *         (these are all the values used for DIFFICULTY_MEDIUM). Now we branch on the value of
         *         our field {@code int mDifficulty}:
         *         <ul>
         *             <li>
         *                 DIFFICULTY_EASY: we multiply {@code mFuel} by 3/2, multiply {@code mGoalWidth} by
         *                 4/3, multiply {@code mGoalSpeed} by by 3/2, multiply {@code mGoalAngle} by 4/3,
         *                 and multiply {@code speedInit} by 3/4
         *             </li>
         *             <li>
         *                 DIFFICULTY_HARD: we multiply {@code mFuel} by 7/8, multiply {@code mGoalWidth} by
         *                 3/4, multiply {@code mGoalSpeed} by by 7/8, and multiply {@code speedInit} by 4/3
         *             </li>
         *         </ul>
         *     </li>
         *     <li>
         *         We initialize our field {@code double mX} to half of our field {@code int mCanvasWidth},
         *         and initialize our field {@code double mY} to our field {@code int mCanvasHeight} minus
         *         half of our field {@code int mLanderHeight} (this is the initial location for the lander
         *         sprite).
         *     </li>
         *     <li>
         *         We now start the rocket ship moving with a little random motion by setting our field
         *         {@code double mDY} (the velocity in the Y direction) to a random number between 0
         *         and 1.0 times minus {@code speedInit}, {@code double mDX} (the velocity in the X direction)
         *         to a random number between 0 and 1.0 times 2 times {@code speedInit} and subtract
         *         {@code speedInit} from that value, and we set our field {@code double mHeading} to 0.
         *     </li>
         *     <li>
         *         Then we loop setting our field {@code int mGoalX} to a random number between 0 and
         *         1.0 times the quantity {@code mCanvasWidth} minus {@code mGoalWidth} until this value
         *         is farther than {@code mCanvasHeight} divided by 6 from the center.
         *     </li>
         *     <li>
         *         Finally we initialize our field {@code long mLastTime} to the current system time in
         *         milliseconds plus 100, and call our {@code setState} method to set the game state to
         *         STATE_RUNNING.
         *     </li>
         * </ul>
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                // First set the game for Medium difficulty
                mFuel = PHYS_FUEL_INIT;
                mEngineFiring = false;
                mGoalWidth = (int) (mLanderWidth * TARGET_WIDTH);
                mGoalSpeed = TARGET_SPEED;
                mGoalAngle = TARGET_ANGLE;
                int speedInit = PHYS_SPEED_INIT;

                // Adjust difficulty params for EASY/HARD
                if (mDifficulty == DIFFICULTY_EASY) {
                    mFuel = mFuel * 3 / 2;
                    mGoalWidth = mGoalWidth * 4 / 3;
                    mGoalSpeed = mGoalSpeed * 3 / 2;
                    mGoalAngle = mGoalAngle * 4 / 3;
                    speedInit = speedInit * 3 / 4;
                } else if (mDifficulty == DIFFICULTY_HARD) {
                    mFuel = mFuel * 7 / 8;
                    mGoalWidth = mGoalWidth * 3 / 4;
                    mGoalSpeed = mGoalSpeed * 7 / 8;
                    speedInit = speedInit * 4 / 3;
                }

                // pick a convenient initial location for the lander sprite
                mX = mCanvasWidth >> 1;
                mY = mCanvasHeight - (mLanderHeight >> 1);

                // start with a little random motion
                mDY = Math.random() * -speedInit;
                mDX = Math.random() * 2 * speedInit - speedInit;
                mHeading = 0;

                // Figure initial spot for landing, not too near center
                do {
                    mGoalX = (int) (Math.random() * (mCanvasWidth - mGoalWidth));
                } while (!(Math.abs(mGoalX - (mX - (mLanderWidth >> 1))) > mCanvasHeight / 6));

                mLastTime = System.currentTimeMillis() + 100;
                setState(STATE_RUNNING);
            }
        }

        /**
         * Pauses the physics update & animation. In a block that is synchronized on our field
         * {@code SurfaceHolder mSurfaceHolder} if our current game state {@code int mMode} is
         * int mMode we call our {@code setState} method to set the game state to STATE_PAUSE.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when the Activity is being
         * restored after having been previously destroyed. In a block that is synchronized on our field
         * {@code SurfaceHolder mSurfaceHolder} we:
         * <ul>
         *     <li>
         *         Call our {@code setState} method to set the game state to STATE_PAUSE.
         *     </li>
         *     <li>
         *         Set our field {@code int mRotating} to 0, and our field {@code boolean mEngineFiring}
         *         to false.
         *     </li>
         *     <li>
         *         Load our field {@code int mDifficulty} with the value stored under the key KEY_DIFFICULTY in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code double mX} with the value stored under the key KEY_X in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code double mY} with the value stored under the key KEY_Y in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code double mDX} with the value stored under the key KEY_DX in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code double mDY} with the value stored under the key KEY_DY in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code double mHeading} with the value stored under the key KEY_HEADING in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code int mLanderWidth} with the value stored under the key KEY_LANDER_WIDTH in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code int mLanderHeight} with the value stored under the key KEY_LANDER_HEIGHT in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code int mGoalX} with the value stored under the key KEY_GOAL_X in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code int mGoalSpeed} with the value stored under the key KEY_GOAL_SPEED in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code int mGoalWidth} with the value stored under the key KEY_GOAL_WIDTH in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         Load our field {@code int mWinsInARow} with the value stored under the key KEY_WINS in our parameter {@code Bundle savedState}
         *     </li>
         *     <li>
         *         And Load our field {@code double mFuel} with the value stored under the key KEY_FUEL in our parameter {@code Bundle savedState}
         *     </li>
         * </ul>
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
                setState(STATE_PAUSE);
                mRotating = 0;
                mEngineFiring = false;

                mDifficulty = savedState.getInt(KEY_DIFFICULTY);
                mX = savedState.getDouble(KEY_X);
                mY = savedState.getDouble(KEY_Y);
                mDX = savedState.getDouble(KEY_DX);
                mDY = savedState.getDouble(KEY_DY);
                mHeading = savedState.getDouble(KEY_HEADING);

                mLanderWidth = savedState.getInt(KEY_LANDER_WIDTH);
                mLanderHeight = savedState.getInt(KEY_LANDER_HEIGHT);
                mGoalX = savedState.getInt(KEY_GOAL_X);
                mGoalSpeed = savedState.getInt(KEY_GOAL_SPEED);
                mGoalAngle = savedState.getInt(KEY_GOAL_ANGLE);
                mGoalWidth = savedState.getInt(KEY_GOAL_WIDTH);
                mWinsInARow = savedState.getInt(KEY_WINS);
                mFuel = savedState.getDouble(KEY_FUEL);
            }
        }

        /**
         * Called when our {@code start} method is called in our {@code surfaceCreated} override, this
         * method calls our {@code updatePhysics} method to update the position, speed and fuel of the
         * rocket if the game is in the STATE_RUNNING state, it then calls our {@code doDraw} method
         * whether we are running or not to draw to the surface. To do all this we loop as long as
         * our field {@code boolean mRun} is true:
         * <ul>
         *     <li>
         *         First we set our variable {@code Canvas c} to null.
         *     </li>
         *     <li>
         *         Then wrapped in a try block whose finally block calls the {@code unlockCanvasAndPost}
         *         method of our field {@code SurfaceHolder mSurfaceHolder} with {@code c} if it is not null
         *         to make sure our Surface is not left in an inconsistent state we ...
         *         <ul>
         *             <li>
         *                 Set {@code c} to the {@code Canvas} returned when the {@code lockCanvas} method
         *                 of {@code mSurfaceHolder} is called to start editing the pixels in the surface
         *                 ({@code c} can then be used to draw on the surface).
         *             </li>
         *             <li>
         *                 In a block synchronized on {@code mSurfaceHolder} if our field {@code mMode}
         *                 is in STATE_RUNNING (the game is running) we call our {@code updatePhysics}
         *                 method to update the physics of the game, and then in a block synchronized
         *                 on our field {@code mRunLock} if our field {@code mRun} is still true we
         *                 call our method {@code doDraw} to draw the game to {@code Canvas c}.
         *             </li>
         *         </ul>
         *     </li>
         * </ul>
         */
        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized (mRunLock) {
                            if (mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the Activity is being suspended.
         * In a block synchronized on our field {@code SurfaceHolder mSurfaceHolder} we:
         * <ul>
         *     <li>
         *         First make sure that our parameter {@code Bundle map} is not null, doing nothing more if it is.
         *     </li>
         *     <li>
         *         We store our field {@code int mDifficulty} under the key KEY_DIFFICULTY in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code double mX} under the key KEY_X in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code double mY} under the key KEY_Y in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code double mDX} under the key KEY_DX in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code double mDY} under the key KEY_DY in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code double mHeading} under the key KEY_HEADING in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mLanderWidth} under the key KEY_LANDER_WIDTH in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mLanderHeight} under the key KEY_LANDER_HEIGHT in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mGoalX} under the key KEY_GOAL_X in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mGoalSpeed} under the key KEY_GOAL_SPEED in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mGoalAngle} under the key KEY_GOAL_ANGLE in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mGoalWidth} under the key KEY_GOAL_WIDTH in {@code map}
         *     </li>
         *     <li>
         *         We store our field {@code int mWinsInARow} under the key KEY_WINS in {@code map}
         *     </li>
         *     <li>
         *         and we store our field {@code double mFuel} under the key KEY_FUEL in {@code map}
         *     </li>
         * </ul>
         *
         * @param map {@code Bundle} to store the game state variables in
         */
        public void saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    map.putInt(KEY_DIFFICULTY, mDifficulty);
                    map.putDouble(KEY_X, mX);
                    map.putDouble(KEY_Y, mY);
                    map.putDouble(KEY_DX, mDX);
                    map.putDouble(KEY_DY, mDY);
                    map.putDouble(KEY_HEADING, mHeading);
                    map.putInt(KEY_LANDER_WIDTH, mLanderWidth);
                    map.putInt(KEY_LANDER_HEIGHT, mLanderHeight);
                    map.putInt(KEY_GOAL_X, mGoalX);
                    map.putInt(KEY_GOAL_SPEED, mGoalSpeed);
                    map.putInt(KEY_GOAL_ANGLE, mGoalAngle);
                    map.putInt(KEY_GOAL_WIDTH, mGoalWidth);
                    map.putInt(KEY_WINS, mWinsInARow);
                    map.putDouble(KEY_FUEL, mFuel);
                }
            }
        }

        /**
         * Sets the current difficulty. In a block synchronized on our field {@code SurfaceHolder mSurfaceHolder}
         * we set our field {@code int mDifficulty} to our parameter {@code difficulty}.
         *
         * @param difficulty new difficulty to use.
         */
        public void setDifficulty(int difficulty) {
            synchronized (mSurfaceHolder) {
                mDifficulty = difficulty;
            }
        }

        /**
         * Sets if the engine is currently firing. In a block synchronized on our field
         * {@code SurfaceHolder mSurfaceHolder} we set our field {@code boolean mEngineFiring}
         * to our parameter {@code firing}.
         *
         * @param firing true to start the rocket firing, false to stop it.
         */
        public void setFiring(boolean firing) {
            synchronized (mSurfaceHolder) {
                mEngineFiring = firing;
            }
        }

        /**
         * Used to signal the thread whether it should be running or not. Passing true allows the
         * thread to run; passing false will shut it down if it's already running. Calling start()
         * after this was most recently called with false will result in an immediate shutdown.
         * In a block synchronized on our field {@code Object mRunLock} we set our field
         * {@code boolean mRun} to our parameter {@code b}.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized (mRunLock) {
                mRun = b;
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the failure state, in the
         * victory state, etc. In a block synchronized on our field {@code SurfaceHolder mSurfaceHolder}
         * we call our method {@code setState(int, CharSequence)} with null for the second parameter.
         *
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the failure state, in the
         * victory state, etc. In a block synchronized on our field {@code SurfaceHolder mSurfaceHolder}
         * we set our field {@code int mMode} to our parameter {@code int mode}. We then branch on the
         * value of this new mode:
         * <ul>
         *     <li>
         *         STATE_RUNNING: We initialize our variable {@code Message msg} with an instance from
         *         the global message pool initialized to target {@code Handler mHandler}. We then
         *         initialize our variable {@code Bundle b} with a new instance, store an empty string
         *         in it under the key "text", store the int INVISIBLE under the key "viz", and set the
         *         data of {@code msg} to {@code b}. We then push {@code msg} onto the end of the message
         *         queue of {@code mHandler} after all pending messages before the current time (It will
         *         be received in {@code handleMessage}, in the thread attached to this handler).
         *     </li>
         *     <li>
         *         Any other state: We set our field {@code int mRotating} to 0 (stop rotating), set our
         *         field {@code boolean mEngineFiring} to false (stop the rocket engine), initialize our
         *         variable {@code Resources res} with a Resources instance for the application's package,
         *         and initialize {@code CharSequence str} to the empty string. We then set the value
         *         of {@code str} based on the value of our game state {@code mMode}:
         *         <ul>
         *             <li>
         *                 STATE_READY: We set {@code str} to the string with resource id R.string.mode_ready
         *             </li>
         *             <li>
         *                 STATE_PAUSE: We set {@code str} to the string with resource id R.string.mode_pause
         *             </li>
         *             <li>
         *                 STATE_LOSE: We set {@code str} to the string with resource id R.string.mode_lose
         *             </li>
         *             <li>
         *                 STATE_WIN: We set {@code str} to a string formed by concatenating the string
         *                 with resource id R.string.mode_win_prefix followed by the string value of our
         *                 field {@code int mWinsInARow} followed by the string with resource id
         *                 R.string.mode_win_suffix
         *             </li>
         *         </ul>
         *         <li>
         *             If our parameter {@code CharSequence message} is not null we set {@code str} to the
         *             string created by concatenating {@code message} followed by a newline character,
         *             followed by {@code str}.
         *         </li>
         *         <li>
         *             If the new value of our field {@code mMode} is STATE_LOSE, we set our field
         *             {@code mWinsInARow} to 0.
         *         </li>
         *         <li>
         *             We initialize our variable {@code Message msg} with an instance from the global
         *             message pool initialized to target {@code Handler mHandler}. We then initialize
         *             our variable {@code Bundle b} with a new instance, store {@code str} in it under
         *             the key "text", store the int VISIBLE under the key "viz", and set the data of
         *             {@code msg} to {@code b}
         *         </li>
         *         <li>
         *             We then push {@code msg} onto the end of the message queue of {@code mHandler}
         *             after all pending messages before the current time (It will be received in
         *             {@code handleMessage}, in the thread attached to this handler).
         *         </li>
         *     </li>
         * </ul>
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized (mSurfaceHolder) {
                mMode = mode;

                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
                    mRotating = 0;
                    mEngineFiring = false;
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    if (mMode == STATE_READY)
                        str = res.getText(R.string.mode_ready);
                    else if (mMode == STATE_PAUSE)
                        str = res.getText(R.string.mode_pause);
                    else if (mMode == STATE_LOSE)
                        str = res.getText(R.string.mode_lose);
                    else if (mMode == STATE_WIN)
                        str = res.getString(R.string.mode_win_prefix)
                                + mWinsInARow + " "
                                + res.getString(R.string.mode_win_suffix);

                    if (message != null) {
                        str = message + "\n" + str;
                    }

                    if (mMode == STATE_LOSE) mWinsInARow = 0;

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }

        /**
         * Callback invoked when the surface dimensions change. In a block synchronized on our field
         * {@code SurfaceHolder mSurfaceHolder} we store our parameter {@code int width} in our field
         * {@code int mCanvasWidth}, and our parameter {@code int height} in our field {@code int mCanvasHeight}.
         * Finally we resize our field {@code Bitmap mBackgroundImage} to be {@code width} by {@code height}.
         *
         * @param width  new width of our surface
         * @param height new height of our surface
         */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

                // don't forget to resize the background image
                mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);
            }
        }

        /**
         * Resumes from a pause. In a block synchronized on our field {@code SurfaceHolder mSurfaceHolder}
         * we set our field {@code long mLastTime} to the current system time in milliseconds plus 100.
         * After exiting the synchronized block we call our method {@code setState} to set the game state
         * to STATE_RUNNING.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
            setState(STATE_RUNNING);
        }

        /**
         * Handles a key-down event. In a block synchronized on our field {@code SurfaceHolder mSurfaceHolder}
         * we initialize our variable {@code boolean okStart} to false. Then we set it to true if the
         * value of our parameter {@code int keyCode} is KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN, or KEYCODE_S.
         * We then branch based on the following conditions:
         * <ul>
         *     <li>
         *         {@code okStart} is true and our field {@code mMode} is STATE_READY, STATE_LOSE, or
         *         STATE_WIN: (ready-to-start -> start) we call our method {@code doStart} and return
         *         true to consume the event.
         *     </li>
         *     <li>
         *         Our field {@code mMode} is STATE_PAUSE and {@code okStart} is true: (paused -> running)
         *         we call our method {@code unpause} to resume the paused game and return true to
         *         consume the event.
         *     </li>
         *     <li>
         *         Our field {@code mMode} is STATE_RUNNING: (game is running) We branch on the value of
         *         our parameter {@code keyCode}:
         *         <ul>
         *             <li>
         *                 KEYCODE_DPAD_CENTER or KEYCODE_SPACE: (center/space -> fire) We call our
         *                 method {@code setFiring(true)} to start the rocket engine and return true
         *                 to consume the event.
         *             </li>
         *             <li>
         *                 KEYCODE_DPAD_LEFT or KEYCODE_Q: (left/q -> left) We set our field {@code int mRotating}
         *                 to -1 and return true to consume the event.
         *             </li>
         *             <li>
         *                 KEYCODE_DPAD_RIGHT or KEYCODE_W: (right/w -> right) We set our field {@code int mRotating}
         *                 to 1 and return true to consume the event.
         *             </li>
         *             <li>
         *                 KEYCODE_DPAD_UP: (up -> pause) We call our method {@code pause} to pause the game
         *                 and return true to consume the event.
         *             </li>
         *         </ul>
         *     </li>
         * </ul>
         * If the parameter {@code keyCode} is not one we are interested in at this point in the game
         * we return false to the caller to allow the event to be handled by the next receiver.
         *
         * @param keyCode the key that was pressed
         * @param msg     the original event object
         * @return true if we consumed the event, false if the key is not one we use.
         */
        @SuppressWarnings("unused") // parameter msg is unused
        boolean doKeyDown(int keyCode, KeyEvent msg) {
            synchronized (mSurfaceHolder) {
                boolean okStart = false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) okStart = true;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true;
                if (keyCode == KeyEvent.KEYCODE_S) okStart = true;

                if (okStart
                        && (mMode == STATE_READY || mMode == STATE_LOSE || mMode == STATE_WIN)) {
                    // ready-to-start -> start
                    doStart();
                    return true;
                } else if (mMode == STATE_PAUSE && okStart) {
                    // paused -> running
                    unpause();
                    return true;
                } else if (mMode == STATE_RUNNING) {
                    // center/space -> fire
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_SPACE) {
                        setFiring(true);
                        return true;
                        // left/q -> left
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            || keyCode == KeyEvent.KEYCODE_Q) {
                        mRotating = -1;
                        return true;
                        // right/w -> right
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                            || keyCode == KeyEvent.KEYCODE_W) {
                        mRotating = 1;
                        return true;
                        // up -> pause
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        pause();
                        return true;
                    }
                }

                return false;
            }
        }

        /**
         * Handles a key-up event. First we set our variable {@code boolean handled} to false, then
         * in a block synchronized on our field {@code SurfaceHolder mSurfaceHolder} if our field
         * {@code int mMode} is STATE_RUNNING (game is running) we branch on the value of our parameter
         * {@code int keyCode}:
         * <ul>
         *     <li>
         *         KEYCODE_DPAD_CENTER or KEYCODE_SPACE: We call our method {@code setFiring(false)} to
         *         stop the rocket engine firing, and set {@code handled} to true.
         *     </li>
         *     <li>
         *         KEYCODE_DPAD_LEFT, KEYCODE_Q, KEYCODE_DPAD_RIGHT or KEYCODE_W: We set our field
         *         {@code int mRotating} to 0, and set {@code handled} to true.
         *     </li>
         * </ul>
         * Having exited the synchronized block we return {@code handled} to the caller.
         *
         * @param keyCode the key that was pressed
         * @param msg     the original event object
         * @return true if the key was handled and consumed, or else false
         */
        @SuppressWarnings("unused") // parameter msg is unused
        boolean doKeyUp(int keyCode, KeyEvent msg) {
            boolean handled = false;

            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_SPACE) {
                        setFiring(false);
                        handled = true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            || keyCode == KeyEvent.KEYCODE_Q
                            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                            || keyCode == KeyEvent.KEYCODE_W) {
                        mRotating = 0;
                        handled = true;
                    }
                }
            }

            return handled;
        }

        /**
         * Draws the ship, fuel/speed bars, and background to the provided Canvas. First we have our
         * parameter {@code Canvas canvas} draw our background image {@code Bitmap mBackgroundImage}
         * on itself at (0,0). Then we initialize our variable {@code int yTop} (the top Y coordinate
         * for the drawing of the rocket ship) to our field {@code mCanvasHeight} minus the quantity
         * {@code mY} (the Y location of the ship) plus half of the lander height {@code mLanderHeight}.
         * We we initialize our variable {@code int xLeft} (the left X coordinate for the drawing of
         * the rocket ship) to our field {@code mX} (the X location of the ship) minus half of the width
         * of the ship {@code mLanderWidth}. We then draw the fuel gauge by initializing our variable
         * {@code int fuelWidth} to the width of the bar given by UI_BAR times the fraction of fuel
         * remaining ({@code mFuel} divided PHYS_FUEL_MAX). We set our scratch rectangle {@code RectF mScratchRect}
         * to have its left top corner at (4,4), and its right bottom corner at 4 plus {@code fuelWidth}
         * for the X coordinate and 4 plus UI_BAR_HEIGHT for the Y coordinate, then have {@code canvas}
         * draw {@code mScratchRect} on itself using {@code mLinePaint} as the {@code Paint}. We next
         * want to draw the speed gauge with a two-tone effect and to do this we initialize {@code double speed}
         * to the square root of {@code mDX} squared plus {@code mDY} squared (the length of the speed
         * vector given the X velocity and Y velocity), then initialize {@code int speedWidth} to {@code speed}
         * divided by PHYS_SPEED_MAX times the size of our UI bar UI_BAR. If {@code speed} is less than
         * or equal to {@code mGoalSpeed} we set the rectangle {@code mScratchRect} to have its upper
         * left corner at 4 plus UI_BAR plus 4 for the X coordinate, 4 for the Y coordinate and its
         * bottom right corner at 4 plus UI_BAR plus 4 plus {@code speedWidth} for the X coordinate
         * and 4 plus UI_BAR_HEIGHT for the Y coordinate. We then have {@code canvas} draw {@code mScratchRect}
         * on itself using {@code mLinePaint} as the paint. If on the other hand {@code speed} is greater
         * than {@code mGoalSpeed} we want to draw the bad color in back, with the good color in front
         * of it, so we set the rectangle {@code mScratchRect} to have its upper left corner at 4 plus
         * UI_BAR plus 4 for the X coordinate, 4 for the Y coordinate and its bottom right corner at 4
         * plus UI_BAR plus 4 plus {@code speedWidth} for the X coordinate and 4 plus UI_BAR_HEIGHT for
         * the Y coordinate. We then have {@code canvas} draw {@code mScratchRect} on itself using
         * {@code mLinePaintBad} as the paint. We then initialize {@code int goalWidth} to be {@code mGoalSpeed}
         * divided by {@code PHYS_SPEED_MAX} times UI_BAR, and set the rectangle {@code mScratchRect} to
         * have its upper left corner at 4 plus UI_BAR plus 4 for the X coordinate, 4 for the Y coordinate and its
         * bottom right corner at 4 plus UI_BAR plus 4 plus {@code goalWidth} for the X coordinate and 4 plus
         * UI_BAR_HEIGHT for the Y coordinate. We then have {@code canvas} draw {@code mScratchRect} on itself
         * using {@code mLinePaint} as the paint.
         * <p>
         * We now want to draw the landing pad, so we have {@code canvas} draw a line on itself from
         * ({@code mGoalX},1+{@code mGoalX}-TARGET_PAD_HEIGHT) to
         * ({@code mGoalX}+{@code mGoalWidth},1+{@code mCanvasHeight}-TARGET_PAD_HEIGHT) using
         * {@code mLinePaint} as the paint.
         * <p>
         * Now we want to draw the ship with its current rotation so we have {@code canvas} save the
         * current matrix and clip onto a private stack, then rotate {@code canvas} by {@code mHeading}
         * degrees around the point ({@code mX},{@code mCanvasHeight}-{@code mY}). If the game state
         * {@code mMode} is STATE_LOSE we set the bounding rectangle of {@code mCrashedImage} to a
         * rectangle whose left top corner is ({@code xLeft},{@code yTop}) and whose right bottom corner
         * is ({@code xLeft}+{@code mLanderWidth},{@code yTop}+{@code mLanderHeight}). We then have
         * {@code mCrashedImage} draw itself on {@code canvas}. If on the other hand {@code mEngineFiring}
         * is firing we set the bounding rectangle of {@code mFiringImage} to a rectangle whose left
         * top corner is ({@code xLeft},{@code yTop}) and whose right bottom corner is
         * ({@code xLeft}+{@code mLanderWidth},{@code yTop}+{@code mLanderHeight}). We then have
         * {@code mFiringImage} draw itself on {@code canvas}. If neither of these are true we set the
         * bounding rectangle of {@code mLanderImage} to a rectangle whose left top corner is
         * ({@code xLeft},{@code yTop}) and whose right bottom corner is
         * ({@code xLeft}+{@code mLanderWidth},{@code yTop}+{@code mLanderHeight}). We then have
         * {@code mLanderImage} draw itself on {@code canvas}.
         * <p>
         * Finally we have {@code canvas} restore its state to the values it placed on its stack.
         *
         * @param canvas {@code Canvas} we are to draw to.
         */
        private void doDraw(Canvas canvas) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawBitmap(mBackgroundImage, 0, 0, null);

            int yTop = mCanvasHeight - ((int) mY + mLanderHeight / 2);
            int xLeft = (int) mX - mLanderWidth / 2;

            // Draw the fuel gauge
            int fuelWidth = (int) (UI_BAR * mFuel / PHYS_FUEL_MAX);
            mScratchRect.set(4, 4, 4 + fuelWidth, 4 + UI_BAR_HEIGHT);
            canvas.drawRect(mScratchRect, mLinePaint);

            // Draw the speed gauge, with a two-tone effect
            double speed = Math.hypot(mDX, mDY);
            int speedWidth = (int) (UI_BAR * speed / PHYS_SPEED_MAX);

            if (speed <= mGoalSpeed) {
                mScratchRect.set(4 + UI_BAR + 4, 4,
                        4 + UI_BAR + 4 + speedWidth, 4 + UI_BAR_HEIGHT);
                canvas.drawRect(mScratchRect, mLinePaint);
            } else {
                // Draw the bad color in back, with the good color in front of
                // it
                mScratchRect.set(4 + UI_BAR + 4, 4,
                        4 + UI_BAR + 4 + speedWidth, 4 + UI_BAR_HEIGHT);
                canvas.drawRect(mScratchRect, mLinePaintBad);
                int goalWidth = (UI_BAR * mGoalSpeed / PHYS_SPEED_MAX);
                mScratchRect.set(4 + UI_BAR + 4, 4, 4 + UI_BAR + 4 + goalWidth,
                        4 + UI_BAR_HEIGHT);
                canvas.drawRect(mScratchRect, mLinePaint);
            }

            // Draw the landing pad
            canvas.drawLine(mGoalX, 1 + mCanvasHeight - TARGET_PAD_HEIGHT,
                    mGoalX + mGoalWidth, 1 + mCanvasHeight - TARGET_PAD_HEIGHT,
                    mLinePaint);


            // Draw the ship with its current rotation
            canvas.save();
            canvas.rotate((float) mHeading, (float) mX, mCanvasHeight
                    - (float) mY);
            if (mMode == STATE_LOSE) {
                mCrashedImage.setBounds(xLeft, yTop,
                        xLeft + mLanderWidth, yTop + mLanderHeight);
                mCrashedImage.draw(canvas);
            } else if (mEngineFiring) {
                mFiringImage.setBounds(xLeft, yTop,
                        xLeft + mLanderWidth, yTop + mLanderHeight);
                mFiringImage.draw(canvas);
            } else {
                mLanderImage.setBounds(xLeft, yTop,
                        xLeft + mLanderWidth, yTop + mLanderHeight);
                mLanderImage.draw(canvas);
            }
            canvas.restore();
        }

        /**
         * Figures the lander state (x, y, fuel, ...) based on the passage of realtime. Does not
         * {@code invalidate()}. Called at the start of {@code draw()}. Detects the end-of-game and
         * sets the UI to the next state. First we initialize {@code long now} to the current system
         * time in milliseconds, and if {@code mLastTime} is greater than this we return having done
         * nothing (our game start sets {@code mLastTime} to 100ms in the future to delay the start
         * of the game). Otherwise we initialize {@code double elapsed} to the quantity {@code now}
         * minus {@code mLastTime} all divided by 1000.0 (the time that has elapsed since {@code mLastTime}
         * in seconds). If our field {@code int mRotating} is not zero we want to update our heading
         * so we add {@code mRotating} times PHYS_SLEW_SEC, times {@code elapsed} to our heading field
         * {@code double mHeading}, and if it is less than 0 we normalize it by adding 360 degrees or
         * if it is greater than or equal to 360 we normalize it by subtracting 360 degrees.
         * <p>
         * We next initialize our base accelerations {@code double ddx} (0 for x), and {@code double ddy}
         * (gravity for y: minus PHYS_DOWN_ACCEL_SEC times {@code elapsed}). If {@code mEngineFiring} is
         * true (the rocket engine is firing) we need to adjust these so we initialize {@code double elapsedFiring}
         * to {@code elapsed} and initialize {@code double fuelUsed} to {@code elapsedFiring} times
         * {@code PHYS_FUEL_SEC}. If {@code fuelUsed} (the amount of fuel we used) is greater than
         * {@code mFuel} (the amount of fuel we had left) we have to adjust for the fact we ran out
         * of fuel partway through the time that has elapsed, and if so we set {@code elapsedFiring}
         * to {@code mFuel} divided by {@code fuelUsed} times {@code elapsed} (the amount of time our
         * fuel lasted during this time period), and set {@code fuelUsed} to {@code mFuel}. We then
         * set {@code mEngineFiring} to false. Having corrected the {@code fuelUsed} if needed we subtract
         * it from {@code mFuel} then initialize {@code double accel} to PHYS_FIRE_ACCEL_SEC times
         * {@code elapsedFiring} (acceleration from the engine), and initialize {@code double radians}
         * to 2 times pi times {@code mHeading} divided by 360 (our heading in radians). We then set
         * {@code ddx} to the sin of {@code radians} times {@code accel} and {@code ddy} to the cosine
         * of {@code radians} times {@code accel}.
         * <p>
         * Having updated {@code ddx} and {@code ddy} if the rocket engine was firing, we initialize
         * {@code double dxOld} to {@code mDX} and {@code double dyOld} to {@code mDY}, add {@code ddx}
         * to {@code mDX} and {@code ddy} to {@code mDY} (the speeds for the end of the period) then
         * update the position ({@code mX},{@code mY}) based on average speed during the period (ie.
         * add {@code elapsed} times the quantity {@code mDX} plus {@code dxOld} all divided by 2 to
         * {@code mX}, and add {@code elapsed} times the quantity {@code mDY} plus {@code dyOld} all
         * divided by 2 to to {@code mY}). We then set our field {@code mLastTime} to {@code now}.
         * <p>
         * We now need to evaluate whether we have landed, so to do this we initialize {@code double yLowerBound}
         * to TARGET_PAD_HEIGHT plus {@code mLanderHeight} divided by 2 minus TARGET_BOTTOM_PADDING and
         * if {@code mY} is now less than or equal to {@code yLowerBound} the Eagle has landed and we
         * need to see if we won or lost (if it is still greater the game goes on and our method is done).
         * To determine if we won or lost we set {@code mY} to {@code yLowerBound}, initialize {@code int result}
         * to STATE_LOSE, {@code CharSequence message} to the empty string, {@code Resources res} to a
         * {@code Resources} instance for the application's package, {@code double speed} to the magnitude
         * of the vector ({@code mDX},{@code mDY}), and {@code boolean onGoal} to the value of the inequality
         * {@code mGoalX} is less than or equal to {@code mX} minus half of {@code mLanderWidth}, and
         * {@code mX} plus half of {@code mLanderWidth} is less than or equal to {@code mGoalX} plus
         * {@code mGoalWidth} (when true we are on the landing pad). Next we check to see if we have
         * achieved the oddball "Hyperspace" win (upside down, going fast) which happens when {@code onGoal}
         * is true and the absolute value of {@code mHeading} minus 180 is less than {@code mGoalAngle}
         * and {@code speed} is greater than PHYS_SPEED_HYPERSPACE, in which case we increment {@code mWinsInARow}
         * call our {@code doStart} method to restart the game back at the top and then we return.
         * <p>
         * In all other cases we need to evaluate whether we have failed to achieve our goals, so we
         * proceed to branch evaluating each in turn:
         * <ul>
         *     <li>
         *         {@code onGoal} is false: (we have missed the landing pad) We set {@code message} to the
         *         string with resource id R.string.message_off_pad.
         *     </li>
         *     <li>
         *         {@code mHeading} is greater than {@code mGoalAngle} or less than 360 minus {@code mGoalAngle}
         *         (Bad Angle) We set {@code message} to the string with resource id R.string.message_bad_angle.
         *     </li>
         *     <li>
         *         {@code speed} is greater than {@code mGoalSpeed} (Too Fast) We set {@code message} to the
         *         string with resource id R.string.message_too_fast.
         *     </li>
         *     <li>
         *         If we succeed in avoiding loss due to the above tests we set {@code result} to STATE_WIN
         *         and increment {@code mWinsInARow}
         *     </li>
         * </ul>
         * Finally we call our {@code setState} method to set the game state to {@code result} and display
         * {@code message} (if we lost that is, it is still the empty string if we won).
         */
        private void updatePhysics() {
            long now = System.currentTimeMillis();

            // Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) return;

            double elapsed = (now - mLastTime) / 1000.0;

            // mRotating -- update heading
            if (mRotating != 0) {
                mHeading += mRotating * (PHYS_SLEW_SEC * elapsed);

                // Bring things back into the range 0..360
                if (mHeading < 0)
                    mHeading += 360;
                else if (mHeading >= 360) mHeading -= 360;
            }

            // Base accelerations -- 0 for x, gravity for y
            double ddx = 0.0;
            double ddy = -PHYS_DOWN_ACCEL_SEC * elapsed;

            if (mEngineFiring) {
                // taking 0 as up, 90 as to the right
                // cos(deg) is ddy component, sin(deg) is ddx component
                double elapsedFiring = elapsed;
                double fuelUsed = elapsedFiring * PHYS_FUEL_SEC;

                // tricky case where we run out of fuel partway through the
                // time that has elapsed
                if (fuelUsed > mFuel) {
                    elapsedFiring = mFuel / fuelUsed * elapsed;
                    fuelUsed = mFuel;

                    // Oddball case where we adjust the "control" from here
                    mEngineFiring = false;
                }

                mFuel -= fuelUsed;

                // have this much acceleration from the engine
                double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

                double radians = 2 * Math.PI * mHeading / 360;
                ddx = Math.sin(radians) * accel;
                ddy += Math.cos(radians) * accel;
            }

            double dxOld = mDX;
            double dyOld = mDY;

            // figure speeds for the end of the period
            mDX += ddx;
            mDY += ddy;

            // figure position based on average speed during the period
            mX += elapsed * (mDX + dxOld) / 2;
            mY += elapsed * (mDY + dyOld) / 2;

            mLastTime = now;

            // Evaluate if we have landed ... stop the game
            double yLowerBound = TARGET_PAD_HEIGHT + (mLanderHeight >> 1)
                    - TARGET_BOTTOM_PADDING;
            if (mY <= yLowerBound) {
                mY = yLowerBound;

                int result = STATE_LOSE;
                CharSequence message = "";
                Resources res = mContext.getResources();
                double speed = Math.hypot(mDX, mDY);
                boolean onGoal = (mGoalX <= mX - mLanderWidth / 2
                        && mX + mLanderWidth / 2 <= mGoalX + mGoalWidth);

                // "Hyperspace" win -- upside down, going fast,
                // puts you back at the top.
                if (onGoal && Math.abs(mHeading - 180) < mGoalAngle
                        && speed > PHYS_SPEED_HYPERSPACE) {
                    mWinsInARow++;
                    doStart();

                    return;
                    // Oddball case: this case does a return, all other cases
                    // fall through to setMode() below.
                } else if (!onGoal) {
                    message = res.getText(R.string.message_off_pad);
                } else if (!(mHeading <= mGoalAngle || mHeading >= 360 - mGoalAngle)) {
                    message = res.getText(R.string.message_bad_angle);
                } else if (speed > mGoalSpeed) {
                    message = res.getText(R.string.message_too_fast);
                } else {
                    result = STATE_WIN;
                    mWinsInARow++;
                }

                setState(result, message);
            }
        }
    }

    /**
     * Handle to the application context, used to e.g. fetch Drawables.
     */
    private Context mContext;

    /**
     * Pointer to the text view to display "Paused.." etc.
     */
    private TextView mStatusText;

    /**
     * The thread that actually draws the animation
     */
    private LunarThread thread;

    /**
     * Perform inflation from XML. First we call our super's constructor, then we initialize our variable
     * {@code SurfaceHolder holder} with the {@code SurfaceHolder} of our {@code SurfaceView} and add
     * 'this' as a {@code Callback} for it (we implement {@code SurfaceView.Callback}). Then we initialize
     * our field {@code LunarThread thread} with an anonymous class whose {@code handleMessage} override
     * sets the visibility of {@code TextView mStatusText} to the value stored in the data {@code Bundle}
     * of its parameter {@code Message m} under the key "viz", and sets the text of {@code TextView mStatusText}
     * to the value stored in the data {@code Bundle} under the key "text". We then enable the focus for
     * our {@code LunarView} so that we get key events before returning.
     *
     * @param context The Context the view is running in
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    @SuppressLint("HandlerLeak")
    public LunarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new LunarThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });

        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     *
     * @return the animation thread
     */
    public LunarThread getThread() {
        return thread;
    }

    /**
     * Standard override to get key-press events. We just return the value returned by the {@code doKeyDown}
     * method of our field {@code LunarThread thread}.
     *
     * @param keyCode a key code that represents the button pressed, from
     *                {@link android.view.KeyEvent}
     * @param msg     the KeyEvent object that defines the button action
     * @return true to consume the event, false to pass it on.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        return thread.doKeyDown(keyCode, msg);
    }

    /**
     * Standard override for key-up. We actually care about these, so we can turn off the engine or
     * stop rotating. We just return the value returned by the {@code doKeyUp} method of our field
     * {@code LunarThread thread}.
     *
     * @param keyCode A key code that represents the button pressed, from
     *                {@link android.view.KeyEvent}.
     * @param msg     The KeyEvent object that defines the button action.
     * @return true to consume the event, false to pass it on.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        return thread.doKeyUp(keyCode, msg);
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on focus lost. e.g. user
     * switches to take a call. If our parameter {@code boolean hasWindowFocus} is false (we have lost
     * focus) we call the {@code pause} method of our field {@code LunarThread thread} to pause the
     * game.
     *
     * @param hasWindowFocus True if the window containing this view now has
     *                       focus, false otherwise.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

    /**
     * Installs a pointer to the {@code TextView mStatusText} used for messages.
     *
     * @param textView {@code TextView} to use for our field {@code TextView mStatusText}
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    /**
     * This is called immediately after any structural changes (format or size) have been made to the
     * surface. Callback invoked when the surface dimensions change. We just call the {@code setSurfaceSize}
     * method of our field {@code LunarThread thread} to have it set its width to our parameter {@code int width}
     * and its height to our parameter {@code int height}.
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        thread.setSurfaceSize(width, height);
    }

    /**
     * Callback invoked when the Surface has been created and is ready to be used. We first call the
     * {@code setRunning(true)} method of our field {@code LunarThread thread} to signal the thread
     * that it should be running (sets its field {@code boolean mRun} to true) then call its {@code start}
     * method to have its {@code run} override called to actually start it running.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /**
     * Callback invoked when the Surface has been destroyed and must no longer be touched.
     * WARNING: after this method returns, the Surface/Canvas must never be touched again!
     * We initialize our variable {@code boolean retry} to true then call the {@code setRunning(false)}
     * method of our field {@code LunarThread thread} to signal the thread that it should stop running
     * (sets its field {@code boolean mRun} to false). Then while our variable {@code retry} remains
     * true we loop executing a try block intended to catch and log {@code InterruptedException} calling
     * the {@code join} method of our field {@code LunarThread thread}, setting {@code retry} to false
     * only when the {@code join} method returns having waited for the thread to die without being
     * interrupted.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.i(TAG, "surfaceDestroyed was interrupted");
            }
        }
    }
}
