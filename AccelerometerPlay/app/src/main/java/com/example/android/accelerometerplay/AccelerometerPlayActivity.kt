/*
 * Copyright (C) 2010 The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.accelerometerplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import kotlin.math.sqrt
import androidx.core.graphics.scale

/**
 * This is an example of using the accelerometer to integrate the device's
 * acceleration to a position using the Verlet method. This is illustrated with
 * a very simple particle system comprised of a few iron balls freely moving on
 * an inclined wooden table. The inclination of the virtual table is controlled
 * by the device's accelerometer.
 *
 * @see SensorManager
 *
 * @see SensorEvent
 *
 * @see Sensor
 */
class AccelerometerPlayActivity : ComponentActivity() {
    /**
     * The [SimulationView] instance which is used as our content view.
     */
    private var mSimulationView: SimulationView? = null

    /**
     * Our handle to the SENSOR_SERVICE system level service for accessing sensors.
     */
    private var mSensorManager: SensorManager? = null

    /**
     * Our handle to the POWER_SERVICE system level service for controlling power management,
     * including "wake locks," which let you keep the device on while you're running long tasks.
     */
    private var mPowerManager: PowerManager? = null

    /**
     * Our handle to the WINDOW_SERVICE system level service, used for accessing the system's window
     * manager in order to retrieve the default display for our field `Display mDisplay`.
     */
    private var mWindowManager: WindowManager? = null

    /**
     * The default `Display`, we use it to retrieve the current rotation of the screen in
     * order to take it into account when interpreting the sensors (which always return data in
     * a coordinate space aligned with the screen in its native orientation).
     */
    private var mDisplay: Display? = null

    /**
     * The PARTIAL_WAKE_LOCK we acquire in order to keep the screen on during the simulation. This
     * is not really necessary since we add flags to our current window which do this as well.
     */
    private var mWakeLock: WakeLock? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`.
     * Next we initialize our [SensorManager] property [mSensorManager] with an instance of the
     * system level service SENSOR_SERVICE (used for accessing sensors), and [PowerManager] property
     * [mPowerManager] with an instance of the system level service POWER_SERVICE (used for
     * controlling power management, including "wake locks"), and [WindowManager] property
     * [mWindowManager] with an instance of the system level service WINDOW_SERVICE (used for
     * accessing the system's window manager). We then use [mWindowManager] to retrieve the
     * [Display] upon which this [WindowManager] instance will create new windows and save it
     * in our [Display] property [mDisplay]. We use [mPowerManager] to create a PARTIAL_WAKE_LOCK
     * using our class name string as the tag and save it in our [WakeLock] property [mWakeLock].
     * We initialize our [SimulationView] property [mSimulationView] with a new instance and set
     * it as our content view. Finally we retrieve the current [android.view.Window]
     * for this activity and add the following flags to it:
     *  * FLAG_SHOW_WHEN_LOCKED: special flag to let windows be shown when the screen is locked.
     *  This will let application windows take precedence over key guard or any other lock screens
     *
     *  * FLAG_DISMISS_KEYGUARD: when set the window will cause the keyguard to be dismissed,
     *  only if it is not a secure lock keyguard. Because such a keyguard is not needed for
     *  security, it will never re-appear if the user navigates to another window (in contrast
     *  to FLAG_SHOW_WHEN_LOCKED, which will only temporarily hide both secure and non-secure
     *  keyguards but ensure they reappear when the user moves to another UI that doesn't hide
     *  them). If the keyguard is currently active and is secure (requires an unlock credential)
     *  than the user will still need to confirm it before seeing this window, unless
     *  FLAG_SHOW_WHEN_LOCKED has also been set.
     *
     *  * FLAG_KEEP_SCREEN_ON: as long as this window is visible to the user, keep the device's
     *  screen turned on and bright.
     *
     *  * FLAG_TURN_SCREEN_ON: when set as a window is being added or made visible, once the
     *  window has been shown then the system will poke the power manager's user activity
     *  (as if the user had woken up the device) to turn the screen on.
     *
     *  * FLAG_ALLOW_LOCK_WHILE_SCREEN_ON: as long as this window is visible to the user, allow
     *  the lock screen to activate while the screen is on.
     *
     * We initialize our [FrameLayout] variable `rootView` to the view with ID `android.R.id.content`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the listener argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `insets` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument, then it updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `insets.left`,
     * the right margin set to `insets.right`, the top margin set to `insets.top`,
     * and the bottom margin set to `insets.bottom`. Finally it returns
     * [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Get an instance of the SensorManager
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Get an instance of the PowerManager
        mPowerManager = getSystemService(POWER_SERVICE) as PowerManager

        // Get an instance of the WindowManager
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mDisplay = display
        } else {
            @Suppress("DEPRECATION") // Needed for versions less than Build.VERSION_CODES.R
            mDisplay = mWindowManager!!.defaultDisplay
        }

        // Create a partial wake lock
        mWakeLock = mPowerManager!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)

        // instantiate our simulation view and set it as the activity's content
        mSimulationView = SimulationView(this)
        setContentView(mSimulationView)
        @Suppress("DEPRECATION") // TODO: Fix WindowManager.LayoutParams deprecations
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        val rootView = window.decorView.findViewById<FrameLayout>(android.R.id.content)
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
     * Called after [onRestoreInstanceState], [onRestart], or [onPause], for
     * our activity to start interacting with the user. First we call our super's implementation of
     * `onResume`, then we use our field `WakeLock mWakeLock` to acquire a wake lock
     * with a timeout value of WAKE_LOCK_TIMEOUT (1_000_000L), and then we call the `startSimulation`
     * method of our field `SimulationView mSimulationView` to start our simulation running.
     */
    override fun onResume() {
        super.onResume()
        /*
         * when the activity is resumed, we acquire a wake-lock so that the
         * screen stays on, since the user will likely not be fiddling with the
         * screen or buttons.
         */
        mWakeLock!!.acquire(WAKE_LOCK_TIMEOUT)

        // Start the simulation
        mSimulationView!!.startSimulation()
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but
     * has not (yet) been killed. First we call our super's implementation of `onPause`. We
     * then call the `stopSimulation` method of our field `SimulationView mSimulationView`
     * to stop the simulation and unregister the `SensorEventListener` it registered so it will
     * stop consuming sensor power. Finally we release the wakelock we acquired using our field
     * `WakeLock mWakeLock`.
     */
    override fun onPause() {
        super.onPause()
        /*
         * When the activity is paused, we make sure to stop the simulation,
         * release our sensor resources and wake locks
         */
        // Stop the simulation
        mSimulationView!!.stopSimulation()

        // and release our wake-lock
        mWakeLock!!.release()
    }

    /**
     * `View` which draws our simulation.
     * Our constructor, called from the `onCreate` override of `AccelerometerPlayActivity`.
     * First we call our super's constructor. Then we initialize our field `Sensor mAccelerometer`
     * with a reference to the default accelerometer sensor returned by the `getDefaultSensor`
     * method of the SENSOR_SERVICE system level service `SensorManager mSensorManager`. We
     * initialize `DisplayMetrics metrics` with a new instance, then fetch the window manager
     * for showing custom windows, call its `getDefaultDisplay` method to retrieve the window
     * it is managing, and call the `getMetrics` method of that window to load `metrics`
     * with the display metrics that describe the size and density of the display. We then initialize
     * our field `float mXDpi` with the exact physical pixels per inch of the screen in the X
     * dimension from the `xdpi` field of `metrics`, and `float mYDpi` with the
     * exact physical pixels per inch of the screen in the Y dimension from the `ydpi` field
     * of `metrics`. We convert `mXDpi` to meters to set `mMetersToPixelsX` and
     * `mYDpi` to meters to set `mMetersToPixelsY`. We initialize `Bitmap ball`
     * by decoding the png with resource id R.drawable.ball, calculate `int dstWidth` and
     * `int dstHeight` to be approximately 0.5 cm given the value of `mMetersToPixelsX`
     * and `mMetersToPixelsY`, then set `Bitmap mBitmap` to a new bitmap created from
     * `ball` by scaling it to `int dstWidth` by `int dstHeight`. We initialize
     * `Options opts` with a new instance, set its `inDither` field to true (ignored),
     * and set its `inPreferredConfig` field to RGB_565. We then use `opts` as the
     * options when we load the jpg with resource id R.drawable.wood into `Bitmap mWood`.
     *
     * @param context the [Context] to use to access resources.
     */
    internal inner class SimulationView(context: Context?) : View(context), SensorEventListener {
        /**
         * Reference to the default accelerometer sensor (TYPE_ACCELEROMETER).
         */
        private val mAccelerometer: Sensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        /**
         * Timestamp of the last accelerometer event processed.
         */
        private var mLastT: Long = 0

        /**
         * Delta time between the previous two accelerometer events processed.
         */
        private var mLastDeltaT = 0f

        /**
         * The exact physical pixels per inch of the screen in the X dimension.
         */
        private val mXDpi: Float

        /**
         * The exact physical pixels per inch of the screen in the Y dimension.
         */
        private val mYDpi: Float

        /**
         * `mXDpi` in meters, the exact physical pixels per meters of the screen in the X dimension.
         */
        private val mMetersToPixelsX: Float

        /**
         * `mYDpi` in meters, the exact physical pixels per meters of the screen in the Y dimension.
         */
        private val mMetersToPixelsY: Float

        /**
         * Scaled `Bitmap` containing our ball, the png with resource id R.drawable.ball
         */
        private val mBitmap: Bitmap

        /**
         * `Bitmap` containing our background, the jpg with resource id R.drawable.wood
         */
        private val mWood: Bitmap

        /**
         * The X coordinate of the origin of the screen relative to the origin of the bitmap
         */
        private var mXOrigin = 0f

        /**
         * The Y coordinate of the origin of the screen relative to the origin of the bitmap
         */
        private var mYOrigin = 0f

        /**
         * The X value of the current `SensorEvent`, after taking into account the rotation of
         * the screen with respect to the sensors.
         */
        private var mSensorX = 0f

        /**
         * The Y value of the current `SensorEvent`, after taking into account the rotation of
         * the screen with respect to the sensors.
         */
        private var mSensorY = 0f

        /**
         * The time in nanoseconds at which the current event happened.
         */
        private var mSensorTimeStamp: Long = 0

        /**
         * The value of the running Java Virtual Machine's high-resolution time source, in nanoseconds
         * when the current event was received by our `onSensorChanged` override.
         */
        private var mCpuTimeStamp: Long = 0

        /**
         * X coordinate of the far edge of the playing field in meters relative to the center
         */
        private var mHorizontalBound = 0f

        /**
         * Y coordinate of the far edge of the playing field in meters relative to the center
         */
        private var mVerticalBound = 0f

        /**
         * `ParticleSystem` containing our collection of 15 particles
         */
        private val mParticleSystem = ParticleSystem()

        /**
         * Each of our particles holds its previous and current position, and its acceleration.
         * For added realism each particle also has its own friction coefficient.
         */
        internal inner class Particle {
            /**
             * Current X coordinate of this particle's position.
             */
            var mPosX = 0f

            /**
             * Current Y coordinate of this particle's position.
             */
            var mPosY = 0f

            /**
             * Current acceleration of the particle in the X direction.
             */
            private var mAccelX = 0f

            /**
             * Current acceleration of the particle in the X direction.
             */
            private var mAccelY = 0f

            /**
             * Previous X coordinate of this particle's position.
             */
            private var mLastPosX = 0f

            /**
             * Previous Y coordinate of this particle's position.
             */
            private var mLastPosY = 0f

            /**
             * This particle's coefficient of friction.
             */
            private val mOneMinusFriction: Float

            /**
             * Our constructor. We just initialize our field `float mOneMinusFriction` with a
             * random coefficient of friction.
             */
            init {
                // make each particle a bit different by randomizing its
                // coefficient of friction
                val r = (Math.random().toFloat() - 0.5f) * 0.2f
                mOneMinusFriction = 1.0f - S_FRICTION + r
            }

            /**
             * Applies physics model to this `Particle` given the current accelerometer reading.
             * We initialize the mass of our virtual object `float m` to 1_000, initialize the
             * X component of the force of gravity `float gx` to minus the X component of the
             * current accelerometer reading `sx` times `m`, and initialize the Y component
             * of the force of gravity `float gy` to minus the Y component of the current accelerometer
             * reading `sy` times `m`. We initialize the inverse of the mass of the object
             * `float invm` to 1.0 over `m`, then initialize `float ax` to `gx`
             * times `invm` and `float ay` to `gy` times `invm`.
             *
             * Now we calculate the time-corrected Verlet integration to calculate the new position
             * of the `Particle` `float x` and `float y`, set `mLastPosX` to
             * `mPosX` and `mLastPosY` to `mPosY` then update `mPosX` and
             * `mPosY` with `x` and `y` respectively. Finally we save the acceleration
             * components `ax` and `ay` in `mAccelX` and `mAccelY`.
             *
             * @param sx X component of the current accelerometer reading.
             * @param sy Y component of the current accelerometer reading.
             * @param dT Delta time since previous sensor event.
             * @param dTC Time corrected delta time (used to compensate for variable delta time).
             */
            fun computePhysics(sx: Float, sy: Float, dT: Float, dTC: Float) {
                // Force of gravity applied to our virtual object
                val m = 1000.0f // mass of our virtual object
                val gx = -sx * m
                val gy = -sy * m

                /*
                 * F = mA <=> A = F / m We could simplify the code by
                 * completely eliminating "m" (the mass) from all the equations,
                 * but it would hide the concepts from this sample code.
                 */
                val invm = 1.0f / m
                val ax = gx * invm
                val ay = gy * invm

                /*
                 * Time-corrected Verlet integration The position Verlet
                 * integrator is defined as x(t+dt) = x(t) + x(t) - x(t-dt) +
                 * a(t).t^2 However, the above equation doesn't handle variable
                 * dt very well, a time-corrected version is needed: x(t+dt) =
                 * x(t) + (x(t) - x(t-dt)) * (dt/dt_prev) + a(t).t^2 We also add
                 * a simple friction term (f) to the equation: x(t+dt) = x(t) +
                 * (1-f) * (x(t) - x(t-dt)) * (dt/dt_prev) + a(t)t^2
                 */
                val dTdT = dT * dT
                val x = mPosX + mOneMinusFriction * dTC * (mPosX - mLastPosX) + mAccelX * dTdT
                val y = mPosY + mOneMinusFriction * dTC * (mPosY - mLastPosY) + mAccelY * dTdT
                mLastPosX = mPosX
                mLastPosY = mPosY
                mPosX = x
                mPosY = y
                mAccelX = ax
                mAccelY = ay
            }

            /**
             * Resolving constraints and collisions with the Verlet integrator can be very simple,
             * we simply need to move a colliding or constrained particle in such way that the
             * constraint is satisfied. To do this we initialize `float xmax` to the value
             * of the X coordinate of the far edge of the playing field in meters `mHorizontalBound`,
             * and `float ymax` to the value of the Y coordinate of the far edge of the playing
             * field in meters `mVerticalBound`. We initialize `float x` to the current
             * X coordinate of the position of the ball `mPosX` and `float y` to the current
             * Y coordinate of the position of the ball `mPosY`. If `x` is greater than
             * `xmax` we set `mPosX` to `xmax` and if it is less the minus `xmax`
             * we set `mPosX` to minus `xmax`. If `x` is greater than `ymax`
             * we set `mPosY` to `ymax` and if it is less the minus `ymax` we set
             * `mPosY` to minus `ymax`.
             */
            fun resolveCollisionWithBounds() {
                val xmax = mHorizontalBound
                val ymax = mVerticalBound
                val x = mPosX
                val y = mPosY
                if (x > xmax) {
                    mPosX = xmax
                } else if (x < -xmax) {
                    mPosX = -xmax
                }
                if (y > ymax) {
                    mPosY = ymax
                } else if (y < -ymax) {
                    mPosY = -ymax
                }
            }
        }

        /**
         * A particle system is just a collection of particles
         */
        internal inner class ParticleSystem {
            /**
             * Our collection of balls.
             */
            private val mBalls = arrayOfNulls<Particle>(NUM_PARTICLES)

            /**
             * Our constructor. We loop through our array `Particle mBalls[]` creating a new
             * instance of `Particle` for each.
             */
            init {
                /*
                 * Initially our particles have no speed or acceleration
                 */
                for (i in mBalls.indices) {
                    mBalls[i] = Particle()
                }
            }

            /**
             * Update the position of each particle in the system using the Verlet integrator. We copy
             * our parameter `long timestamp` to initialize our variable `long t` and if
             * our field `mLastT` is not zero (we have been called before) we initialize our
             * variable `float dT` to the delta time between `t` and `mLastT` divided
             * by 1 billion (converting nanoseconds to seconds). Then if `mLastDeltaT` is not 0
             * we calculate the corrected delta time `float dTC` to be `dT` over `mLastDeltaT`,
             * initialize `int count` to the size of our array `mBalls` in order to use it
             * to loop through all the `Particle ball` in `mBalls` calling this `computePhysics`
             * method with `sx`, `sy`, `dT` and `dTC`. When done (or if `mLastT`
             * was 0) we set `mLastDeltaT` to `dT`, and then whether `mLastT` was 0
             * or not we set `mLastT` to `t`.
             *
             * @param sx X component of the current accelerometer reading.
             * @param sy Y component of the current accelerometer reading.
             * @param timestamp latency corrected timestamp of the sensor event in nanoseconds.
             */
            private fun updatePositions(sx: Float, sy: Float, timestamp: Long) {
                if (mLastT != 0L) {
                    val dT = (timestamp - mLastT).toFloat() * (1.0f / 1000000000.0f)
                    if (mLastDeltaT != 0f) {
                        val dTC = dT / mLastDeltaT
                        val count = mBalls.size
                        for (i in 0 until count) {
                            val ball = mBalls[i]
                            ball!!.computePhysics(sx, sy, dT, dTC)
                        }
                    }
                    mLastDeltaT = dT
                }
                mLastT = timestamp
            }

            /**
             * Performs one iteration of the simulation. First updating the position of all the
             * particles and resolving the constraints and collisions. First we call our method
             * `updatePositions` with our parameters to update the position of all of the
             * `Particle` objects in our system. We initialize NUM_MAX_ITERATIONS to 10 (this
             * is the maximum number of collision resolution passes we perform) and initialize our
             * flag `boolean more` to true (it is set to true whenever a collision has been
             * detected, and is false if no collisions were detected in the last loop through the
             * balls in the system). We then initialize `int count` with the length of our
             * array `Particle mBalls[]`.
             *
             * We now loop while `more` is true (a collision was detected the last time) and
             * while we have not looped for more the NUM_MAX_ITERATIONS:
             *  * We set `more` to false.
             *
             *  * We loop over `i` for all the `mBalls[ i ]` setting `Particle curr`
             *  to `mBalls[ i ]` then loop over `j` from `i+1` to `count`:
             *
             *  * Setting `Particle ball` to `mBalls[ j ]` then calculating the
             *  distance in the X coordinate between `ball` and `curr` (the
             *  difference of their two `mPosX` fields) to be `float dx` and
             *  the distance in the Y coordinate between `ball` and `curr` to
             *  be `float dy`. The square of the distance between them (the sum of
             *  `dx` times `dx` and `dy` times `dy`) is calculated
             *  to set `float dd`. If `dd` is less than or equal to `sBallDiameter2`
             *  (a collision has occurred), we update the `mPosX` and `mPosY`
             *  fields of both `curr` and `ball` to simulate a springy bounce
             *  with a bit randomness to it, and set `more` to true.
             *
             *  * We call the `resolveCollisionWithBounds` method of `curr` to
             *  make sure any position changes made to it leave it inside the bounds of
             *  the simulation.
             *
             * @param sx X component of the current accelerometer reading.
             * @param sy Y component of the current accelerometer reading.
             * @param now latency corrected timestamp of the sensor event in nanoseconds.
             */
            fun update(sx: Float, sy: Float, now: Long) {
                // update the system's positions
                updatePositions(sx, sy, now)

                // We do no more than a limited number of iterations
                val maxCollisionIterations = 10

                /*
                 * Resolve collisions, each particle is tested against every
                 * other particle for collision. If a collision is detected the
                 * particle is moved away using a virtual spring of infinite
                 * stiffness.
                 */
                var more = true
                val count = mBalls.size
                var k = 0
                while (k < maxCollisionIterations && more) {
                    more = false
                    for (i in 0 until count) {
                        val curr = mBalls[i]
                        for (j in i + 1 until count) {
                            val ball = mBalls[j]
                            var dx = ball!!.mPosX - curr!!.mPosX
                            var dy = ball.mPosY - curr.mPosY
                            var dd = dx * dx + dy * dy
                            // Check for collisions
                            if (dd <= S_BAL_DIAMETER2) {
                                /*
                                 * add a little bit of entropy, after nothing is
                                 * perfect in the universe.
                                 */
                                dx += (Math.random().toFloat() - 0.5f) * 0.0001f
                                dy += (Math.random().toFloat() - 0.5f) * 0.0001f
                                dd = dx * dx + dy * dy
                                // simulate the spring
                                val d = sqrt(dd.toDouble()).toFloat()
                                val c = 0.5f * (S_BAL_DIAMETER - d) / d
                                curr.mPosX -= dx * c
                                curr.mPosY -= dy * c
                                ball.mPosX += dx * c
                                ball.mPosY += dy * c
                                more = true
                            }
                        }
                        /*
                         * Finally make sure the particle doesn't intersects
                         * with the walls.
                         */
                        curr!!.resolveCollisionWithBounds()
                    }
                    k++
                }
            }

            /**
             * Returns the number of `Particle` objects in our `ParticleSystem`, which is
             * just the length of our array `Particle mBalls[]`.
             *
             * @return the number of `Particle` objects in our `ParticleSystem`
             */
            fun getParticleCount(): Int {
                return mBalls.size
            }

            /**
             * Getter for the `mPosX` field of the `i`'th entry in our array `mBalls[i]`.
             *
             * @param i which `Particle` in our array `Particle mBalls[]`.
             * @return the `mPosX` field of `mBalls[i]`
             */
            fun getPosX(i: Int): Float {
                return mBalls[i]!!.mPosX
            }

            /**
             * Getter for the `mPosY` field of the `i`'th entry in our array `mBalls[i]`.
             *
             * @param i which `Particle` in our array `Particle mBalls[]`.
             * @return the `mPosY` field of `mBalls[i]`
             */
            fun getPosY(i: Int): Float {
                return mBalls[i]!!.mPosY
            }

        }

        /**
         * Starts the simulation running. We just register 'this' as a `SensorEventListener` for
         * `Sensor mAccelerometer` (our `onSensorChanged` override will be called) with a
         * sampling period of SENSOR_DELAY_UI (rate suitable for the user interface).
         */
        fun startSimulation() {
            /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
             */
            mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        /**
         * Called to stop our simulation running (called from our `onPause` override). We just
         * unregister 'this' as a `SensorEventListener` for all sensors.
         */
        fun stopSimulation() {
            mSensorManager!!.unregisterListener(this)
        }

        init {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION") // TODO: Fix getMetrics deprecation
            windowManager.defaultDisplay.getMetrics(metrics)
            mXDpi = metrics.xdpi
            mYDpi = metrics.ydpi
            mMetersToPixelsX = mXDpi / 0.0254f
            mMetersToPixelsY = mYDpi / 0.0254f

            // rescale the ball so it's about 0.5 cm on screen
            val ball = BitmapFactory.decodeResource(resources, R.drawable.ball)
            val dstWidth = (S_BAL_DIAMETER * mMetersToPixelsX + 0.5f).toInt()
            val dstHeight = (S_BAL_DIAMETER * mMetersToPixelsY + 0.5f).toInt()
            mBitmap = ball.scale(dstWidth, dstHeight)
            val opts = BitmapFactory.Options()
            @Suppress("DEPRECATION") // TODO: As of Build.VERSION_CODES.N, this is ignored
            opts.inDither = true
            opts.inPreferredConfig = Bitmap.Config.RGB_565
            mWood = BitmapFactory.decodeResource(resources, R.drawable.wood, opts)
        }

        /**
         * This is called during layout when the size of this view has changed. If you were just
         * added to the view hierarchy, you're called with the old values of 0. We calculate the
         * value of `mXOrigin` (X coordinate of the origin of the screen relative to the
         * origin of the background bitmap) by subtracting the width of `mBitmap` from our
         * parameter `w` and multiplying by 0.5f, and the value of `mYOrigin` (Y
         * coordinate of the origin of the screen relative to the origin of the background bitmap)
         * by subtracting the height of `mBitmap` from our parameter `h` and multiplying
         * by 0.5f. We then calculate the value of `mHorizontalBound` (X coordinate of the far
         * edge of the playing field in meters relative to the center) by dividing our parameter
         * `w` by `mMetersToPixelsX`, subtracting off `sBallDiameter` (diameter
         * of the balls in meters) then multiplying the result by 0.5f, and likewise we calculate
         * the value of `mVerticalBound` (Y coordinate of the far edge of the playing field in
         * meters relative to the center) by dividing our parameter `h` by `mMetersToPixelsX`,
         * subtracting off `sBallDiameter` then multiplying the result by 0.5f.
         *
         * @param w Current width of this view.
         * @param h Current height of this view.
         * @param oldw Old width of this view.
         * @param oldh Old height of this view.
         */
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            // compute the origin of the screen relative to the origin of
            // the bitmap
            mXOrigin = (w - mBitmap.width) * 0.5f
            mYOrigin = (h - mBitmap.height) * 0.5f
            mHorizontalBound = (w / mMetersToPixelsX - S_BAL_DIAMETER) * 0.5f
            mVerticalBound = (h / mMetersToPixelsY - S_BAL_DIAMETER) * 0.5f
        }

        /**
         * Called when there is a new sensor event. Note that "on changed" is somewhat of a misnomer,
         * as this will also be called if we have a new reading from a sensor with the exact same
         * sensor values (but a newer timestamp). If the type of sensor of our parameter
         * `SensorEvent event` is not TYPE_ACCELEROMETER (constant describing an accelerometer
         * sensor type) we return having done nothing. Otherwise we switch on the rotation of the
         * screen from its "natural" orientation:
         *  * ROTATION_0 (0 degree rotation (natural orientation)) we set `mSensorX` to the
         *  `values[0]` field of our parameter `SensorEvent event`, and `mSensorY`
         *  to the `values[1]` field and break.
         *
         *  * ROTATION_90 (90 degree rotation) we set `mSensorX` to minus the `values[1]`
         *  field of our parameter `SensorEvent event`, and `mSensorY` to the
         *  `values[0]` field and break.
         *
         *  * ROTATION_180 (180 degree rotation) we set `mSensorX` to minus the `values[0]`
         *  field of our parameter `SensorEvent event`, and `mSensorY` to minus the
         *  `values[1]` field and break.
         *
         *  * ROTATION_270 (270 degree rotation) we set `mSensorX` to the `values[1]`
         *  field of our parameter `SensorEvent event`, and `mSensorY` to minus the
         *  `values[0]` field and break.
         *
         * Finally we set `mSensorTimeStamp` (time in nanoseconds at which the current event
         * happened) to the `timestamp` field of `event` and `mCpuTimeStamp` (the
         * current time in nanosecond) to the current value of the running Java Virtual Machine's
         * high-resolution time source, in nanoseconds.
         *
         * @param event the [SensorEvent][SensorEvent].
         */
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            when (mDisplay!!.rotation) {
                Surface.ROTATION_0 -> {
                    mSensorX = event.values[0]
                    mSensorY = event.values[1]
                }

                Surface.ROTATION_90 -> {
                    mSensorX = -event.values[1]
                    mSensorY = event.values[0]
                }

                Surface.ROTATION_180 -> {
                    mSensorX = -event.values[0]
                    mSensorY = -event.values[1]
                }

                Surface.ROTATION_270 -> {
                    mSensorX = event.values[1]
                    mSensorY = -event.values[0]
                }
            }
            mSensorTimeStamp = event.timestamp
            mCpuTimeStamp = System.nanoTime()
        }

        /**
         * We implement this to do our drawing. First we have our parameter `Canvas canvas` draw
         * our woody background bitmap `mWood` starting at (0,0) with null as the `Paint`.
         * We initialize `ParticleSystem particleSystem` with a copy of of `mParticleSystem`,
         * initialize `long now` by adding the time elapsed since our `onSensorChanged`
         * override processed the current accelerometer sensor event to the time in nanoseconds at
         * which the event happened in `mSensorTimeStamp`. We initialize `sx` by copying
         * the value of `mSensorX` (X component of the current SensorEvent, after taking into
         * account the rotation of the screen with respect to the sensors) to it, and `sy` by
         * copying the value of `mSensorY` (Y component of the current SensorEvent, after taking
         * into account the rotation of the screen with respect to the sensors) to it. We then call
         * the `update` method of `particleSystem` with  `sx`, `sy`, and `now` to have it update
         * all of the positions of the particles in it given the current sensor event.
         *
         * We now create copies of our fields as follows:
         *  * `float xc` gets `mXOrigin` (X coordinate of the origin of the screen
         *  relative to the origin of the bitmap
         *
         *  * `float yc` gets `mYOrigin` (Y coordinate of the origin of the screen
         *  relative to the origin of the bitmap
         *
         *  * `float xs` gets `mMetersToPixelsX` (exact physical pixels per meters
         *  of the screen in the X dimension)
         *
         *  * `float ys` gets `mMetersToPixelsY` (exact physical pixels per meters
         *  of the screen in the Y dimension)
         *
         *  * `Bitmap bitmap` gets `mBitmap` (scaled Bitmap containing our ball)
         *
         * We then initialize `int count` with the number of balls in `particleSystem` in
         * order to use it as the limit in a for loop over `int i`, where we:
         *  * Initialize `float x` by adding `xc` to `xs` times the X position
         *  of the `Particle` in `particleSystem` at position `i`
         *
         *  * Initialize `float y` by adding `yc` to `xs` times the Y position of the `Particle`
         *  in `particleSystem` at position `i`
         *
         *  * We then have our parameter `canvas` draw `bitmap` at `(x,y)` with
         *  a null paint.
         *
         * Having drawn all the balls we call the `invalidate` method to ensure that we will be
         * called again at some point in the future.
         *
         * @param canvas the canvas on which the background will be drawn
         */
        override fun onDraw(canvas: Canvas) {

            /*
             * draw the background
             */
            canvas.drawBitmap(mWood, 0f, 0f, null)

            /*
             * compute the new position of our object, based on accelerometer
             * data and present time.
             */
            val particleSystem = mParticleSystem
            val now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp)
            val sx = mSensorX
            val sy = mSensorY
            particleSystem.update(sx, sy, now)
            val xc = mXOrigin
            val yc = mYOrigin
            val xs = mMetersToPixelsX
            val ys = mMetersToPixelsY
            val bitmap = mBitmap
            val count = particleSystem.getParticleCount()
            for (i in 0 until count) {
                /*
                 * We transform the canvas so that the coordinate system matches
                 * the sensors coordinate system with the origin in the center
                 * of the screen and the unit is the meter.
                 */
                val x = xc + particleSystem.getPosX(i) * xs
                val y = yc - particleSystem.getPosY(i) * ys
                canvas.drawBitmap(bitmap, x, y, null)
            }

            // and make sure to redraw asap
            invalidate()
        }

        /**
         * Called when the accuracy of the registered sensor has changed. We ignore.
         *
         * @param sensor   The sensor whose accuracy changed.
         * @param accuracy The new accuracy of this sensor, one of `SensorManager.SENSOR_STATUS_*`
         */
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
}

/**
 * Timeout for our wakelock.
 */
private const val WAKE_LOCK_TIMEOUT = 1000000L
/**
 * diameter of the balls in meters
 */
private const val S_BAL_DIAMETER = 0.004f

/**
 * Square of the diameter of the balls in meters squared.
 */
private const val S_BAL_DIAMETER2 = S_BAL_DIAMETER * S_BAL_DIAMETER

/**
 * friction of the virtual table and air
 */
private const val S_FRICTION = 0.1f
/**
 * Number of balls in our particle system.
 */
const val NUM_PARTICLES: Int = 15