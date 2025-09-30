/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.pictureinpicture.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.RawRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.example.android.pictureinpicture.R
import java.lang.ref.WeakReference
import androidx.core.graphics.drawable.toDrawable

/**
 * A [RelativeLayout] that displays a video. Used by both `MainActivity` and
 * `MediaSessionPlaybackActivity`.
 *
 * @param context The [Context] the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a
 *        reference to a style resource that supplies default values for
 *        the view. Can be 0 to not look for defaults.
 */
class MovieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    /**
     * Monitors all events related to [MovieView].
     */
    abstract class MovieListener {
        /**
         * Called when the video is started or resumed.
         */
        open fun onMovieStarted() {}

        /**
         * Called when the video is paused or finished.
         */
        open fun onMovieStopped() {}

        /**
         * Called when this view should be minimized.
         */
        open fun onMovieMinimized() {}
    }

    /**
     * Displays the video playback. Its id is `R.id.surface` in layout `R.layout.view_movie`.
     */
    val mSurfaceView: SurfaceView

    // Controls

    /**
     * Play/Pause button with id `R.id.toggle`, its image is toggled in the [adjustToggleState]
     * method between `R.drawable.ic_pause_64dp`, and `R.drawable.ic_play_arrow_64dp` depending on
     * whether the movie is paused or playing.
     */
    val mToggle: ImageButton

    /**
     * [View] with id `R.id.shade`, it is a translucent GRAY shade which is used to partially
     * obscure the movie when the controls are visible on top of it.
     */
    val mShade: View

    /**
     * [ImageButton] with id `R.id.fast_forward`, it is the fast forward button.
     */
    val mFastForward: ImageButton

    /**
     * [ImageButton] with id `R.id.fast_rewind`, it is the fast rewind button.
     */
    val mFastRewind: ImageButton

    /**
     * [ImageButton] with id `R.id.minimize`, it is the minimize (enter PiP) button.
     */
    val mMinimize: ImageButton

    /**
     * The [Player] responsible for the playback of the media.
     */
    private var player: Player? = null
    
    /**
     * This is the [TimeoutHandler] used to hide the controls after the timeout period defined by
     * the constant [TIMEOUT_CONTROLS] has passed.
     */
    private var mTimeoutHandler: TimeoutHandler? = null
    
    /**
     * The listener for all the events happening in this [MovieView].
     */
    var mMovieListener: MovieListener? = null

    /**
     * The resource ID for the video to play.
     */
    @RawRes
    var mVideoResourceId: Int = 0 // Primarily informational now

    /**
     * The title of the video to play.
     */
    var title: String? = null // Primarily informational now

    /**
     * Whether we adjust our view bounds or we fill the remaining area with black bars
     */
    var mAdjustViewBounds: Boolean = false

    /**
     * Listener for changes in a Player.
     * All methods have no-op default implementations to allow selective overrides.
     *
     * If the return value of a Player getter changes due to a change in command availability, the
     * corresponding listener method(s) will be invoked. If the return value of a Player getter does
     * not change because the corresponding command is not available, the corresponding listener
     * method will not be invoked.
     */
    private val internalPlayerListener: Player.Listener = object : Player.Listener {
        /**
         * Called when the value of `isPlaying()` changes.
         * `onEvents(Player, Player.Events)` will also be called to report this event along with
         * other events that happen in the same Looper message queue iteration.
         *
         * First we call our [adjustToggleState] method to update the toggle button image of the
         * play/pause button [mToggle]. Then we call [setKeepScreenOn] with our [Boolean] parameter
         * [isPlaying] to keep the screen on if it is `true` or allow it to turn off if it is `false`.
         * If our [TimeoutHandler] property [mTimeoutHandler] is `null` and [isPlaying] is `true` we
         * set it to a new instance of [TimeoutHandler]. Then we call the [TimeoutHandler.removeMessages]
         * method with the [TimeoutHandler.MESSAGE_HIDE_CONTROLS] constant to remove any messages
         * that are queued. If [isPlaying] is `true` we call the [MovieListener.onMovieStarted] callback
         * of our [MovieListener] property [mMovieListener] and call the [TimeoutHandler.sendEmptyMessageDelayed]
         * method of [mTimeoutHandler] with the [TimeoutHandler.MESSAGE_HIDE_CONTROLS] constant and
         * [TIMEOUT_CONTROLS] as the delay in milliseconds. If [isPlaying] is `false` we call the
         * [MovieListener.onMovieStopped] callback of our [MovieListener] property [mMovieListener].
         *
         * @param isPlaying Whether the player is playing.
         */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            adjustToggleState()
            keepScreenOn = isPlaying
            if (mTimeoutHandler == null && isPlaying) {
                mTimeoutHandler = TimeoutHandler(this@MovieView)
            }
            mTimeoutHandler?.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
            if (isPlaying) {
                mMovieListener?.onMovieStarted()
                mTimeoutHandler?.sendEmptyMessageDelayed(TimeoutHandler.MESSAGE_HIDE_CONTROLS, TIMEOUT_CONTROLS.toLong())
            } else {
                mMovieListener?.onMovieStopped()
            }
        }

        /**
         * Called when the value returned from `getPlaybackState()` changes.
         * `onEvents(Player, Player.Events)` will also be called to report this event along with
         * other events that happen in the same Looper message queue iteration.
         *
         * First we call our [adjustToggleState] method to update the toggle button image of the
         * play/pause button [mToggle]. If our [Int] parameter [playbackState] is equal to
         * [Player.STATE_ENDED] we call the [setKeepScreenOn] method with `false` to allow the
         * screen to turn off. Then we call the [MovieListener.onMovieStopped] callback of our
         * [MovieListener] property [mMovieListener] and call the [TimeoutHandler.removeMessages]
         * method of our [TimeoutHandler] property [mTimeoutHandler] with the
         * [TimeoutHandler.MESSAGE_HIDE_CONTROLS] constant to remove any messages that are queued.
         *
         * @param playbackState The new playback `Player.State`.
         */
        override fun onPlaybackStateChanged(playbackState: Int) {
            adjustToggleState()
            if (playbackState == Player.STATE_ENDED) {
                keepScreenOn = false
                mMovieListener?.onMovieStopped()
                mTimeoutHandler?.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS) // Stop hiding controls when ended
            }
        }

        /**
         * Called each time when `getVideoSize()` changes.
         * `onEvents(Player, Player.Events)` will also be called to report this event along with
         * other events that happen in the same Looper message queue iteration.
         *
         * We just call the [RelativeLayout.requestLayout] method to trigger a call to schedule a
         * layout pass of the view tree.
         *
         * @param videoSize The new size of the video.
         */
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            requestLayout() // Trigger onMeasure when video size is known
        }
    }

    /**
     * Initializes the view. We start by setting the background color to black, then call the `inflate`
     * method to inflate the layout `R.layout.view_movie` into this view. Then we set our [SurfaceView]
     * property [mSurfaceView] to the view with id `R.id.surface` in the inflated layout. We also set
     * our [View] properties [mShade] to the view with id `R.id.shade` in the inflated layout and
     * our [ImageButton] properties [mToggle], [mFastForward], [mFastRewind], and [mMinimize] to
     * the views with ids `R.id.toggle`, `R.id.fast_forward`, `R.id.fast_rewind`, and `R.id.minimize`
     * respectively in the inflated layout. We call the [Context.withStyledAttributes] extension
     * function of our [Context] parameter [context] with the arguments:
     *  - `set`: The base set of attribute values, our [AttributeSet] parameter [attrs].
     *  - `attrs`: The additional custom attributes to be recognised, the attributes defined in
     *  `R.styleable.MovieView`.
     *  - `defStyleAttr`: An attribute in the current theme that contains a reference to a style
     *  that supplies default values for the [TypedArray] that is used to retrieve the attributes
     *  in the `block` lambda of the extension function, our [Int] parameter [defStyleAttr]
     *  - `defStyleRes`: A resource identifier of a style resource that supplies default values for
     *  the [TypedArray], used only if defStyleAttr is 0 or can not be found in the theme. Can be 0
     *  to not look for defaults. We pass the resource ID `R.style.Widget_PictureInPicture_MovieView`
     *  (a style that sets the attribute `android:src` to `null`, and `android:adjustViewBounds` to
     *  `false`.
     *
     * In the lambda we set our [Int] property [mVideoResourceId] to the resource value in the
     * [TypedArray] whose index is `R.styleable.MovieView_android_src`. We set our [Boolean] property
     * [mAdjustViewBounds] to the boolean value in the [TypedArray] whose index is
     * `R.styleable.MovieView_android_adjustViewBounds`. We set our [String] property [title] to the
     * string value in the [TypedArray] whose index is `R.styleable.MovieView_android_title`.
     *
     * Next we initialize our [View.OnClickListener] variable `listener` with a lambda that switches
     * on the [View.id] of the view that was clicked:
     *  - `R.id.surface`: We call our [toggleControls] method.
     *  - `R.id.toggle`: We call our [toggle] method.
     *  - `R.id.fast_forward`: We call our [fastForward] method.
     *  - `R.id.fast_rewind`: We call our [fastRewind] method.
     *  - `R.id.minimize`: We call our [MovieListener.onMovieMinimized] callback of our
     *  [MovieListener] property [mMovieListener].
     *
     * Then if our [Player] property [player] is not `null` and its [Player.isPlaying] property
     * is `true`: if our [TimeoutHandler] property [mTimeoutHandler] is `null` we set it to a new
     * instance of [TimeoutHandler]. Then we call the [TimeoutHandler.removeMessages] method with
     * the [TimeoutHandler.MESSAGE_HIDE_CONTROLS] constant to remove any messages that are queued,
     * and call the [TimeoutHandler.sendEmptyMessageDelayed] method with the message
     * [TimeoutHandler.MESSAGE_HIDE_CONTROLS] and [TIMEOUT_CONTROLS] as the delay in milliseconds.
     *
     * Having defined our [OnClickListener] variable `listener` we set the [OnClickListener] of
     * [mSurfaceView], [toggle], [mFastForward], [mFastRewind], and [mMinimize] to `listener`.
     * We then call the [SurfaceHolder.addCallback] method of the [SurfaceHolder] property of
     * our [SurfaceView] property [mSurfaceView] with an object that implements the
     * [SurfaceHolder.Callback] interface. (See the object declaration for more details.)
     * Finally we call the [adjustToggleState] method to update the toggle button image of the
     * play/pause button [mToggle].
     */
    init {
        setBackgroundColor(Color.BLACK)
        inflate(context, R.layout.view_movie, this)
        mSurfaceView = findViewById(R.id.surface)
        mShade = findViewById(R.id.shade)
        mToggle = findViewById(R.id.toggle)
        mFastForward = findViewById(R.id.fast_forward)
        mFastRewind = findViewById(R.id.fast_rewind)
        mMinimize = findViewById(R.id.minimize)

        context.withStyledAttributes(
            set = attrs,
            attrs = R.styleable.MovieView,
            defStyleAttr = defStyleAttr,
            defStyleRes = R.style.Widget_PictureInPicture_MovieView
        ) {
            mVideoResourceId = getResourceId(R.styleable.MovieView_android_src, 0)
            setAdjustViewBounds(getBoolean(R.styleable.MovieView_android_adjustViewBounds, false))
            title = getString(R.styleable.MovieView_android_title)
        }

        val listener = OnClickListener { view: View ->
            when (view.id) {
                R.id.surface -> toggleControls()
                R.id.toggle -> toggle()
                R.id.fast_forward -> fastForward()
                R.id.fast_rewind -> fastRewind()
                R.id.minimize -> mMovieListener?.onMovieMinimized()
            }
            if (player?.isPlaying == true) {
                if (mTimeoutHandler == null) {
                    mTimeoutHandler = TimeoutHandler(this@MovieView)
                }
                mTimeoutHandler!!.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
                mTimeoutHandler!!.sendEmptyMessageDelayed(TimeoutHandler.MESSAGE_HIDE_CONTROLS, TIMEOUT_CONTROLS.toLong())
            }
        }
        mSurfaceView.setOnClickListener(listener)
        mToggle.setOnClickListener(listener)
        mFastForward.setOnClickListener(listener)
        mFastRewind.setOnClickListener(listener)
        mMinimize.setOnClickListener(listener)

        mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            /**
             * Called when the surface is first created.
             * This is where we can start drawing on the surface. We set the video surface of our
             * [Player] field [player] to the surface of our [SurfaceHolder] parameter [holder].
             *
             * @param holder The [SurfaceHolder] whose surface is being created.
             */
            override fun surfaceCreated(holder: SurfaceHolder) {
                player?.setVideoSurface(holder.surface)
            }

            /**
             * This is called immediately after any structural changes (format or size) have been
             * made to the surface. You should at this point update the imagery in the surface.
             * This method is always called at least once, after surfaceCreated. We ignore this.
             *
             * @param holder The SurfaceHolder whose surface has changed.
             * @param format The new {@link PixelFormat} of the surface.
             * @param width The new width of the surface.
             * @param height The new height of the surface.
             */
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            /**
             * Called when the surface is being destroyed.
             * We clear the video surface of our [Player] field [player] by detaching it from the
             * surface of our [SurfaceHolder] parameter [holder].
             *
             * @param holder The [SurfaceHolder] whose surface is being destroyed.
             */
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player?.clearVideoSurface(holder.surface)
            }
        })
        adjustToggleState() // Initial state based on no player
    }

    /**
     * Sets the [Player] to be used by this view.
     *
     * This function handles the transition from an old player to a new one. It removes the internal
     * listener from the current player, updates the player reference to the new one, and then adds
     * the listener to the new player. If a valid surface is already available, it attaches the new
     * player to it. Finally, it updates the UI state (like the play/pause button) and requests a
     * re-layout to adjust the view dimensions if the new player already has a video track with a
     * specific size.
     *
     * @param newPlayer The new [Player] instance to use for video playback, or `null` to clear it.
     */
    fun setPlayer(newPlayer: Player?) {
        player?.removeListener(internalPlayerListener)
        player = newPlayer
        player?.addListener(internalPlayerListener)
        if (player != null && mSurfaceView.holder.surface != null && mSurfaceView.holder.surface.isValid) {
            player?.setVideoSurface(mSurfaceView.holder.surface)
        }
        adjustToggleState()
        requestLayout() // For onMeasure if player has video size already
    }

    /**
     * Measures the view and its content to determine the measured width and the
     * measured height.
     *
     * This implementation adjusts the bounds of the view to match the aspect ratio of the video
     * if the video's dimensions are available.
     *
     * If [mAdjustViewBounds] is `true`, this view will be resized to match the video's aspect ratio.
     * The final size depends on the `MeasureSpec` provided by the parent. For example, if the
     * width is fixed (`MeasureSpec.EXACTLY`) and the height is not, the height will be calculated
     * from the width and the aspect ratio.
     *
     * If [mAdjustViewBounds] is `false`, the view will fill the space given by the `MeasureSpec`,
     * and black bars will be added via padding to letterbox or pillarbox the video, preserving
     * the aspect ratio of the video content within the view's bounds.
     *
     * If the video size is not yet known (width or height is 0), it falls back to the default
     * `RelativeLayout.onMeasure` behavior.
     *
     * @param widthMeasureSpec Horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec Vertical space requirements as imposed by the parent.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val videoWidth: Int = player?.videoSize?.width ?: 0
        val videoHeight: Int = player?.videoSize?.height ?: 0

        if (videoWidth != 0 && videoHeight != 0) {
            val aspectRatio: Float = videoHeight.toFloat() / videoWidth
            val width: Int = MeasureSpec.getSize(widthMeasureSpec)
            val widthMode: Int = MeasureSpec.getMode(widthMeasureSpec)
            val height: Int = MeasureSpec.getSize(heightMeasureSpec)
            val heightMode: Int = MeasureSpec.getMode(heightMeasureSpec)

            if (mAdjustViewBounds) {
                if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                    super.onMeasure(
                        /* widthMeasureSpec = */ widthMeasureSpec,
                        /* heightMeasureSpec = */ MeasureSpec
                            .makeMeasureSpec(
                                (width * aspectRatio).toInt(),
                                MeasureSpec.EXACTLY
                            )
                    )
                } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                    super.onMeasure(
                        /* widthMeasureSpec = */ MeasureSpec.makeMeasureSpec(
                            (height / aspectRatio).toInt(),
                            MeasureSpec.EXACTLY
                        ),
                        /* heightMeasureSpec = */ heightMeasureSpec
                    )
                } else {
                    super.onMeasure(
                        /* widthMeasureSpec = */ widthMeasureSpec,
                        /* heightMeasureSpec = */ MeasureSpec.makeMeasureSpec(
                            (width * aspectRatio).toInt(),
                            MeasureSpec.EXACTLY
                        )
                    )
                }
            } else {
                val viewRatio: Float = height.toFloat() / width
                if (aspectRatio > viewRatio) {
                    val padding: Int = ((width - height / aspectRatio) / 2).toInt()
                    setPadding(padding, 0, padding, 0)
                } else {
                    val padding: Int = ((height - width * aspectRatio) / 2).toInt()
                    setPadding(0, padding, 0, padding)
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Called when the view is detached from its window.
     *
     * This function performs cleanup operations to prevent memory leaks and unnecessary background
     * processing. It cancels any pending messages to hide the playback controls, nullifies the timeout
     * handler, and removes the internal listener from the [Player]. The [Player] itself is not
     * released here because its lifecycle is managed by the containing Activity.
     */
    override fun onDetachedFromWindow() {
        mTimeoutHandler?.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
        mTimeoutHandler = null
        player?.removeListener(internalPlayerListener) // Clean up listener
        // Player lifecycle is managed by the Activity, so don't release player here.
        super.onDetachedFromWindow()
    }

    /**
     * Sets the listener for movie events.
     *
     * @param movieListener The [MovieListener] to be set. Can be `null` to clear the listener.
     */
    fun setMovieListener(movieListener: MovieListener?) {
        mMovieListener = movieListener
    }

    /**
     * Sets whether this view should adjust its bounds to preserve the video's aspect ratio.
     *
     * When set to `true`, the view's bounds will be resized to match the video's aspect ratio.
     * This is useful when the aspect ratio of the video is the primary concern for layout.
     *
     * When set to `false`, the view will not resize itself. Instead, it will use a black
     * background to create "letterboxing" or "pillarboxing" effects, ensuring the video content
     * is displayed with the correct aspect ratio within the view's original bounds.
     *
     * This method also triggers a re-layout of the view by calling `requestLayout()` to apply
     * the changes.
     *
     * @param adjustViewBounds `true` to adjust the view's bounds to match the video's aspect ratio,
     * `false` to fill the bounds and letterbox the content.
     */
    fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (mAdjustViewBounds == adjustViewBounds) return
        mAdjustViewBounds = adjustViewBounds
        background = if (adjustViewBounds) null else Color.BLACK.toDrawable()
        requestLayout()
    }

    /**
     * Shows the playback controls.
     *
     * This function makes the playback controls (play/pause, fast forward, fast rewind, minimize)
     * and a background shade visible. It uses a `TransitionManager` to animate the appearance
     * of these controls with a fade-in effect.
     */
    fun showControls() {
        TransitionManager.beginDelayedTransition(this)
        mShade.visibility = VISIBLE
        mToggle.visibility = VISIBLE
        mFastForward.visibility = VISIBLE
        mFastRewind.visibility = VISIBLE
        mMinimize.visibility = VISIBLE
    }

    /**
     * Hides the playback controls.
     *
     * This function makes the playback controls (play/pause, fast forward, fast rewind, minimize)
     * and the background shade invisible. It uses a `TransitionManager` to animate the disappearance
     * of these controls with a fade-out effect.
     */
    fun hideControls() {
        TransitionManager.beginDelayedTransition(this)
        mShade.visibility = INVISIBLE
        mToggle.visibility = INVISIBLE
        mFastForward.visibility = INVISIBLE
        mFastRewind.visibility = INVISIBLE
        mMinimize.visibility = INVISIBLE
    }

    /**
     * Seeks forward in the video by a fixed interval.
     *
     * This moves the playback position forward by the value of [FAST_FORWARD_REWIND_INTERVAL].
     * If the player is not available, this operation does nothing.
     */
    fun fastForward() {
        // player?.seekForward() // Uses player's configured seek increment
        player?.seekTo( (player?.currentPosition ?: 0) + FAST_FORWARD_REWIND_INTERVAL ) // Keep original 5s logic
    }

    /**
     * Seeks backward in the video by a fixed interval.
     *
     * This moves the playback position backward by the value of [FAST_FORWARD_REWIND_INTERVAL].
     * If the player is not available, this operation does nothing.
     */
    fun fastRewind() {
        // player?.seekBackward() // Uses player's configured seek increment
        player?.seekTo( (player?.currentPosition ?: 0) - FAST_FORWARD_REWIND_INTERVAL ) // Keep original 5s logic
    }

    /**
     * Starts or resumes video playback.
     *
     * This method calls the `play` function on the internal [Player] instance. If the player is
     * available, it will start playing the video from the current position.
     *
     * The state change will be caught by the [internalPlayerListener], which in turn calls the
     * `onMovieStarted` method of the [mMovieListener].
     */
    fun play() {
        player?.play()
        // Listener will call mMovieListener.onMovieStarted()
    }

    /**
     * Pauses video playback.
     *
     * This method calls the `pause` function on the internal [Player] instance. If the player is
     * available, it will pause the video at the current position.
     *
     * The state change will be caught by the [internalPlayerListener], which in turn calls the
     * `onMovieStopped` method of the [mMovieListener].
     */
    fun pause() {
        player?.pause()
        // Listener will call mMovieListener.onMovieStopped()
    }

    /**
     * Toggles the playback state of the video.
     *
     * If the video is currently playing, it will be paused. If it is paused or stopped, it will
     * start playing. This method checks the `isPlaying` status of the internal [Player] and calls
     * either `pause()` or `play()` accordingly. If the player is not available, this operation
     * does nothing.
     */
    fun toggle() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    /**
     * Toggles the visibility of the playback controls.
     *
     * If the controls are currently visible (determined by checking the visibility of the [mShade]
     * view), this function calls [hideControls] to hide them. Otherwise, it calls [showControls]
     * to make them visible.
     */
    fun toggleControls() {
        if (mShade.isVisible) hideControls() else showControls()
    }

    /**
     * Adjusts the icon and content description of the play/pause toggle button ([mToggle])
     * based on the current playback state of the [Player].
     *
     * If the video is playing, the button will show a "pause" icon and its content description
     * will be set to the string resource `R.string.pause`.
     *
     * If the video is not playing (i.e., it is paused, stopped, or the player is not set),
     * the button will show a "play" icon and its content description will be set to the
     * string resource `R.string.play`.
     */
    fun adjustToggleState() {
        if (player?.isPlaying == true) {
            mToggle.contentDescription = resources.getString(R.string.pause)
            mToggle.setImageResource(R.drawable.ic_pause_64dp)
        } else {
            mToggle.contentDescription = resources.getString(R.string.play)
            mToggle.setImageResource(R.drawable.ic_play_arrow_64dp)
        }
    }

    /**
     * A [Handler] that hides the playback controls after a certain timeout.
     *
     * This handler is responsible for automatically hiding the on-screen controls (like play/pause,
     * fast-forward, etc.) when there is no user interaction for a predefined period. It holds a
     * [WeakReference] to the [MovieView] to avoid memory leaks. When it receives the
     * [MESSAGE_HIDE_CONTROLS] message, it calls the [MovieView.hideControls] method on the
     * referenced view.
     *
     * @param view The [MovieView] instance this handler is associated with.
     */
    class TimeoutHandler internal constructor(view: MovieView) : Handler(Looper.myLooper()!!) {
        /**
         * A [WeakReference] to the [MovieView] that this handler is associated with.
         * This is used to prevent memory leaks by allowing the [MovieView] to be garbage
         * collected even if there are pending messages in the handler's queue.
         */
        private val mMovieViewRef: WeakReference<MovieView> = WeakReference(view)

        /**
         * Handles incoming messages.
         *
         * This method is called when a message is received by the handler. It checks if the message's
         * `what` code matches [MESSAGE_HIDE_CONTROLS]. If it does, it retrieves the [MovieView]
         * instance from the [WeakReference] and calls its [MovieView.hideControls] method to hide the
         * playback controls. If the `what` code does not match, the message is passed to the superclass
         * for default processing.
         *
         * @param msg The [Message] object to be handled.
         */
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_HIDE_CONTROLS) {
                mMovieViewRef.get()?.hideControls()
            } else {
                super.handleMessage(msg)
            }
        }
        companion object {
            /**
             * A message code that the [TimeoutHandler] uses to request hiding the playback controls.
             */
            const val MESSAGE_HIDE_CONTROLS: Int = 1
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        @Suppress("unused")
        const val TAG: String = "MovieView"

        /**
         * The amount of time to fast-forward or rewind, in milliseconds.
         */
        const val FAST_FORWARD_REWIND_INTERVAL: Int = 5000 // ms

        /**
         * The timeout for hiding the controls, in milliseconds.
         */
        const val TIMEOUT_CONTROLS: Int = 3000 // ms
    }
}
