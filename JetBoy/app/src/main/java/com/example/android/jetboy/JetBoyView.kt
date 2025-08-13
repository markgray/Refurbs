/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * 
 */
// Android JET demonstration code:
// All inline comments related to the use of the JetPlayer class are preceded by "JET info:"
@file:Suppress("UNUSED_PARAMETER", "ReplaceNotNullAssertionWithElvisReturn", "ImplicitThis", "JoinDeclarationAndAssignment", "PrivatePropertyName", "KotlinConstantConditions", "UnnecessaryVariable", "MemberVisibilityCanBePrivate", "PropertyName",
    "RedundantSuppression"
)

package com.example.android.jetboy

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.JetPlayer
import android.media.JetPlayer.OnJetEventListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import java.util.Random
import java.util.Timer
import java.util.TimerTask
import java.util.Vector
import java.util.concurrent.ConcurrentLinkedQueue
import androidx.core.graphics.scale

/**
 * The constructor called when the layout file layout/main.xml is inflated in the main [JetBoy]
 * activity. In our `init` block we call our super's constructor, then we initialize [SurfaceHolder]
 * variable `val holder` by fetching the underlying [SurfaceHolder] of our [SurfaceView], and add
 * `this` as a [SurfaceHolder.Callback] to it. If we are not in edit mode (edit mode is `true` when
 * we are displayed within a developer tool like a layout editor) we initialize [JetBoyThread] field
 * [thread] (thread that actually draws the animation) with a new instance that uses an anonymous
 * [Handler] whose [Handler.handleMessage] override updates the timer [TextView] field [mTimerView]
 * with the string that is stored in the data [Bundle] of the message it receives under the key
 * "text", then if there is a string stored in the [Bundle] under the key "STATE_LOSE" it evaluates
 * whether the player has won or lost and updates the state of the UI accordingly. Whether we are
 * in edit mode or not we set ourselves to be focusable, and log the fact that we are done creating
 * the view.
 *
 * @param context The [Context] the view is running in, through which it can access the current
 * theme, resources, etc.
 * @param attrs   The attributes of the XML tag that is inflating the view.
 */
class JetBoyView(
    context: Context,
    attrs: AttributeSet?
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    /**
     * The thread that actually draws the animation
     */
    var thread: JetBoyThread? = null

    init {
        // register our interest in hearing about changes to our surface
        val holder = holder
        holder.addCallback(this)

        // create thread only; it's started in surfaceCreated()
        // except if used in the layout editor.
        if (!isInEditMode) {
            @SuppressLint("HandlerLeak")
            thread = JetBoyThread(holder, context,
                object : Handler(Looper.myLooper()!!) {
                    /**
                     * We implement this to receive messages. First we set the text of our timer
                     * [TextView] field [mTimerView] to the text stored under the key "text" in the
                     * data bundle of our [Message] parameter [m]. Then if and only if there is a
                     * string stored under the key "STATE_LOSE" in the data bundle of our [Message]
                     * parameter [m] we set the visibility of [Button] field [mButtonRetry] ("RETRY")
                     * to VISIBLE, set the visibility of [TextView] field [mTimerView] to INVISIBLE,
                     * and set the visibility of [TextView] field [mTextView] to VISIBLE. We then
                     * log the value of [Int] field [mHitTotal] (the total number of hits scored by
                     * the user). If [mHitTotal] is greater than or equal to [Int] field
                     * [SUCCESS_THRESHOLD] we set the text of [TextView] field [mTextView] to the
                     * string with resource id `R.string.winText` ("You win...") otherwise we set
                     * its text to the string "Sorry, You Lose! ...". We then set the text of
                     * [TextView] field [mTimerView] to the string "1:12", and set the height of
                     * [TextView] field [mTextView] to 20.
                     *
                     * @param m [Message] that we have been sent using our [Handler.sendMessage] method
                     */
                    @SuppressLint("SetTextI18n")
                    override fun handleMessage(m: Message) {
                        mTimerView!!.text = m.data.getString("text")
                        if (m.data.getString("STATE_LOSE") != null) {
                            //mButtonRestart.setVisibility(View.VISIBLE);
                            mButtonRetry!!.visibility = VISIBLE
                            mTimerView!!.visibility = INVISIBLE
                            mTextView!!.visibility = VISIBLE
                            Log.d(TAG, "the total was $mHitTotal")
                            if (mHitTotal >= SUCCESS_THRESHOLD) {
                                mTextView!!.setText(R.string.winText)
                            } else {
                                mTextView!!.text = ("Sorry, You Lose! You got " + mHitTotal
                                    + ". You need 50 to win.")
                            }
                            mTimerView!!.text = "1:12"
                            mTextView!!.height = 20
                        }
                    } //end handle msg
                })
        }
        isFocusable = true // make sure we get key events
        Log.d(TAG, "@@@ done creating view!")
    }

    /**
     * used to calculate level for mutes and trigger clip
     */
    var mHitStreak: Int = 0

    /**
     * total number of asteroids you have hit.
     */
    var mHitTotal: Int = 0

    /**
     * which music bed is currently playing?
     */
    var mCurrentBed: Int = 0

    /**
     * a lazy graphic fudge for the initial title splash
     */
    private var mTitleBG: Bitmap? = null

    /**
     * Background `Bitmap` used once we start playing (or have lost)
     */
    private var mTitleBG2: Bitmap? = null

    /**
     * Base class for any external event passed to the JetBoyThread. This can
     * include user input, system events, network input, etc.
     */
    open class GameEvent {
        /**
         * Current system time when we were constructed.
         */
        var eventTime: Long

        /**
         * Our constructor, we just initialize our field `long eventTime` to the current
         * system time in milliseconds.
         */
        init {
            eventTime = System.currentTimeMillis()
        }
    }

    /**
     * A GameEvent subclass for key based user input. Values are those used by the standard onKey,
     * with Simple constructor to make populating this event easier. It just saves its parameters in
     * its fields.
     *
     * @param keyCode the [KeyEvent] keycode returned by the [KeyEvent.getKeyCode] method
     * of the [KeyEvent] parameter [msg]
     * @param up      `true` if it was a key up event, `false` if it was a key down event.
     * @param msg     [KeyEvent] Description of the key event.
     */
    class KeyGameEvent(
        /**
         * The [KeyEvent] keycode that we represent.
         */
        var keyCode: Int,
        /**
         * `true` if it was a key up event, `false` if it was a key down event.
         */
        var up: Boolean,
        /**
         * [KeyEvent] Description of our key event.
         */
        var msg: KeyEvent
        ) : GameEvent()

    /**
     * A GameEvent subclass for events from the JetPlayer. It uses a Simple constructor to make
     * populating this event easier which just saves its parameters in our fields.
     *
     * @param player     the JET player the status update is coming from
     * @param segment    8 bit unsigned value
     * @param track      6 bit unsigned value
     * @param channel    4 bit unsigned value
     * @param controller 7 bit unsigned value
     * @param value      7 bit unsigned value
     */
    internal class JetGameEvent(
        /**
         * the JET player the status update is coming from
         */
        var player: JetPlayer,
        /**
         * 8 bit unsigned value
         */
        var segment: Short,
        /**
         * 6 bit unsigned value
         */
        var track: Byte,
        /**
         * 4 bit unsigned value
         */
        var channel: Byte,
        /**
         * 7 bit unsigned value
         */
        var controller: Byte,
        /**
         * 7 bit unsigned value
         */
        var value: Byte) : GameEvent()

    /**
     * JET info: the JetBoyThread receives all the events from the JET player
     * JET info: through the OnJetEventListener interface.
     *
     * This is the constructor for the main worker bee. Its [SurfaceHolder] parameter [mSurfaceHolder]
     * becomes its [SurfaceHolder] field [mSurfaceHolder], its [Context] parameter [mContext] becomes
     * its [Context] field [mContext], and its [Handler] parameter [mHandler] becomes its [Handler]
     * field [mHandler].
     *
     * In its `init` block it initializes its [Resources] field [mRes] with a [Resources] instance
     * for its [Context] field [mContext]. We initialize our mute [Array] of [BooleanArray] field
     * [muteMask] (associated with the music beds in the JET file) to their appropriate `true` or
     * `false` value. We set our state to [STATE_START], and call our method [setInitialGameState]
     * to set up all our initial JET requirements (including loading the JET file). We initialize
     * [Bitmap] field [mTitleBG] by decoding the png with resource id `R.drawable.title_hori`,
     * initialize [Bitmap] field [mBackgroundImageFar] by decoding the png with resource id
     * `R.drawable.background_a`, initialize [Bitmap] field [mLaserShot] by decoding the png with
     * resource id `R.drawable.laser`, initialize [Bitmap] field [mBackgroundImageNear] by decoding
     * the png with resource id `R.drawable.background_b`, initialize the four bitmaps used
     * by [Array] of [Bitmap] field [mShipFlying] by decoding the png's with resource id
     * `R.drawable.ship2_1`, `R.drawable.ship2_2`, `R.drawable.ship2_3`, and `R.drawable.ship2_4`,
     * initialize the four bitmaps used by [Array] of [Bitmap] field [mBeam] by decoding the png's
     * with resource id `R.drawable.intbeam_1`, `R.drawable.intbeam_2`, `R.drawable.intbeam_3`, and
     * `R.drawable.intbeam_4`, initialize [Bitmap] field [mTimerShell] by decoding the png with
     * resource id `R.drawable.int_timer`, initialize [Array] of [Bitmap] field [mAsteroids] with
     * the 12 png's with resource id's `R.drawable.asteroid01` through `R.drawable.asteroid12`, and
     * initialize [Array] of [Bitmap] field [mExplosions] with the 4 png's with resource id's
     * `R.drawable.asteroid_explode1` through `R.drawable.asteroid_explode4`.
     *
     * @param mSurfaceHolder [SurfaceHolder] we are to use
     * @param mContext       The [Context] our view is running in, used to access resources.
     * @param mHandler       [Handler] that is handling our thread which [JetBoyView] will use to
     * send us messages.
     */
    open inner class JetBoyThread(
        /**
         * Handle to the underlying [SurfaceHolder] of the [SurfaceView] we interact with
         */
        private val mSurfaceHolder: SurfaceHolder,
        /**
         * Handle to the application context, used to e.g. fetch Drawables.
         */
        private val mContext: Context,
        /**
         * Message handler used by thread to interact with TextView
         */
        private val mHandler: Handler
    ) : Thread(), OnJetEventListener {
        /**
         * Has our [setInitialGameState] method been called to initialize the game state?
         */
        var mInitialized: Boolean = false

        /**
         * Queue for GameEvents
         */
        var mEventQueue: ConcurrentLinkedQueue<GameEvent> = ConcurrentLinkedQueue<GameEvent>()

        /**
         * Context for processKey to maintain state across frames
         */
        protected var mKeyContext: Any? = null

        /**
         * the timer display in seconds
         */
        var mTimerLimit: Int = 0

        /**
         * used for internal timing logic.
         */
        val TIMER_LIMIT: Int = 72

        /**
         * string value for timer display
         */
        private var mTimerValue = "1:12"

        /**
         * [STATE_START], [STATE_PLAY], [STATE_RUNNING], [STATE_PAUSE], and [STATE_LOSE] are the
         * states we use
         */
        var mState: Int

        /**
         * has laser been fired, used for fx logic on laser fire
         */
        var mLaserOn: Boolean = false

        /**
         * how long has laser been fired, used for fx logic on laser fire
         */
        var mLaserFireTime: Long = 0

        /**
         * The drawable to use as the far background of the animation canvas
         */
        private var mBackgroundImageFar: Bitmap

        /**
         * The drawable to use as the close background of the animation canvas
         */
        private var mBackgroundImageNear: Bitmap

        /**
         * JET info: Event ID within the JET file. In this game 80 is used for sending asteroid
         * across the screen 82 is used as game time for 1/4 note beat.
         */
        private val NEW_ASTEROID_EVENT: Byte = 80

        /**
         * JET info: Event ID within the JET file. In this game 82 is used as game time for
         * 1/4 note beat.
         */
        private val TIMER_EVENT: Byte = 82

        /**
         * used to track beat for synch of mute/un-mute actions
         */
        private var mBeatCount = 1

        /**
         * our intrepid space boy
         */
        private val mShipFlying: Array<Bitmap?> = arrayOfNulls(4)

        /**
         * the twinkly bit
         */
        private val mBeam: Array<Bitmap?> = arrayOfNulls(4)

        /**
         * the things you are trying to hit
         */
        val mAsteroids: Array<Bitmap?> = arrayOfNulls(12)

        /**
         * hit animation
         */
        val mExplosions: Array<Bitmap?> = arrayOfNulls(4)

        /**
         * Contains the png with resource id `R.drawable.int_timer` which is used to decorate our
         * timer [TextView]
         */
        private val mTimerShell: Bitmap

        /**
         * Contains the png with resource id `R.drawable.laser` which is used when the laser is fired.
         */
        private val mLaserShot: Bitmap

        /**
         * used to save the beat event system time.
         */
        private var mLastBeatTime: Long = 0

        /**
         * Current system time which is set in our "worker bee" `run` override but never used.
         */
        private var mPassedTime: Long = 0

        /**
         * how much do we move the asteroids per beat, in pixels.
         */
        private val mPixelMoveX = 25

        /**
         * The asteroid send events are generated from the Jet File, but which land they start in is
         * random.
         */
        private val mRandom = Random()

        /**
         * JET info: the star of our show, a reference to the JetPlayer object.
         */
        private var mJet: JetPlayer? = null

        /**
         * The JET segment queue is playing
         */
        private var mJetPlaying = false

        /**
         * Indicate whether the surface has been created & is ready to draw, set to true in our
         * [surfaceCreated] override by calling the [setRunning] method of `thread` with `true`,
         * set to `false` in our [surfaceDestroyed] override.
         */
        private var mRun = false

        /**
         * [Timer] task queue, updates the screen clock. Also used for tempo timing.
         */
        private var mTimer: Timer? = null

        /**
         * Task used to update the timer every 1000ms.
         */
        private var mTimerTask: TimerTask? = null

        /**
         * one second - used to update timer
         */
        private val mTaskIntervalInMillis = 1000

        /**
         * Current height of the surface/canvas.
         *
         * @see setSurfaceSize
         */
        private var mCanvasHeight = 1

        /**
         * Current width of the surface/canvas.
         *
         * @see setSurfaceSize
         */
        private var mCanvasWidth = 1

        /**
         * used to track the picture to draw for ship animation
         */
        private var mShipIndex = 0

        /**
         * stores all of the [Asteroid] objects in order
         */
        private var mDangerWillRobinson: Vector<Asteroid>? = null

        /**
         * stores all of the [Explosion] objects in order, [Asteroid] objects are replaced
         * with an [Explosion] object in the same position when the laser hits the asteroid.
         */
        private var mExplosion: Vector<Explosion>? = null
        // right to left scroll tracker for near and far BG
        /**
         * right to left scroll tracker for far BG
         */
        private var mBGFarMoveX: Int = 0

        /**
         * right to left scroll tracker for near BG
         */
        private var mBGNearMoveX: Int = 0

        /**
         * how far up (close to top) jet boy can fly
         */
        private val mJetBoyYMin: Int = 40

        /**
         * X position of jet boy
         */
        private val mJetBoyX: Int = 0

        /**
         * Y position of jet boy
         */
        private var mJetBoyY: Int = 0

        /**
         * this is the pixel X position of the laser beam guide.
         */
        private val mAsteroidMoveLimitX: Int = 110

        /**
         * how far up asteroid can be painted
         */
        private val mAsteroidMinY: Int = 40

        /**
         * [Resources] instance for the [Context] our view is running in.
         */
        var mRes: Resources

        /**
         * [Array] of [BooleanArray] to store the mute masks that are applied during game play to
         * respond to the player's hit streaks
         */
        private val muteMask: Array<BooleanArray> = Array(9) { BooleanArray(32) }

        /**
         * See comments of constructor (way up above) for info.
         */
        init {
            mRes = mContext.resources

            // JET info: this are the mute arrays associated with the music beds in the
            // JET info: JET file
            for (ii in 0..7) {
                for (xx in 0..31) {
                    muteMask[ii][xx] = true
                }
            }
            muteMask[0][2] = false
            muteMask[0][3] = false
            muteMask[0][4] = false
            muteMask[0][5] = false
            muteMask[1][2] = false
            muteMask[1][3] = false
            muteMask[1][4] = false
            muteMask[1][5] = false
            muteMask[1][8] = false
            muteMask[1][9] = false
            muteMask[2][2] = false
            muteMask[2][3] = false
            muteMask[2][6] = false
            muteMask[2][7] = false
            muteMask[2][8] = false
            muteMask[2][9] = false
            muteMask[3][2] = false
            muteMask[3][3] = false
            muteMask[3][6] = false
            muteMask[3][11] = false
            muteMask[3][12] = false
            muteMask[4][2] = false
            muteMask[4][3] = false
            muteMask[4][10] = false
            muteMask[4][11] = false
            muteMask[4][12] = false
            muteMask[4][13] = false
            muteMask[5][2] = false
            muteMask[5][3] = false
            muteMask[5][10] = false
            muteMask[5][12] = false
            muteMask[5][15] = false
            muteMask[5][17] = false
            muteMask[6][2] = false
            muteMask[6][3] = false
            muteMask[6][14] = false
            muteMask[6][15] = false
            muteMask[6][16] = false
            muteMask[6][17] = false
            muteMask[7][2] = false
            muteMask[7][3] = false
            muteMask[7][6] = false
            muteMask[7][14] = false
            muteMask[7][15] = false
            muteMask[7][16] = false
            muteMask[7][17] = false
            muteMask[7][18] = false

            // set all tracks to play
            for (xx in 0..31) {
                muteMask[8][xx] = false
            }

            // always set state to start, ensure we come in from front door if
            // app gets tucked into background
            mState = STATE_START
            setInitialGameState()
            mTitleBG = BitmapFactory.decodeResource(mRes, R.drawable.title_hori)

            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this
            // way...thanks lunar lander :)

            // two background since we want them moving at different speeds
            mBackgroundImageFar = BitmapFactory.decodeResource(mRes, R.drawable.background_a)
            mLaserShot = BitmapFactory.decodeResource(mRes, R.drawable.laser)
            mBackgroundImageNear = BitmapFactory.decodeResource(mRes, R.drawable.background_b)
            mShipFlying[0] = BitmapFactory.decodeResource(mRes, R.drawable.ship2_1)
            mShipFlying[1] = BitmapFactory.decodeResource(mRes, R.drawable.ship2_2)
            mShipFlying[2] = BitmapFactory.decodeResource(mRes, R.drawable.ship2_3)
            mShipFlying[3] = BitmapFactory.decodeResource(mRes, R.drawable.ship2_4)
            mBeam[0] = BitmapFactory.decodeResource(mRes, R.drawable.intbeam_1)
            mBeam[1] = BitmapFactory.decodeResource(mRes, R.drawable.intbeam_2)
            mBeam[2] = BitmapFactory.decodeResource(mRes, R.drawable.intbeam_3)
            mBeam[3] = BitmapFactory.decodeResource(mRes, R.drawable.intbeam_4)
            mTimerShell = BitmapFactory.decodeResource(mRes, R.drawable.int_timer)

            // I wanted them to rotate in a certain way
            // so I loaded them backwards from the way created.
            mAsteroids[11] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid01)
            mAsteroids[10] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid02)
            mAsteroids[9] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid03)
            mAsteroids[8] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid04)
            mAsteroids[7] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid05)
            mAsteroids[6] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid06)
            mAsteroids[5] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid07)
            mAsteroids[4] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid08)
            mAsteroids[3] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid09)
            mAsteroids[2] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid10)
            mAsteroids[1] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid11)
            mAsteroids[0] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid12)
            mExplosions[0] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid_explode1)
            mExplosions[1] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid_explode2)
            mExplosions[2] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid_explode3)
            mExplosions[3] = BitmapFactory.decodeResource(mRes, R.drawable.asteroid_explode4)
        }

        /**
         * Does the grunt work of setting up initial jet requirements. We use the factory method
         * [JetPlayer.getJetPlayer] of [JetPlayer] to create a [JetPlayer] instance which we save
         * in our [JetPlayer] field [mJet], set our [Boolean] field [mJetPlaying] to `false`,
         * clear the queue of [mJet], set its [OnJetEventListener] to 'this', and load [mJet] with
         * the raw resources JET file with resource id `R.raw.level1`. We then set our [Int] field
         * [mCurrentBed] to 0, and initialize [Byte] variable `val sSegmentID` to 0. We call the
         * [JetPlayer.queueJetSegment] method of [mJet] to queue up segment 0, using 0 as the index
         * of the sound bank associated with the segment, with 0 as the repeat count (plays only
         * once), 0 for the amount of pitch transposition (normal playback), 0 as the bitmask to
         * specify which MIDI tracks will be muted during playback (none), and `sSegmentID` to
         * uniquely identify the segment. We then call the [JetPlayer.queueJetSegment] method of
         * [mJet] to queue up segment 1, using 0 as the index of the sound bank associated with the
         * segment, with 4 as the repeat count (plays four times), 0 for the amount of pitch
         * transposition (normal playback), 0 as the bitmask to specify which MIDI tracks will be
         * muted during playback (none), and `sSegmentID` to uniquely identify the segment. We again
         * call the [JetPlayer.queueJetSegment] method of [mJet] to queue up segment 1 only this
         * time use 1 for the amount of pitch transposition (up an octave). We then call the
         * [JetPlayer.setMuteArray] method of [mJet] to set the mute flags for the current active
         * segment to the [BooleanArray] at index 0 of [Array] of [BooleanArray] field [muteMask],
         * with `true` for the sync flag so that the mute flags will be updated at the start of the
         * next segment.
         */
        private fun initializeJetPlayer() {

            // JET info: let's create our JetPlayer instance using the factory.
            // JET info: if we already had one, the same singleton is returned.
            mJet = JetPlayer.getJetPlayer()
            mJetPlaying = false

            // JET info: make sure we flush the queue,
            // JET info: otherwise left over events from previous game play can hang around.
            // JET info: ok, here we don't really need that but if you ever reuse a JetPlayer
            // JET info: instance, clear the queue before reusing it, this will also clear any
            // JET info: trigger clips that have been triggered but not played yet.
            mJet!!.clearQueue()

            // JET info: we are going to receive in this example all the JET callbacks
            // JET info: in this animation thread object.
            mJet!!.setEventListener(this)
            Log.d(TAG, "opening jet file")

            // JET info: load the actual JET content the game will be playing,
            // JET info: it's stored as a raw resource in our APK, and is labeled "level1"
            mJet!!.loadJetFile(mContext.resources.openRawResourceFd(R.raw.level1))
            // JET info: if our JET file was stored on the sdcard for instance, we would have used
            // JET info: mJet.loadJetFile("/sdcard/level1.jet");
            Log.d(TAG, "opening jet file DONE")
            mCurrentBed = 0
            val sSegmentID: Byte = 0
            Log.d(TAG, " start queuing jet file")

            // JET info: now we're all set to prepare queuing the JET audio segments for the game.
            // JET info: in this example, the game uses segment 0 for the duration of the game play,
            // JET info: and plays segment 1 several times as the exit music, so we're going to
            // JET info: queue everything upfront, but with more complex JET compositions, we could
            // JET info: also queue the segments during the game play.

            // JET info: this is the main game play music
            // JET info: it is located at segment 0
            // JET info: it uses the first DLS lib in the .jet resource, which is at index 0
            // JET info: index -1 means no DLS
            mJet!!.queueJetSegment(
                /* segmentNum = */ 0,
                /* libNum = */ 0,
                /* repeatCount = */ 0,
                /* transpose = */ 0,
                /* muteFlags = */ 0,
                /* userID = */ sSegmentID
            )

            // JET info: end game music, loop 4 times normal pitch
            mJet!!.queueJetSegment(
                /* segmentNum = */ 1,
                /* libNum = */ 0,
                /* repeatCount = */ 4,
                /* transpose = */ 0,
                /* muteFlags = */ 0,
                /* userID = */ sSegmentID
            )

            // JET info: end game music loop 4 times up an octave
            mJet!!.queueJetSegment(
                /* segmentNum = */ 1,
                /* libNum = */ 0,
                /* repeatCount = */ 4,
                /* transpose = */ 1,
                /* muteFlags = */ 0,
                /* userID = */ sSegmentID
            )

            // JET info: set the mute mask as designed for the beginning of the game, when the
            // JET info: the player hasn't scored yet.
            mJet!!.setMuteArray(muteMask[0], true)
            Log.d(TAG, " start queuing jet file DONE")
        }

        /**
         * Called from the [Thread.run] override of [JetBoyThread] to dispatch the drawing of our
         * [SurfaceView] to the appropriate method depending on the game state. We when branch
         * depending on the value of our [Int] state field [mState]:
         *
         *  * [STATE_RUNNING]: we call our method [doDrawRunning] to do whatever drawing is
         *  appropriate for a running game at this time
         *
         *  * [STATE_START]: we call our method [doDrawReady] to do whatever drawing is appropriate
         *  to do before the game has started.
         *
         *  * [STATE_PLAY], or [STATE_LOSE]: If our [Bitmap] field [mTitleBG2] is `null` we
         *  initialize it by decoding the png with resource id `R.drawable.title_bg_hori`, then
         *  call our method [doDrawPlay] to do whatever drawing is appropriate while we wait for
         *  the user to press the "PLAY!" button.
         *
         * @param canvas [Canvas] on which to do our drawing.
         */
        private fun doDraw(canvas: Canvas?) {
            when (mState) {
                STATE_RUNNING -> {
                    doDrawRunning(canvas)
                }
                STATE_START -> {
                    doDrawReady(canvas)
                }
                STATE_PLAY, STATE_LOSE -> {
                    if (mTitleBG2 == null) {
                        mTitleBG2 = BitmapFactory.decodeResource(mRes, R.drawable.title_bg_hori)
                    }
                    doDrawPlay(canvas)
                }
            }
        }

        /**
         * Draws current state of the running game on its [Canvas] parameter [canvas]. First we
         * decrement the X position of the far background [Int] field [mBGFarMoveX] by 1, and the
         * X position of the near background [Int] field [mBGNearMoveX] by 4. We initialize [Int]
         * variable `val newFarX` to the width of the far background bitmap, [Bitmap] field
         * [mBackgroundImageFar] minus the negative of [mBGFarMoveX] (this is the wrap factor), and
         * if `newFarX` is less than or equal to 0 we have scrolled all the way so we set
         * [mBGFarMoveX] to 0 and use the [Canvas.drawBitmap] method of [canvas] to draw
         * [mBackgroundImageFar] at [mBGFarMoveX], otherwise we have to have it draw
         * [mBackgroundImageFar] twice, once at [mBGFarMoveX], and again at `newFarX`. We do much
         * the same thing for [mBackgroundImageNear]: initialize [Int] variable `val newNearX` to
         * the width of the near background bitmap, [Bitmap] field [mBackgroundImageNear] minus
         * negative [mBGNearMoveX] and if it is less than or equal to zero we set [mBGNearMoveX]
         * to 0 and use the [Canvas.drawBitmap] method of [canvas] to draw [mBackgroundImageNear]
         * at [mBGNearMoveX], otherwise we have it draw it twice, once at [mBGNearMoveX] and once
         * at `newNearX`. Next we call our method [doAsteroidAnimation] to draw the asteroids, and
         * use the [Canvas.drawBitmap] method of [canvas] to draw the [Bitmap] at index [mShipIndex]
         * in [Array] of [Bitmap] field [mBeam] (the vertical laser beam(?) in front of the ship)
         * after which we increment [mShipIndex] and wrap it around to 0 if it has reached 4. We
         * then use the [Canvas.drawBitmap] method of [canvas] to draw the [Bitmap] at index
         * [mShipIndex] in [Array] of [Bitmap] field [mShipFlying] at the location
         * ([mJetBoyX], [mJetBoyY]) which is the same lane as the next asteroid. If the laser is
         * on ([mLaserOn] is `true`) we have [canvas] draw [Bitmap] field [mLaserShot]. Finally we
         * have [canvas] draw [Bitmap] field [mTimerShell] in its correct position.
         *
         * @param canvas [Canvas] on which to do our drawing.
         */
        private fun doDrawRunning(canvas: Canvas?) {

            // decrement the far background
            mBGFarMoveX -= 1

            // decrement the near background
            mBGNearMoveX -= 4

            // calculate the wrap factor for matching image draw
            val newFarX = mBackgroundImageFar.width - -mBGFarMoveX

            // if we have scrolled all the way, reset to start
            if (newFarX <= 0) {
                mBGFarMoveX = 0
                // only need one draw
                canvas!!.drawBitmap(
                    /* bitmap = */ mBackgroundImageFar,
                    /* left = */ mBGFarMoveX.toFloat(),
                    /* top = */ 0f,
                    /* paint = */ null
                )
            } else {
                // need to draw original and wrap
                canvas!!.drawBitmap(
                    /* bitmap = */ mBackgroundImageFar,
                    /* left = */ mBGFarMoveX.toFloat(),
                    /* top = */ 0f,
                    /* paint = */ null
                )
                canvas.drawBitmap(
                    /* bitmap = */ mBackgroundImageFar,
                    /* left = */ newFarX.toFloat(),
                    /* top = */ 0f,
                    /* paint = */ null
                )
            }

            // same story different image...
            // TODO possible method call
            val newNearX = mBackgroundImageNear.width - -mBGNearMoveX
            if (newNearX <= 0) {
                mBGNearMoveX = 0
                canvas.drawBitmap(
                    /* bitmap = */ mBackgroundImageNear,
                    /* left = */ mBGNearMoveX.toFloat(),
                    /* top = */ 0f,
                    /* paint = */ null
                )
            } else {
                canvas.drawBitmap(
                    /* bitmap = */ mBackgroundImageNear,
                    /* left = */ mBGNearMoveX.toFloat(),
                    /* top = */ 0f,
                    /* paint = */ null
                )
                canvas.drawBitmap(
                    /* bitmap = */ mBackgroundImageNear,
                    /* left = */ newNearX.toFloat(),
                    /* top = */ 0f,
                    /* paint = */ null
                )
            }
            doAsteroidAnimation(canvas)
            canvas.drawBitmap(
                /* bitmap = */ mBeam[mShipIndex]!!,
                /* left = */ (51 + 20).toFloat(),
                /* top = */ 0f,
                /* paint = */ null
            )
            mShipIndex++
            if (mShipIndex == 4) mShipIndex = 0

            // draw the space ship in the same lane as the next asteroid
            canvas.drawBitmap(
                /* bitmap = */ mShipFlying[mShipIndex]!!,
                /* left = */ mJetBoyX.toFloat(),
                /* top = */ mJetBoyY.toFloat(),
                /* paint = */ null
            )
            if (mLaserOn) {
                canvas.drawBitmap(
                    /* bitmap = */ mLaserShot,
                    /* left = */ (mJetBoyX + mShipFlying[0]!!.width).toFloat(),
                    /* top = */ (mJetBoyY + mShipFlying[0]!!.height / 2).toFloat(),
                    /* paint = */ null
                )
            }

            // tick tock
            canvas.drawBitmap(
                /* bitmap = */ mTimerShell,
                /* left = */ (mCanvasWidth - mTimerShell.width).toFloat(),
                /* top = */ 0f,
                /* paint = */ null
            )
        }

        /**
         * Sets the game variables to the values they have in the initial state. First we initialize
         * [Int] field [mTimerLimit] (the amount of time remaining) to [TIMER_LIMIT] (72), and set
         * [Int] field [mJetBoyY] (the Y position of the spaceship) to [Int] field [mJetBoyYMin]
         * (the top position for it). We then call our method [initializeJetPlayer] to set up the
         * JET interface. We initialize [Timer] field [mTimer] with a new instance, [Vector] of
         * [Asteroid] field [mDangerWillRobinson] with a new instance, and [Vector] of [Explosion]
         * field [mExplosion] with a new instance. We then set [Boolean] field [mInitialized] to
         * `true`, [Int] field [mHitStreak] to 0, and [Int] field [mHitTotal] to 0.
         */
        private fun setInitialGameState() {
            mTimerLimit = TIMER_LIMIT
            mJetBoyY = mJetBoyYMin

            // set up jet stuff
            initializeJetPlayer()
            mTimer = Timer()
            mDangerWillRobinson = Vector()
            mExplosion = Vector()
            mInitialized = true
            mHitStreak = 0
            mHitTotal = 0
        }

        /**
         * Draws our asteroids and explosions. If our [Vector] of [Asteroid] list of asteroids field
         * [mDangerWillRobinson] is `null` or has no entries and our [Vector] of [Explosion] list of
         * explosions field [mExplosion] is not `null` and has no entries we just return having done
         * nothing. Otherwise we initialize [Long] variable `val frameDelta` to the current system
         * time in milliseconds minus [Long] field [mLastBeatTime], and [Int] variable `val animOffset`
         * to [ANIMATION_FRAMES_PER_BEAT] times `frameDelta` divided by 428.
         *
         * Then we loop backwards on [Int] variable `var i` for each of the [Asteroid] variable
         * `val asteroid` in [Vector] of [Asteroid] field [mDangerWillRobinson]. If the
         * [Asteroid.mMissed] property of the current `asteroid` is `false` we set [Int] field
         * [mJetBoyY] to the [Asteroid.mDrawY] field of `asteroid` then draw the bitmap chosen from
         * [Array] of [Bitmap] field [mAsteroids] by adding `animOffset` to the [Asteroid.mAniIndex]
         * field of `asteroid` modulo the length of [mAsteroids] at the location given by the values
         * of the [Asteroid.mDrawX] and [Asteroid.mDrawY] fields of `asteroid`.
         *
         * Next we loop backwards on [Int] variable `var i` for each of the [Explosion] variable
         * `val ex` in [Vector] of [Explosion] field [mExplosion], drawing the bitmap chosen from
         * [Array] of [Bitmap] field [mExplosions] by adding `animOffset` to the [Explosion.mAniIndex]
         * field of `ex` modulo the length of [mExplosions] at the location given by the values of
         * the [Explosion.mDrawX] and [Explosion.mDrawY] fields of `ex`.
         *
         * @param canvas [Canvas] on which to do our drawing.
         */
        private fun doAsteroidAnimation(canvas: Canvas?) {
            @Suppress("UsePropertyAccessSyntax") // TODO: Watch this! Using property access iw flaggged as error.
            if ((mDangerWillRobinson == null || mDangerWillRobinson!!.isEmpty())
                && (mExplosion != null && mExplosion!!.isEmpty())) return

            // Compute what percentage through a beat we are and adjust
            // animation and position based on that. This assumes 140bpm(428ms/beat).
            // This is just inter-beat interpolation, no game state is updated
            val frameDelta: Long = System.currentTimeMillis() - mLastBeatTime
            val animOffset: Int = (ANIMATION_FRAMES_PER_BEAT * frameDelta / 428).toInt()
            for (i in mDangerWillRobinson!!.size - 1 downTo 0) {
                val asteroid: Asteroid = mDangerWillRobinson!!.elementAt(i)
                if (!asteroid.mMissed) mJetBoyY = asteroid.mDrawY

                // Log.d(TAG, " drawing asteroid " + ii + " at " +
                // asteroid.mDrawX );
                canvas!!.drawBitmap(
                    mAsteroids[(asteroid.mAniIndex + animOffset) % mAsteroids.size]!!,
                    asteroid.mDrawX.toFloat(), asteroid.mDrawY.toFloat(), null)
            }
            for (i in mExplosion!!.size - 1 downTo 0) {
                val ex: Explosion = mExplosion!!.elementAt(i)
                canvas!!.drawBitmap(mExplosions[(ex.mAniIndex + animOffset) % mExplosions.size]!!,
                    ex.mDrawX.toFloat(), ex.mDrawY.toFloat(), null)
            }
        }

        /**
         * Draws [Bitmap] field [mTitleBG] on the [Canvas] passed it (this is the bitmap we use
         * for the [STATE_START] game state).
         *
         * @param canvas [Canvas] on which to do our drawing.
         */
        private fun doDrawReady(canvas: Canvas?) {
            canvas!!.drawBitmap(mTitleBG!!, 0f, 0f, null)
        }

        /**
         * Draws [Bitmap] field [mTitleBG2] on the [Canvas] passed it (this is the bitmap we use
         * for the [STATE_PLAY] and [STATE_LOSE] game states).
         *
         * @param canvas [Canvas] on which to do our drawing.
         */
        private fun doDrawPlay(canvas: Canvas?) {
            canvas!!.drawBitmap(mTitleBG2!!, 0f, 0f, null)
        }

        /**
         * The heart of the worker bee, started by a call to our super's [Thread.start] method in
         * our [surfaceCreated] override. We loop while our [Boolean] field [mRun] is `true`,
         * branching on the value of our [Int] field [mState] state variable:
         *
         *  * [STATE_RUNNING]: First we call our [updateGameState] method to process any input and
         *  apply it to the game state. If [Boolean] field [mJetPlaying] is `false` we set [Boolean]
         *  field [mInitialized] to `false`, call the [JetPlayer.play] method of [JetPlayer] field
         *  [mJet] to start playing the JET segment queue, and then set [mJetPlaying] to `true`. Now
         *  that we know that the JET midi is playing we set [Long] field [mPassedTime] to the
         *  current system time in milliseconds. If [TimerTask] field [mTimerTask] is `null` (the
         *  timer task is not running yet) we set it to an anonymous class whose [TimerTask.run]
         *  override calls our [doCountDown] method and calls the [Timer.schedule] method of [Timer]
         *  field [mTimer] to schedule [mTimerTask] to run after a delay of [mTaskIntervalInMillis]
         *  milliseconds.
         *
         *  * [STATE_PLAY] and our [Boolean] field [mInitialized] is `false`: we call our
         *  [setInitialGameState] method to initialize our variables to start playing.
         *
         *  * [STATE_LOSE]: we set [Boolean] field [mInitialized] to `false`.
         *
         * Next wrapped in a try block whose finally block calls the [SurfaceHolder.unlockCanvasAndPost]
         * method of [SurfaceHolder] field [mSurfaceHolder] to finish editing pixels in the surface
         * in case an exception is thrown: we set [Canvas] variable `c` to the canvas returned by
         * the [SurfaceHolder.lockCanvas] method of [mSurfaceHolder] (starts editing the pixels in
         * the surface, the returned [Canvas] can be used to draw into the surface's bitmap) then
         * call our [doDraw] method with `c` to have it do whatever drawing is called for.
         */
        override fun run() {
            // while running do stuff in this loop...buzz!
            while (mRun) {
                var c: Canvas? = null
                if (mState == STATE_RUNNING) {
                    // Process any input and apply it to the game state
                    updateGameState()
                    if (!mJetPlaying) {
                        mInitialized = false
                        Log.d(TAG, "------> STARTING JET PLAY")
                        mJet!!.play()
                        mJetPlaying = true
                    }
                    mPassedTime = System.currentTimeMillis()

                    // kick off the timer task for counter update if not already
                    // initialized
                    if (mTimerTask == null) {
                        mTimerTask = object : TimerTask() {
                            override fun run() {
                                doCountDown()
                            }
                        }
                        mTimer!!.schedule(mTimerTask, mTaskIntervalInMillis.toLong())
                    } // end of TimerTask init block
                } // end of STATE_RUNNING block
                else if (mState == STATE_PLAY && !mInitialized) {
                    setInitialGameState()
                } else if (mState == STATE_LOSE) {
                    mInitialized = false
                }
                try {
                    c = mSurfaceHolder.lockCanvas(null)
                    // synchronized (mSurfaceHolder) {
                    doDraw(c)
                    // }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c)
                    }
                } // end finally block
            } // end while mRun block
        }

        /**
         * This method handles updating the model of the game state. No rendering is done here only
         * processing of inputs and update of state. This includes positions of all game objects
         * (asteroids, player, explosions), their state (animation frame, hit), creation of new
         * objects, etc. We loop until setting [GameEvent] variable `val event` to the [GameEvent]
         * that the [ConcurrentLinkedQueue.poll] method of [ConcurrentLinkedQueue] of [GameEvent]
         * field [mEventQueue] results in `null` at which point we `break` (and return). We branch
         * on whether `event` is an instance of:
         *
         *  * [KeyGameEvent]: We set our [Any] field [mKeyContext] to the object returned by our
         *  method [processKeyEvent] when passed `event` and our old value of [mKeyContext] (if it
         *  is an 'up' event it returns `null`, otherwise it returns `event`). We then call our
         *  [updateLaser] method with [mKeyContext] as its argument to update the laser state.
         *
         *  * [JetGameEvent]: We initialize [JetGameEvent] variable `val jetEvent` to `event` and if
         *  the [JetGameEvent.value] field of `event` is a [TIMER_EVENT] we set our [Long] field
         *  [mLastBeatTime] (time of the last beat) to the current system time in milliseconds, call
         *  our [updateLaser] method with our [Any] field [mKeyContext] to update the laser state,
         *  call our [updateExplosions] method with our field [mKeyContext] to update the explosions
         *  state, and call our [updateAsteroids] method with our field [mKeyContext] to update the
         *  asteroid positions, hit status and animations. Whether or not it was a [TIMER_EVENT] we
         *  call our [processJetEvent] method to process this [JetGameEvent] variable `jetEvent`.
         */
        fun updateGameState() {
            // Process any game events and apply them
            while (true) {
                val event: GameEvent = mEventQueue.poll() ?: break

                // Log.d(TAG,"*** EVENT = " + event);

                // Process keys tracking the input context to pass in to later
                // calls
                if (event is KeyGameEvent) {
                    // Process the key for affects other then asteroid hits
                    mKeyContext = processKeyEvent(event, mKeyContext)

                    // Update laser state. Having this here allows the laser to
                    // be triggered right when the key is
                    // pressed. If we comment this out the laser will only be
                    // turned on when updateLaser is called
                    // when processing a timer event below.
                    updateLaser(mKeyContext)
                } else if (event is JetGameEvent) {
                    val jetEvent = event

                    // Only update state on a timer event
                    if (jetEvent.value == TIMER_EVENT) {
                        // Note the time of the last beat
                        mLastBeatTime = System.currentTimeMillis()

                        // Update laser state, turning it on if a key has been
                        // pressed or off if it has been
                        // on for too long.
                        updateLaser(mKeyContext)

                        // Update explosions before we update asteroids because
                        // updateAsteroids may add
                        // new explosions that we do not want updated until next
                        // frame
                        updateExplosions(mKeyContext)

                        // Update asteroid positions, hit status and animations
                        updateAsteroids(mKeyContext)
                    }
                    processJetEvent(jetEvent.player, jetEvent.segment, jetEvent.track,
                        jetEvent.channel, jetEvent.controller, jetEvent.value)
                }
            }
        }

        /**
         * This method handles the state updates that can be caused by key press events. Key events
         * may mean different things depending on what has come before, to support this concept this
         * method takes an opaque context object as a parameter and returns an updated version. This
         * context should be set to `null` for the first event then should be set to the last value
         * returned for subsequent events.
         *
         * If the [KeyGameEvent.up] property of our [KeyGameEvent] parameter [event] is `true`, and
         * the [KeyGameEvent.keyCode] field is [KeyEvent.KEYCODE_DPAD_CENTER] we return `null`. If
         * the [KeyGameEvent.up] field is `false` and the [KeyGameEvent.keyCode] field is
         * [KeyEvent.KEYCODE_DPAD_CENTER] we return [event]. If it not a [KeyEvent.KEYCODE_DPAD_CENTER]
         * key code event we return [Any] parameter [context] unchanged.
         *
         * @param event [KeyGameEvent] we are processing.
         * @param context Last [KeyGameEvent] if it was a key down event or `null`
         * @return `null` if it was an up event for [KeyEvent.KEYCODE_DPAD_CENTER], or our [Any]
         * parameter [event] if it was a down event for [KeyEvent.KEYCODE_DPAD_CENTER] and the
         * previous value of [context] was `null`, or the old value of [context] if the above tests
         * all fail.
         */
        fun processKeyEvent(event: KeyGameEvent, context: Any?): Any? {
            // Log.d(TAG, "key code is " + event.keyCode + " " + (event.up ? "up":"down"));

            // If it is a key up on the fire key make sure we mute the
            // associated sound
            if (event.up) {
                if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    return null
                }
            } else {
                if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER && context == null) {
                    return event
                }
            }

            // Return the context unchanged
            return context
        }

        /**
         * This method updates the laser status based on user input and shot duration. First we set
         * [Long] variable `val keyTime` to 0 if our [Any] parameter [inputContext] is `null`, or to
         * the [GameEvent.eventTime] property of [inputContext] if it is not `null`. If [Boolean]
         * field [mLaserOn] is `true` (the laser is being fired) and the current system time in
         * milliseconds minus [Long] field [mLaserFireTime] is greater than 400 (the laser has been
         * on longer than 400ms) we set [mLaserOn] to `false` (turn it off), otherwise if the current
         * system time in milliseconds minus [mLaserFireTime] is greater than 300 we call the
         * [JetPlayer.setMuteFlag] of [JetPlayer] field [mJet] to mute the laser sound effect. If
         * [mLaserOn] is `false` and the current system time in milliseconds minus [mLaserFireTime]
         * is less than or equal to 400 we set [mLaserOn] to `true`, set [mLaserFireTime] to
         * `keyTime` and call the [JetPlayer.setMuteFlag] method of [JetPlayer] field [mJet] to
         * un-mute the laser sound effect.
         *
         * @param inputContext The [GameEvent] we are responding to, or `null`
         */
        protected fun updateLaser(inputContext: Any?) {
            // Lookup the time of the fire event if there is one
            val keyTime = if (inputContext == null) 0 else (inputContext as GameEvent).eventTime

            // Log.d(TAG,"keyTime delta = " +
            // (System.currentTimeMillis()-keyTime) + ": obj = " +
            // inputContext);

            // If the laser has been on too long shut it down
            if (mLaserOn && System.currentTimeMillis() - mLaserFireTime > 400) {
                mLaserOn = false
            } else if (System.currentTimeMillis() - mLaserFireTime > 300) {
                // JET info: the laser sound is on track 23, we mute it (true) right away (false)
                mJet!!.setMuteFlag(23, true, false)
            }

            // Now check to see if we should turn the laser on. We do this after
            // the above shutdown
            // logic so it can be turned back on in the same frame it was turned
            // off in. If we want to add a cool down period this may change.
            if (!mLaserOn && System.currentTimeMillis() - keyTime <= 400) {
                mLaserOn = true
                mLaserFireTime = keyTime

                // JET info: un-mute the laser track (false) right away (false)
                mJet!!.setMuteFlag(23, false, false)
            }
        }

        /**
         * Update asteroid state including position and laser hit status. If the list of asteroids
         * in [Vector] of [Asteroid] field [mDangerWillRobinson] is `null` or is empty we return
         * having done nothing. Otherwise we loop backwards over [Int] variable `var i` for all the
         * [Asteroid] objects in [mDangerWillRobinson]:
         *
         *  * We initialize [Asteroid] variable `val asteroid` with the `i`'th element of
         *  [mDangerWillRobinson]
         *
         *  * If the [Asteroid.mDrawX] property of `asteroid` is less than or equal to [Int] field
         *  [mAsteroidMoveLimitX] plus 20 (it is in range) and the [Asteroid.mMissed] field is `false`
         *  (the user has not fired at it while it was out of range) we check if [Boolean] field
         *  [mLaserOn] is `true` (the laser is firing) and if so we increment [Int] field [mHitStreak]
         *  and [Int] field [mHitTotal], initialize [Explosion] variable `val ex` with a new instance
         *  whose [Explosion.mDrawX] and [Explosion.mDrawY] fields are the same as `asteroid`, add
         *  it to the list of explosions in [Vector] of [Explosion] field [mExplosion], un-mute the
         *  explosion sound effect and remove element `i` from [mDangerWillRobinson] and continue
         *  executing the loop. If on the other hand the laser was not on in time we set the
         *  [Asteroid.mMissed] field of `asteroid` to `true`, and decrement [mHitStreak] (making
         *  sure it does not go below zero.
         *
         *  * If the `i`'th [Asteroid] in `asteroid` is out of range or has already been missed we
         *  subtract [Int] field [mPixelMoveX] from the [Asteroid.mDrawX] field of `asteroid` add
         *  [ANIMATION_FRAMES_PER_BEAT] to its [Asteroid.mAniIndex] field (modulo the size of the
         *  [Array] of [Bitmap] field [mAsteroids]) and if the new value of its [Asteroid.mDrawX]
         *  field is less than 0 (we have scrolled off the screen) we remove element `i` from
         *  [mDangerWillRobinson], and loop around for the next [Asteroid].
         *
         * @param inputContext unused.
         */
        @Suppress("unused")
        fun updateAsteroids(inputContext: Any?) {
            @Suppress("UsePropertyAccessSyntax") // TODO: Property access is an error?
            if ((mDangerWillRobinson == null) or (mDangerWillRobinson!!.isEmpty())) return
            for (i in mDangerWillRobinson!!.size - 1 downTo 0) {
                val asteroid = mDangerWillRobinson!!.elementAt(i)

                // If the asteroid is within laser range but not already missed
                // check if the key was pressed close enough to the beat to make a hit
                if (asteroid.mDrawX <= mAsteroidMoveLimitX + 20 && !asteroid.mMissed) {
                    // If the laser was fired on the beat destroy the asteroid
                    if (mLaserOn) {
                        // Track hit streak for adjusting music
                        mHitStreak++
                        mHitTotal++

                        // replace the asteroid with an explosion
                        val ex = Explosion()
                        ex.mAniIndex = 0
                        ex.mDrawX = asteroid.mDrawX
                        ex.mDrawY = asteroid.mDrawY
                        mExplosion!!.add(ex)
                        mJet!!.setMuteFlag(24, false, false)
                        mDangerWillRobinson!!.removeElementAt(i)

                        // This asteroid has been removed process the next one
                        continue
                    } else {
                        // Sorry, timing was not good enough, mark the asteroid
                        // as missed so on next frame it cannot be hit even if it is still
                        // within range
                        asteroid.mMissed = true
                        mHitStreak -= 1
                        if (mHitStreak < 0) mHitStreak = 0
                    }
                }

                // Update the asteroids position, even missed ones keep moving
                asteroid.mDrawX -= mPixelMoveX

                // Update asteroid animation frame
                asteroid.mAniIndex = ((asteroid.mAniIndex + ANIMATION_FRAMES_PER_BEAT)
                    % mAsteroids.size)

                // if we have scrolled off the screen
                if (asteroid.mDrawX < 0) {
                    mDangerWillRobinson!!.removeElementAt(i)
                }
            }
        }

        /**
         * This method updates explosion animation and removes them once they have completed. If the
         * list of explosions in [Vector] of [Explosion] field [mExplosion] is `null` or is empty we
         * return having done nothing. Otherwise we loop over [Int] variable `var i` backwards through
         * [mExplosion]:
         *
         *  * We initialize [Explosion] variable `val ex` with the element at `i` in [mExplosion].
         *
         *  * We add [ANIMATION_FRAMES_PER_BEAT] to the [Explosion.mAniIndex] field of `ex`.
         *
         *  * If the [Explosion.mAniIndex] field is now greater than 3 we mute the two tracks used
         *  for the explosion sound effect and remove element `i` from [mExplosion].
         *
         * @param inputContext unused.
         */
        @Suppress("unused")
        protected fun updateExplosions(inputContext: Any?) {
            @Suppress("UsePropertyAccessSyntax") // TODO: Property access is an error?
            if ((mExplosion == null) or (mExplosion!!.isEmpty())) return
            for (i in mExplosion!!.indices.reversed()) {
                val ex = mExplosion!!.elementAt(i)
                ex.mAniIndex += ANIMATION_FRAMES_PER_BEAT

                // When the animation completes remove the explosion
                if (ex.mAniIndex > 3) {
                    mJet!!.setMuteFlag(24, true, false)
                    mJet!!.setMuteFlag(23, true, false)
                    mExplosion!!.removeElementAt(i)
                }
            }
        }

        /**
         * This method handles the state updates that can be caused by JET events. If our [Byte]
         * parameter [value] is [NEW_ASTEROID_EVENT] we call our method [doAsteroidCreation] to
         * create a new asteroid. We then increment [Int] field [mBeatCount] setting it back to 1
         * if it is greater than 4 now. If [mBeatCount] is now 1 we want to change the mute array,
         * so we branch on the value of [Int] field [mHitStreak]: (doing it backwards so to fall
         * into the correct one)
         *
         *  * Greater than 28: If [Int] field [mCurrentBed] is not already equal to 7 we check
         *  whether it is less than 7 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 7, then set [mCurrentBed]
         *  to 7, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 7 immediately.
         *
         *  * Greater than 24: If [Int] field [mCurrentBed] is not already equal to 6 we check
         *  whether it is less than 6 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 6, then set [mCurrentBed]
         *  to 6, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 6 immediately.
         *
         *  * Greater than 20: If [Int] field [mCurrentBed] is not already equal to 5 we check
         *  whether it is less than 5 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 5, then set [mCurrentBed]
         *  to 5, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 5 immediately.
         *
         *  * Greater than 16: If [Int] field [mCurrentBed] is not already equal to 4 we check
         *  whether it is less than 4 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 4, then set [mCurrentBed]
         *  to 4, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 4 immediately.
         *
         *  * Greater than 12: If [Int] field [mCurrentBed] is not already equal to 3 we check
         *  whether it is less than 3 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 3, then set [mCurrentBed]
         *  to 3, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 3 immediately.
         *
         *  * Greater than 8: If [Int] field [mCurrentBed] is not already equal to 2 we check
         *  whether it is less than 2 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 2, then set [mCurrentBed]
         *  to 2, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 2 immediately.
         *
         *  * Greater than 4: If [Int] field [mCurrentBed] is not already equal to 1 we check
         *  whether it is less than 1 and if it is we call the [JetPlayer.triggerClip] method of
         *  [JetPlayer] field [mJet] to schedule the playback of clip 1, then set [mCurrentBed]
         *  to 1, and call the [JetPlayer.setMuteArray] method of [mJet] to set the array of muted
         *  tracks to the [muteMask] at index 1 immediately.
         *
         * The parameters are unused except for [Byte] parameter [value].
         *
         * @param player unused
         * @param segment unused
         * @param track unused
         * @param channel unused
         * @param controller unused
         * @param value Value of the JET event, we only care about [NEW_ASTEROID_EVENT] (create a
         * new asteroid)
         */
        @Suppress("unused")
        protected fun processJetEvent(
            player: JetPlayer?,
            segment: Short,
            track: Byte,
            channel: Byte,
            controller: Byte,
            value: Byte
        ) {
            // Check for an event that triggers a new asteroid
            if (value == NEW_ASTEROID_EVENT) {
                doAsteroidCreation()
            }
            mBeatCount++
            if (mBeatCount > 4) {
                mBeatCount = 1
            }

            // Scale the music based on progress

            // it was a game requirement to change the mute array on 1st beat of
            // the next measure when needed
            // and so we track beat count, after that we track hitStreak to
            // determine the music "intensity"
            // if the intensity has gone up, call a corresponding trigger clip, otherwise just
            // execute the rest of the music bed change logic.
            if (mBeatCount == 1) {

                // do it back wards so you fall into the correct one
                if (mHitStreak > 28) {

                    // did the bed change?
                    if (mCurrentBed != 7) {
                        // did it go up?
                        if (mCurrentBed < 7) {
                            mJet!!.triggerClip(7)
                        }
                        mCurrentBed = 7
                        // JET info: change the mute mask to update the way the music plays based
                        // JET info: on the player's skills.
                        mJet!!.setMuteArray(muteMask[7], false)
                    }
                } else if (mHitStreak > 24) {
                    if (mCurrentBed != 6) {
                        if (mCurrentBed < 6) {
                            // JET info: quite a few asteroids hit, trigger the clip with the guy's
                            // JET info: voice that encourages the player.
                            mJet!!.triggerClip(6)
                        }
                        mCurrentBed = 6
                        mJet!!.setMuteArray(muteMask[6], false)
                    }
                } else if (mHitStreak > 20) {
                    if (mCurrentBed != 5) {
                        if (mCurrentBed < 5) {
                            mJet!!.triggerClip(5)
                        }
                        mCurrentBed = 5
                        mJet!!.setMuteArray(muteMask[5], false)
                    }
                } else if (mHitStreak > 16) {
                    if (mCurrentBed != 4) {
                        if (mCurrentBed < 4) {
                            mJet!!.triggerClip(4)
                        }
                        mCurrentBed = 4
                        mJet!!.setMuteArray(muteMask[4], false)
                    }
                } else if (mHitStreak > 12) {
                    if (mCurrentBed != 3) {
                        if (mCurrentBed < 3) {
                            mJet!!.triggerClip(3)
                        }
                        mCurrentBed = 3
                        mJet!!.setMuteArray(muteMask[3], false)
                    }
                } else if (mHitStreak > 8) {
                    if (mCurrentBed != 2) {
                        if (mCurrentBed < 2) {
                            mJet!!.triggerClip(2)
                        }
                        mCurrentBed = 2
                        mJet!!.setMuteArray(muteMask[2], false)
                    }
                } else if (mHitStreak > 4) {
                    if (mCurrentBed != 1) {
                        if (mCurrentBed < 1) {
                            mJet!!.triggerClip(1)
                        }
                        mJet!!.setMuteArray(muteMask[1], false)
                        mCurrentBed = 1
                    }
                }
            }
        }

        /**
         * Creates a new randomly positioned [Asteroid] and adds it to the list of asteroids in
         * [Vector] of [Asteroid] field [mDangerWillRobinson]. First we initialize [Asteroid]
         * variable `val asteroid` with a new instance, and [Int] variable `val drawIndex` with a
         * random integer between [0,4). We set the [Asteroid.mDrawY] field of `asteroid` to
         * [Int] field [mAsteroidMinY] plus 63 times `drawIndex`, the [Asteroid.mDrawX] field to
         * [Int] field [mCanvasWidth] minus the width of the [Bitmap] at index 0 of [Array] of
         * [Bitmap] field [mAsteroids], and the [Asteroid.mStartTime] field to the current system
         * time in milliseconds. We then add `asteroid` to [mDangerWillRobinson].
         */
        private fun doAsteroidCreation() {
            // Log.d(TAG, "asteroid created");
            val asteroid = Asteroid()
            val drawIndex = mRandom.nextInt(4)

            // TODO Remove hard coded value
            asteroid.mDrawY = mAsteroidMinY + drawIndex * 63
            asteroid.mDrawX = mCanvasWidth - mAsteroids[0]!!.width
            asteroid.mStartTime = System.currentTimeMillis()
            mDangerWillRobinson!!.add(asteroid)
        }

        /**
         * Used to signal the thread whether it should be running or not. Passing `true` allows the
         * thread to run; passing `false` will shut it down if it's already running. Calling
         * [Thread.start] after this was most recently called with `false` will result in an
         * immediate shutdown. We save our [Boolean] parameter [b] in our [Boolean] field [mRun],
         * and if [mRun] is now `true` and our [TimerTask] field [mTimerTask] is not `null` we call
         * the [TimerTask.cancel] method of [mTimerTask] to cancel that timer task.
         *
         * @param b true to run, false to shut down
         */
        fun setRunning(b: Boolean) {
            mRun = b
            if (!mRun) {
                if (mTimerTask != null) mTimerTask!!.cancel()
            }
        }

        /**
         * Game State property `getter` and `setter`. Its [get] and [set] methods both synchronize
         * on our [SurfaceHolder] field [mSurfaceHolder], and its [set] method calls our method
         * [setGameState] to have it perform additional tasks associated with changing game state.
         */
        var gameState: Int
            /**
             * Returns the current [Int] value of game state as defined by state tracking constants.
             * In a block synchronized on our [SurfaceHolder] field [mSurfaceHolder] we return the
             * value of our [Int] field [mState].
             *
             * @return the current value of our [Int] games state field [mState].
             */
            get() {
                synchronized(mSurfaceHolder) { return mState }
            }
            /**
             * Sets the game mode. That is, whether we are running, paused, in the failure state,
             * in the victory state, etc. See [setGameState]. In a block synchronized on our
             * [SurfaceHolder] field [mSurfaceHolder] we call our method [setGameState] to set the
             * game state to our [Int] parameter `mode` and to do whatever else is necessary for
             * this change of game state..
             *
             * param `mode` one of [STATE_START], [STATE_PLAY], [STATE_LOSE], [STATE_PAUSE], or
             * [STATE_RUNNING]
             */
            set(mode) {
                synchronized(mSurfaceHolder) { setGameState(state = mode, message = null) }
            }

        /**
         * Sets state based on input, optionally also passing in a text message to log. First we
         * check if our [CharSequence] parameter [message] is not `null` and if so log the change
         * of state. Then in a block synchronized on our [SurfaceHolder] field [mSurfaceHolder] we
         * set our [Int] field [mState] to our [Int] parameter [state] if it is not already that
         * value. We then branch on the value of [mState]:
         *
         *  * [STATE_PLAY]: We initialize [Resources] variable `val res` with an instance for the
         *  context of our [Context] field [mContext]. We set [Bitmap] field [mBackgroundImageFar]
         *  by decoding the png with resource ID `R.drawable.background_a`, then scale it to be
         *  twice the width of our canvas and the height of our canvas and specifying that it
         *  should be filtered. We set [Bitmap] field [mBackgroundImageNear] by decoding the png
         *  with resource ID `R.drawable.background_b`, then scale it to be twice the width of our
         *  canvas and the height of our canvas, specifying that it should be filtered.
         *
         *  * [STATE_RUNNING]: We clear our [ConcurrentLinkedQueue] of [GameEvent] field [mEventQueue]
         *  (when we enter the running state we should clear any old events in the queue) and set
         *  our [Any] field [mKeyContext] (the key state) to `null` so we don't think a button is
         *  pressed when it isn't.
         *
         * @param state   Current game state, one of [STATE_START], [STATE_PLAY], [STATE_LOSE],
         * [STATE_PAUSE], or [STATE_RUNNING].
         * @param message optional message to log
         */
        fun setGameState(state: Int, message: CharSequence?) {
            if (message != null) {
                Log.i(TAG, "Game state set to $state $message")
            }
            synchronized(mSurfaceHolder) {


                // change state if needed
                if (mState != state) {
                    mState = state
                }
                if (mState == STATE_PLAY) {
                    val res = mContext.resources
                    mBackgroundImageFar = BitmapFactory
                        .decodeResource(res, R.drawable.background_a)

                    // don't forget to resize the background image
                    mBackgroundImageFar = mBackgroundImageFar.scale(
                        width = mCanvasWidth * 2,
                        height = mCanvasHeight
                    )
                    mBackgroundImageNear = BitmapFactory
                        .decodeResource(res, R.drawable.background_b)

                    // don't forget to resize the background image
                    mBackgroundImageNear =
                        mBackgroundImageNear.scale(width = mCanvasWidth * 2, height = mCanvasHeight)
                } else if (mState == STATE_RUNNING) {
                    // When we enter the running state we should clear any old events in the queue
                    mEventQueue.clear()

                    // And reset the key state so we don't think a button is pressed when it isn't
                    mKeyContext = null
                }
            }
        }

        /**
         * Add key press input to the [GameEvent] queue. We add a new instance of [KeyGameEvent]
         * constructed from our parameters, with `false` as the up option to our event queue
         * in [ConcurrentLinkedQueue] of [GameEvent] field [mEventQueue], and return `true` to
         * the caller.
         *
         * @param keyCode the keycode of the [KeyEvent] parameter [msg]
         * @param msg     [msg] Description of the key event.
         * @return        We always return `true`, which is in turn returned to the caller of our
         * [onKeyDown] override in [JetBoy] to prevent this event from being propagated further.
         */
        @Suppress("SameReturnValue")
        fun doKeyDown(keyCode: Int, msg: KeyEvent): Boolean {
            mEventQueue.add(KeyGameEvent(keyCode, false, msg))
            return true
        }

        /**
         * Add key press input to the [GameEvent] queue. We add a new instance of [KeyGameEvent]
         * constructed from our parameters, with `true` as the up option to our event queue
         * in [ConcurrentLinkedQueue] of [GameEvent] field [mEventQueue], and return `true` to
         * the caller.
         *
         * @param keyCode the keycode of the [KeyEvent] parameter [msg]
         * @param msg     [KeyEvent] Description of the key event.
         * @return        We always return `true`, which is in turn returned to the caller of our
         * [onKeyDown] override in [JetBoy] to prevent this event from being propagated further.
         */
        @Suppress("SameReturnValue")
        fun doKeyUp(keyCode: Int, msg: KeyEvent): Boolean {
            mEventQueue.add(KeyGameEvent(keyCode, true, msg))
            return true
        }

        /**
         * Callback invoked when the surface dimensions change. In a block synchronized on our
         * [SurfaceHolder] field [mSurfaceHolder] we save our [Int] parameters [width] and [height]
         * in our fields [mCanvasWidth] and [mCanvasHeight] respectively and then resize our
         * [Bitmap] fields [mBackgroundImageFar] and [mBackgroundImageNear] to be twice [width]
         * wide and [height] high specifying that they both be filtered while scaling them.
         *
         * @param width  New width of the canvas
         * @param height New height of the canvas
         */
        fun setSurfaceSize(width: Int, height: Int) {
            // synchronized to make sure these all change atomically
            synchronized(mSurfaceHolder) {
                mCanvasWidth = width
                mCanvasHeight = height

                // don't forget to resize the background image
                mBackgroundImageFar = mBackgroundImageFar.scale(width = width * 2, height = height)

                // don't forget to resize the background image
                mBackgroundImageNear = mBackgroundImageNear.scale(
                    width = width * 2,
                    height = height
                )
            }
        }

        /**
         * Pauses the physics update & animation. In a block synchronized on our [SurfaceHolder]
         * field [mSurfaceHolder], if our [Int] field [mState] is equal to [STATE_RUNNING], we call
         * our method [setGameState] to set set our state to [STATE_PAUSE]. If our [TimerTask] field
         * [mTimerTask] is not null we call its [TimerTask.cancel] method to cancel the timer task.
         * If our [JetPlayer] field [mJet] is not `null` we call its [JetPlayer.pause] method to
         * pause the playback of the JET segment queue.
         */
        fun pause() {
            synchronized(mSurfaceHolder) {
                if (mState == STATE_RUNNING) gameState = STATE_PAUSE
                if (mTimerTask != null) {
                    mTimerTask!!.cancel()
                }
                if (mJet != null) {
                    mJet!!.pause()
                }
            }
        }

        /**
         * Does the work of updating the timer and constructing the string to be displayed in the
         * timer [TextView]. First we subtract a second from our [Int] field [mTimerLimit]. Then
         * wrapped in a `try` block intended to `catch` and log any exceptions we initialize
         * [Int] variable `val moreThanMinute` to [mTimerLimit] minus 60, then branch on the value
         * of `moreThanMinute`:
         *
         *  * Greater than or equal to 0: If `moreThanMinute` is greater than 9 we set our [String]
         *  field [mTimerValue] to the string formed by appending the string value of `moreThanMinute`
         *  to the string "1:", otherwise we append it to the string "1:0".
         *
         *  * Less than 0: If [mTimerLimit] is greater than 9 we set our [String] field [mTimerValue]
         *  to the string formed by appending the string value of [mTimerLimit] to the string "0:",
         *  otherwise we append it to the string "0:0".
         *
         * Having exited the try block we initialize [Message] variable `val msg` with an instance
         * from the global message pool, then initialize [Bundle] variable `val b` with a new
         * instance and store [mTimerValue] in it under the key "text". If [mTimerLimit] is now
         * equal to 0 we put the string value of [STATE_LOSE] in `b` under the key "STATE_LOSE",
         * set [TimerTask] field [mTimerTask] to `null` and [Int] field [mState] to [STATE_LOSE].
         * If [mTimerLimit] is not equal to 0 we set [mTimerTask] to a new instance whose
         * [TimerTask.run] override calls this method again and then call the [Timer.schedule]
         * method of our [Timer] field [mTimer] to schedule [mTimerTask] to run [Int] field
         * [mTaskIntervalInMillis] (1000) milliseconds from now. We then set the data bundle of
         * `msg` to `b` and call the [Handler.sendMessage] method of our [Handler] field [mHandler]
         * to push `msg` onto the end of its message queue.
         */
        private fun doCountDown() {
            //Log.d(TAG,"Time left is " + mTimerLimit);
            mTimerLimit -= 1
            try {
                //subtract one minute and see what the result is.
                val moreThanMinute = mTimerLimit - 60
                mTimerValue = if (moreThanMinute >= 0) {
                    if (moreThanMinute > 9) {
                        "1:$moreThanMinute"
                    } else {
                        "1:0$moreThanMinute"
                    }
                } else {
                    if (mTimerLimit > 9) {
                        "0:$mTimerLimit"
                    } else {
                        "0:0$mTimerLimit"
                    }
                }
            } catch (e1: Exception) {
                Log.e(TAG, "doCountDown threw $e1")
            }
            val msg = mHandler.obtainMessage()
            val b = Bundle()
            b.putString("text", mTimerValue)

            //time's up
            if (mTimerLimit == 0) {
                b.putString("STATE_LOSE", "" + STATE_LOSE)
                mTimerTask = null
                mState = STATE_LOSE
            } else {
                mTimerTask = object : TimerTask() {
                    override fun run() {
                        doCountDown()
                    }
                }
                mTimer!!.schedule(mTimerTask, mTaskIntervalInMillis.toLong())
            }

            //this is how we send data back up to the main JetBoyView thread.
            //if you look in constructor of JetBoyView you will see code for
            //Handling of messages. This is borrowed directly from lunar lander.
            //Thanks again!
            msg.data = b
            mHandler.sendMessage(msg)
        }

        // JET info: JET event listener interface implementation:

        /**
         * Required [OnJetEventListener] method. Notifications for queue updates. We ignore.
         *
         * @param player     the JET player the status update is coming from
         * @param nbSegments the number of segments in the JET queue
         */
        override fun onJetNumQueuedSegmentUpdate(player: JetPlayer, nbSegments: Int) {
            //Log.i(TAG, "onJetNumQueuedUpdate(): nbSegs =" + nbSegments);
        }

        // JET info: JET event listener interface implementation:

        /**
         * The method which receives notification from event listener. This is where we queue up
         * events 80 and 82. We just add a new instance of [JetGameEvent] constructed from our
         * parameters to our event queue in [ConcurrentLinkedQueue] of [GameEvent] field
         * [mEventQueue].
         *
         * Most of the data passed is unneeded for JetBoy logic but shown for code sample
         * completeness.
         *
         * @param player     the JET player the status update is coming from
         * @param segment    8 bit unsigned value
         * @param track      6 bit unsigned value
         * @param channel    4 bit unsigned value
         * @param controller 7 bit unsigned value
         * @param value      7 bit unsigned value
         */
        override fun onJetEvent(player: JetPlayer, segment: Short, track: Byte, channel: Byte,
                                controller: Byte, value: Byte) {

            //Log.d(TAG, "jet got event " + value);

            //events fire outside the animation thread. This can cause timing issues.
            //put in queue for processing by animation thread.
            mEventQueue.add(JetGameEvent(player, segment, track, channel, controller, value))
        }

        // JET info: JET event listener interface implementation:

        /**
         * Callback for when JET pause state is updated. We ignore.
         *
         * @param player – the JET player the status update is coming from.
         * @param paused – indicates whether JET is paused (1) or not (0)
         */
        override fun onJetPauseUpdate(player: JetPlayer, paused: Int) {
            //Log.i(TAG, "onJetPauseUpdate(): paused =" + paused);
        }

        // JET info: JET event listener interface implementation:

        /**
         * Callback for when JET's currently playing segment's userID is updated. We ignore.
         *
         * @param player the JET player the status update is coming from
         * @param userId the ID of the currently playing segment
         * @param repeatCount the repetition count for the segment (0 means it plays once)
         */
        override fun onJetUserIdUpdate(player: JetPlayer, userId: Int, repeatCount: Int) {
            //Log.i(TAG, "onJetUserIdUpdate(): userId =" + userId + " repeatCount=" + repeatCount);
        }
    } //end thread class

    /**
     * The [TextView] in our layout that displays the current timer value
     */
    private var mTimerView: TextView? = null

    /**
     * The [Button] in our layout that starts a new game after the user finishes or loses.
     */
    private var mButtonRetry: Button? = null

    /**
     * The [TextView] in our layout that displays the winning or losing information at the end
     * of the game.
     */
    private var mTextView: TextView? = null

    /**
     * Pass in a reference to the timer view widget so we can update it from here. We just save our
     * [TextView] parameter [tv] in our [TextView] field [mTimerView].
     *
     * @param tv [TextView] to use for our timer `View`: [TextView] field [mTimerView]
     */
    fun setTimerView(tv: TextView?) {
        mTimerView = tv
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on focus lost. e.g. user
     * switches to take a call. If our [Boolean] parameter [hasWindowFocus] is `false` and our
     * [JetBoyThread] field [thread] is not `null` we call the [JetBoyThread.pause] method of
     * [thread] to pause the game.
     *
     * @param hasWindowFocus `true` if the window containing this view now has focus,
     * `false` otherwise.
     */
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus) {
            if (thread != null) thread!!.pause()
        }
    }

    /**
     * This is called immediately after any structural changes (format or size) have been made to the
     * surface. You should at this point update the imagery in the surface. This method is always
     * called at least once, after [surfaceCreated]. We just call the [JetBoyThread.setSurfaceSize]
     * method of our [JetBoyThread] field [thread] to inform it of the new values for our [Int]
     * parameters [width] and [height].
     *
     * @param holder The [SurfaceHolder] whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        thread!!.setSurfaceSize(width, height)
    }

    /**
     * This is called immediately after the surface is first created. We call the
     * [JetBoyThread.setRunning] method of our [JetBoyThread] field [thread] to signal
     * the thread that it should be running, then call its [JetBoyThread.start] method
     * to have it begin execution.
     *
     * @param arg0 The [SurfaceHolder] whose surface is being created.
     */
    override fun surfaceCreated(arg0: SurfaceHolder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread!!.setRunning(true)
        thread!!.start()
    }

    /**
     * This is called immediately before a surface is being destroyed. We initialize [Boolean]
     * variable `var retry` to `true` then call the [JetBoyThread.setRunning] method of our
     * [JetBoyThread] field [thread] with `false` to signal the thread that it should stop running.
     * Then we loop while `retry` remains `true` wrapping an attempt to wait for [thread] to die in
     * a `try` block that catches and logs [InterruptedException]. We exit the loop by setting
     * `retry` to false if the call to [JetBoyThread.join] returns without an exception being thrown.
     *
     * @param arg0 The SurfaceHolder whose surface is being destroyed.
     */
    override fun surfaceDestroyed(arg0: SurfaceHolder) {
        var retry = true
        thread!!.setRunning(false)
        while (retry) {
            try {
                thread!!.join()
                retry = false
            } catch (_: InterruptedException) {
                Log.i(TAG, "surfaceDestroyed was interrupted")
            }
        }
    }

    /**
     * Setter for our [Button] field [mButtonRetry].
     *
     * @param buttonRetry [Button] to use to allow user to start game over.
     */
    fun setButtonView(buttonRetry: Button?) {
        mButtonRetry = buttonRetry
        //  mButtonRestart = _buttonRestart;
    }

    /**
     * Setter for our [TextView] field [mTextView] (we reuse the help screen from the end game
     * screen).
     *
     * @param textView [TextView] to use for the end game screen.
     */
    fun setTextView(textView: TextView?) {
        mTextView = textView
    }

    companion object {
        /**
         * the number of asteroids that must be destroyed
         */
        const val SUCCESS_THRESHOLD: Int = 50

        /**
         * TAG used for logging.
         */
        const val TAG: String = "JetBoy"
    }
}

    // State-tracking constants.

    /**
     * Game is in the start state, set by our constructor.
     */
    const val STATE_START: Int = -1

    /**
     * Game is waiting for the player to press the "PLAY!" button.
     */
    const val STATE_PLAY: Int = 0

    /**
     * Timer has run out, win or lose is not actually known until the hit count is checked.
     */
    const val STATE_LOSE: Int = 1

    /**
     * Game animation is paused, set by our threads `onPause` method when our window
     * loses focus.
     */
    const val STATE_PAUSE: Int = 2

    /**
     * The player has pressed the "PLAY!" button and we have begun the game animation.
     */
    const val STATE_RUNNING: Int = 3

    /**
     * How many frames per beat? The basic animation can be changed for instance to 3/4 by
     * changing this to 3. Untested is the impact on other parts of game logic for non 4/4 time.
     */
    private const val ANIMATION_FRAMES_PER_BEAT = 4
