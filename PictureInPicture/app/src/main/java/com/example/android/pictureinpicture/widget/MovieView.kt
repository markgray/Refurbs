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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNUSED_ANONYMOUS_PARAMETER", "MemberVisibilityCanBePrivate")

package com.example.android.pictureinpicture.widget

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.RawRes
import com.example.android.pictureinpicture.R
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Provides video playback. There is nothing directly related to Picture-in-Picture here.
 *
 *
 * This is similar to `android.widget.VideoView`, but it comes with a custom control
 * (play/pause, fast forward, and fast rewind).
 */
class MovieView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {
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
     * Shows the video playback. Its id is R.id.surface in layout R.layout.view_movie.
     */
    val mSurfaceView: SurfaceView
    // Controls
    /**
     * Play/Pause button with id R.id.toggle, its image is toggled in the `adjustToggleState`
     * method between R.drawable.ic_pause_64dp, and R.drawable.ic_play_arrow_64dp depending on whether
     * the movie is paused or playing.
     */
    val mToggle: ImageButton

    /**
     * `View` with id R.id.shade, it is a translucent GRAY shade which is used to partially
     * obscure the movie when the controls are visible on top of it.
     */
    val mShade: View

    /**
     * `ImageButton` with id R.id.fast_forward, it is the fast forward button.
     */
    val mFastForward: ImageButton

    /**
     * `ImageButton` with id R.id.fast_rewind, it is the fast rewind button.
     */
    val mFastRewind: ImageButton

    /**
     * `ImageButton` with id R.id.minimize, it is the minimize (enter PiP button).
     */
    val mMinimize: ImageButton

    /**
     * This plays the video. This will be null when no video is set.
     */
    var mMediaPlayer: MediaPlayer? = null

    /**
     * The resource ID for the video to play.
     */
    @RawRes
    var mVideoResourceId: Int = 0

    /**
     * The title of the video to play.
     */
    var title: String? = null

    /**
     * Whether we adjust our view bounds or we fill the remaining area with black bars
     */
    var mAdjustViewBounds: Boolean = false

    /**
     * Handles timeout for media controls, calls our `hideControls` method after TIMEOUT_CONTROLS
     * (3000ms) occurs.
     */
    var mTimeoutHandler: TimeoutHandler? = null

    /**
     * The listener for all the events we publish, set by our `setMovieListener` method.
     */
    var mMovieListener: MovieListener? = null

    /**
     * Position of the movie that we save in the `surfaceDestroyed` callback of the
     * [SurfaceHolder.Callback] interface, then restore in the `onPrepared` callback
     * of the [MediaPlayer.OnPreparedListener] interface.
     */
    var mSavedCurrentPosition: Int = 0
    /**
     * Constructor called from XML. Perform inflation from XML and apply a class-specific base style
     * from a theme attribute. This constructor of View allows subclasses to use their own base style
     * when they are inflating. First we call our super's constructor, then we set our background
     * color to BLACK. Then we inflate our layout file R.layout.view_movie, attaching the result to
     * to "this" `MovieView extends RelativeLayout` instance.
     *
     *
     * We initialize our fields `SurfaceView mSurfaceView` by finding the view with id R.id.surface,
     * `View mShade` by finding the view with id R.id.shade, `ImageButton mToggle` by finding
     * the view with id R.id.toggle, `ImageButton mFastForward` by finding the view with id
     * R.id.fast_forward, `ImageButton mFastRewind` by finding the view with id R.id.fast_rewind,
     * and `ImageButton mMinimize` by finding the view with id R.id.minimize.
     *
     *
     * We initialize `TypedArray attributes` by retrieving styled attribute information in this
     * Context's theme from the `AttributeSet attrs`, for the attributes in R.styleable.MovieView
     * (Attributes that can be used with a MovieView: android:src, android:title, and android:adjustViewBounds),
     * using `defStyleAttr` as the default values, and R.style.Widget_PictureInPicture_MovieView
     * as the default style (android:src is null, android:adjustViewBounds is false).
     *
     *
     * We then call our method `setVideoResourceId` to set the video resource id that we will play
     * to the resource id in `attributes` stored under the key R.styleable.MovieView_android_src
     * (set to @raw/vid_bigbuckbunny (raw/vid_bigbuckbunny.mp4) by an android:src="@raw/vid_bigbuckbunny"
     * attribute in our layout file layout/activity_main.xml).
     *
     *
     * We call our method `setAdjustViewBounds` to set the background in accordance with the
     * boolean stored under the key R.styleable.MovieView_android_adjustViewBounds in `attributes`
     * (set to true by an android:adjustViewBounds="true" attribute in our layout file layout/activity_main.xml).
     *
     *
     * We call our method `setTitle` to set the title of our movie to the string store stored under
     * the key R.styleable.MovieView_android_title in `attributes` (set to "Big Buck Bunny" by an
     * android:title="@string/title_bigbuckbunny" attribute in our layout file layout/activity_main.xml).
     * We then recycle `attributes`.
     *
     *
     * We initialize `OnClickListener listener` with an anonymous class whose `onClick`
     * switches on the id of the view that was clicked to handle each of the buttons whose
     * `OnClickListener` it is, then it starts or resets the timeout to hide controls. We use
     * `listener` as the `OnClickListener` for the views `mSurfaceView`, `mToggle`,
     * `mFastForward`, `mFastRewind`, and `mMinimize` so we call the method
     * `setOnClickListener(listener)` for each of these views.
     *
     *
     * Finally we fetch the holder of `mSurfaceView` and set its `SurfaceHolder.Callback`
     * to an anonymous class which calls our method `openVideo` when the `surfaceCreated`
     * callback is called, and calls our method `closeVideo` when the `surfaceDestroyed`
     * method is called.
     *
     * @param context      The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     * reference to a style resource that supplies default values for
     * the view. Can be 0 to not look for defaults.
     */
    /**
     * Constructor called from XML. We just all our 3 argument constructor using 0 as the
     * `defStyleAttr` argument.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    /**
     * Our one argument constructor, we just call our 2 argument constructor with null as the
     * `attrs` argument.
     *
     * param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    init {
        setBackgroundColor(Color.BLACK)

        // Inflate the content
        inflate(context, R.layout.view_movie, this)
        mSurfaceView = findViewById(R.id.surface)
        mShade = findViewById(R.id.shade)
        mToggle = findViewById(R.id.toggle)
        mFastForward = findViewById(R.id.fast_forward)
        mFastRewind = findViewById(R.id.fast_rewind)
        mMinimize = findViewById(R.id.minimize)
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.MovieView,
            defStyleAttr,
            R.style.Widget_PictureInPicture_MovieView)
        mVideoResourceId = attributes.getResourceId(R.styleable.MovieView_android_src, 0)
        setAdjustViewBounds(
            attributes.getBoolean(R.styleable.MovieView_android_adjustViewBounds, false))
        title = attributes.getString(R.styleable.MovieView_android_title)
        attributes.recycle()

        // Bind view events
        val listener = OnClickListener { view ->

            /**
             * Called when one of the views we have been registered as an `OnClickListener`
             * is clicked. First we switch on the id of the view that was clicked:
             *
             *  *
             * R.id.surface (our `SurfaceView` in layout/view_movie.xml) we call
             * our method `toggleControls` to toggle the visibility of our control
             * buttons, and break.
             *
             *  *
             * R.id.toggle (the play/pause button in layout/view_movie.xml) we call our
             * method `toggle` to toggle between playing and paused states, and break.
             *
             *  *
             * R.id.fast_forward (the fast forward button in layout/view_movie.xml) we
             * call our method `fastForward` to fast forward 5000ms, and break.
             *
             *  *
             * R.id.fast_rewind (the fast rewind button in layout/view_movie.xml) we
             * call our method `fastRewind` to fast forward 5000ms, and break.
             *
             *  *
             * R.id.minimize (the minimise button (down carrot) in layout/view_movie.xml)
             * if our field `MovieListener mMovieListener` is not null we call its
             * method `onMovieMinimized` then we break.
             *
             *
             * Finally if `MediaPlayer mMediaPlayer` is not null, we first initialize
             * `TimeoutHandler mTimeoutHandler` with a new instance if it is null, then
             * we remove all MESSAGE_HIDE_CONTROLS from it. If `mMediaPlayer` is playing
             * we send a delayed empty message to `mTimeoutHandler` with MESSAGE_HIDE_CONTROLS
             * as the `what`, and TIMEOUT_CONTROLS (3000ms) as the delay to use.
             *
             * @param view view that was clicked.
             */
            /**
             * Called when one of the views we have been registered as an `OnClickListener`
             * is clicked. First we switch on the id of the view that was clicked:
             *
             *  *
             * R.id.surface (our `SurfaceView` in layout/view_movie.xml) we call
             * our method `toggleControls` to toggle the visibility of our control
             * buttons, and break.
             *
             *  *
             * R.id.toggle (the play/pause button in layout/view_movie.xml) we call our
             * method `toggle` to toggle between playing and paused states, and break.
             *
             *  *
             * R.id.fast_forward (the fast forward button in layout/view_movie.xml) we
             * call our method `fastForward` to fast forward 5000ms, and break.
             *
             *  *
             * R.id.fast_rewind (the fast rewind button in layout/view_movie.xml) we
             * call our method `fastRewind` to fast forward 5000ms, and break.
             *
             *  *
             * R.id.minimize (the minimise button (down carrot) in layout/view_movie.xml)
             * if our field `MovieListener mMovieListener` is not null we call its
             * method `onMovieMinimized` then we break.
             *
             *
             * Finally if `MediaPlayer mMediaPlayer` is not null, we first initialize
             * `TimeoutHandler mTimeoutHandler` with a new instance if it is null, then
             * we remove all MESSAGE_HIDE_CONTROLS from it. If `mMediaPlayer` is playing
             * we send a delayed empty message to `mTimeoutHandler` with MESSAGE_HIDE_CONTROLS
             * as the `what`, and TIMEOUT_CONTROLS (3000ms) as the delay to use.
             *
             * @param view view that was clicked.
             */
            when (view.id) {
                R.id.surface -> toggleControls()
                R.id.toggle -> toggle()
                R.id.fast_forward -> fastForward()
                R.id.fast_rewind -> fastRewind()
                R.id.minimize -> if (mMovieListener != null) {
                    mMovieListener!!.onMovieMinimized()
                }
            }
            // Start or reset the timeout to hide controls
            if (mMediaPlayer != null) {
                if (mTimeoutHandler == null) {
                    mTimeoutHandler = TimeoutHandler(this@MovieView)
                }
                mTimeoutHandler!!.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
                if (mMediaPlayer!!.isPlaying) {
                    mTimeoutHandler!!.sendEmptyMessageDelayed(
                        TimeoutHandler.MESSAGE_HIDE_CONTROLS, TIMEOUT_CONTROLS.toLong())
                }
            }
        }
        mSurfaceView.setOnClickListener(listener)
        mToggle.setOnClickListener(listener)
        mFastForward.setOnClickListener(listener)
        mFastRewind.setOnClickListener(listener)
        mMinimize.setOnClickListener(listener)

        // Prepare video playback
        mSurfaceView
            .holder
            .addCallback(
                object : SurfaceHolder.Callback {
                    /**
                     * This is called immediately after the surface is first created. We call
                     * our method `openVideo` to initialize `MediaPlayer mMediaPlayer`,
                     * set its surface to the surface of `SurfaceHolder holder` and start
                     * playing the video.
                     *
                     * @param holder The SurfaceHolder whose surface is being created.
                     */
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        openVideo(holder.surface)
                    }

                    /**
                     * This is called immediately after any structural changes (format or
                     * size) have been made to the surface. We ignore it.
                     *
                     * @param holder The SurfaceHolder whose surface has changed.
                     * @param format The new PixelFormat of the surface.
                     * @param width The new width of the surface.
                     * @param height The new height of the surface.
                     */
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        // Do nothing
                    }

                    /**
                     * This is called immediately before a surface is destroyed. If our field
                     * `MediaPlayer mMediaPlayer` is not null we set our field
                     * `int mSavedCurrentPosition` to the current position of the video
                     * being played by `mMediaPlayer`, then we call our method `closeVideo`
                     * to release `mMediaPlayer` and set it to null.
                     *
                     * @param holder The SurfaceHolder whose surface is being destroyed.
                     */
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        if (mMediaPlayer != null) {
                            mSavedCurrentPosition = mMediaPlayer!!.currentPosition
                        }
                        closeVideo()
                    }
                })
    }

    /**
     * Measure the view and its content to determine the measured width and the measured height. If
     * our field `MediaPlayer mMediaPlayer` is not null we have some work to do. We initialize
     * `int videoWidth` with the width of `mMediaPlayer` and `int videoHeight` with
     * its height. If either `videoWidth` or `videoHeight` is 0 we do not have any work
     * to do after all, otherwise we initialize `float aspectRatio` with the ratio of
     * `videoHeight` over `videoWidth`. We initialize `int width` with the width
     * in pixels specified in `widthMeasureSpec`, and `int widthMode` with the mode,
     * initialize `int height` with the height specified in `heightMeasureSpec`, and
     * `int widthMode` with the mode. Then if our field `mAdjustViewBounds` is:
     *
     *  *
     * true:
     *
     *  *
     * if `widthMode` is EXACTLY, and `heightMode` is NOT EXACTLY:
     * we call our super's implementation of `onMeasure` with `widthMeasureSpec`
     * as the width, and a `MeasureSpec` specifying a height of `width*aspectRatio`
     * and mode of EXACTLY.
     *
     *  *
     * if `widthMode` is NOT EXACTLY, and `heightMode` is EXACTLY:
     * we call our super's implementation of `onMeasure` with a `MeasureSpec`
     * specifying a width of `height/aspectRatio`, and mode of EXACTLY, and
     * a height of `heightMeasureSpec`.
     *
     *  *
     * otherwise we call our super's implementation of `onMeasure` with `widthMeasureSpec`
     * as the width, and a `MeasureSpec` specifying a height of `width*aspectRatio`
     * and mode of EXACTLY.
     *
     *
     *
     *  *
     * false: we initialize `float viewRatio` to be the ratio `height / width`,
     * and if `aspectRatio` is:
     *
     *  *
     * greater than `viewRatio`: we initialize `int padding` to the amount
     * of padding needed to add to the left and right of our view to center it, then
     * call the `setPadding` method to set our padding to it.
     *
     *  *
     * less than or equal to `viewRatio`: we initialize `int padding` to
     * the amount of padding needed to add to the top and bottom of our view to center
     * it, then call the `setPadding` method to set our padding to it.
     *
     * After setting the padding we call our super's implementation of `onMeasure`
     * with our parameters `widthMeasureSpec` and `heightMeasureSpec`.
     *
     * Having called our super's implementation in all cases we return.
     *
     *
     * If `mMediaPlayer` is null we just call our super's implementation of
     * `onMeasure` with our parameters `widthMeasureSpec` and `heightMeasureSpec`.
     *
     * @param widthMeasureSpec  horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mMediaPlayer != null) {
            val videoWidth = mMediaPlayer!!.videoWidth
            val videoHeight = mMediaPlayer!!.videoHeight
            if (videoWidth != 0 && videoHeight != 0) {
                val aspectRatio = videoHeight.toFloat() / videoWidth
                val width = MeasureSpec.getSize(widthMeasureSpec)
                val widthMode = MeasureSpec.getMode(widthMeasureSpec)
                val height = MeasureSpec.getSize(heightMeasureSpec)
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                if (mAdjustViewBounds) {
                    if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                        super.onMeasure(
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec((width * aspectRatio).toInt(), MeasureSpec.EXACTLY))
                    } else if (widthMode != MeasureSpec.EXACTLY
                        && heightMode == MeasureSpec.EXACTLY) {
                        super.onMeasure(
                            MeasureSpec.makeMeasureSpec((height / aspectRatio).toInt(), MeasureSpec.EXACTLY),
                            heightMeasureSpec)
                    } else {
                        super.onMeasure(
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec((width * aspectRatio).toInt(), MeasureSpec.EXACTLY))
                    }
                } else {
                    val viewRatio = height.toFloat() / width
                    if (aspectRatio > viewRatio) {
                        val padding = ((width - height / aspectRatio) / 2).toInt()
                        setPadding(padding, 0, padding, 0)
                    } else {
                        val padding = ((height - width * aspectRatio) / 2).toInt()
                        setPadding(0, padding, 0, padding)
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                }
                return
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * This is called when the view is detached from a window. If our field `TimeoutHandler mTimeoutHandler`
     * is not null we remove all MESSAGE_HIDE_CONTROLS from its queue and set it to null. In any case we
     * then call our super's implementation of `onDetachedFromWindow`.
     */
    override fun onDetachedFromWindow() {
        if (mTimeoutHandler != null) {
            mTimeoutHandler!!.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
            mTimeoutHandler = null
        }
        super.onDetachedFromWindow()
    }

    /**
     * Sets the listener to monitor movie events. We just save our parameter `MovieListener movieListener`
     * in our field `MovieListener mMovieListener`.
     *
     * @param movieListener The listener to be set.
     */
    fun setMovieListener(movieListener: MovieListener?) {
        mMovieListener = movieListener
    }
    /**
     * The raw resource id of the video to play. We just return our field `mVideoResourceId`
     * to the caller.
     *
     * @return ID of the video resource.
     */
    /**
     * Sets the raw resource ID of video to play. If our parameter `int id` is equal to our
     * field `int mVideoResourceId` we return having done nothing. Otherwise we store `id`
     * in `mVideoResourceId`, then initialize `Surface surface` by fetching the `SurfaceHolder`
     * of `SurfaceView mSurfaceView` and fetching the `Surface` from it. If `surface`
     * is not null and it is a valid `Surface` we call our method `closeVideo` to release
     * `MediaPlayer mMediaPlayer` if it is not null, and then call `openVideo(surface)`
     * to create a new `mMediaPlayer` and start the mp4 with resource ID `id` playing.
     *
     * param id The raw resource ID.
     */
    var videoResourceId: Int
        get() = mVideoResourceId
        set(id) {
            if (id == mVideoResourceId) {
                return
            }
            mVideoResourceId = id
            val surface = mSurfaceView.holder.surface
            if (surface != null && surface.isValid) {
                closeVideo()
                openVideo(surface)
            }
        }

    /**
     * Setter for our field `mAdjustViewBounds`. If `mAdjustViewBounds` is already equal
     * to our parameter `adjustViewBounds` we return having done nothing. Otherwise we save
     * `adjustViewBounds` in `mAdjustViewBounds`. If `mAdjustViewBounds` is now
     * true we set the background to null, if false we set it to BLACK. Finally we call the method
     * `requestLayout` to schedule a layout pass of our view tree.
     *
     * @param adjustViewBounds value to set our field `mAdjustViewBounds` to.
     */
    fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (mAdjustViewBounds == adjustViewBounds) {
            return
        }
        mAdjustViewBounds = adjustViewBounds
        if (adjustViewBounds) {
            background = null
        } else {
            setBackgroundColor(Color.BLACK)
        }
        requestLayout()
    }

    /**
     * Shows all the controls. First we start a delayed transition to the view that will exist when
     * the controls are visible. Then we set the visibility of the following views to VISIBLE:
     * `mShade`, `mToggle`, `mFastForward`, `mFastRewind`, and `mMinimize`
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
     * Hides all the controls. First we start a delayed transition to the view that will exist when
     * the controls are invisible. Then we set the visibility of the following views to INVISIBLE:
     * `mShade`, `mToggle`, `mFastForward`, `mFastRewind`, and `mMinimize`
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
     * Fast-forward the video. If our field `mMediaPlayer` is null, we return having done nothing.
     * Otherwise we call the `seekTo` method of `mMediaPlayer` to seek to a position that
     * is FAST_FORWARD_REWIND_INTERVAL (5000ms) forward from the current position.
     */
    fun fastForward() {
        if (mMediaPlayer == null) {
            return
        }
        mMediaPlayer!!.seekTo(mMediaPlayer!!.currentPosition + FAST_FORWARD_REWIND_INTERVAL)
    }

    /**
     * Fast-rewind the video. If our field `mMediaPlayer` is null, we return having done nothing.
     * Otherwise we call the `seekTo` method of `mMediaPlayer` to seek to a position that
     * is FAST_FORWARD_REWIND_INTERVAL (5000ms) behind the current position.
     */
    fun fastRewind() {
        if (mMediaPlayer == null) {
            return
        }
        mMediaPlayer!!.seekTo(mMediaPlayer!!.currentPosition - FAST_FORWARD_REWIND_INTERVAL)
    }

    /**
     * Returns the current position of the video. If the the player has not been created, then
     * assumes the beginning of the video. If `MediaPlayer mMediaPlayer` is null we just return
     * 0 to the caller, otherwise we return the value returned by the `getCurrentPosition` method
     * of `mMediaPlayer`
     *
     * @return The current position of the video.
     */
    val currentPosition: Int
        get() = if (mMediaPlayer == null) {
            0
        } else mMediaPlayer!!.currentPosition

    /**
     * Determines whether the movie is playing or not. If `MediaPlayer mMediaPlayer` is not
     * null we return the value returned by its `isPlaying` method, otherwise we return false.
     *
     * @return true if the movie is playing, false if it is not.
     */
    val isPlaying: Boolean
        get() = mMediaPlayer != null && mMediaPlayer!!.isPlaying

    /**
     * Starts the movie playing. If our field `MediaPlayer mMediaPlayer` is null we return having
     * done nothing. Otherwise we call its `start` method to start the movie, and call our method
     * `adjustToggleState` to set the image and content description of `ImageButton mToggle`
     * play/pause button depending on whether the movie is playing or paused. We then call the method
     * `setKeepScreenOn(true)` to keep the screen from turning off. Finally if our field
     * `MovieListener mMovieListener` is not null we call its `onMovieStarted` override.
     */
    fun play() {
        if (mMediaPlayer == null) {
            return
        }
        mMediaPlayer!!.start()
        adjustToggleState()
        keepScreenOn = true
        if (mMovieListener != null) {
            mMovieListener!!.onMovieStarted()
        }
    }

    /**
     * Pauses the movie. If our field `MediaPlayer mMediaPlayer` is null call our method
     * `adjustToggleState` to set the image and content description of `ImageButton mToggle`
     * play/pause button depending on whether the movie is playing or paused, then return. Otherwise we
     * call the `start` method of `mMediaPlayer` to start the movie, and call our method
     * `adjustToggleState` to set the image and content description of `ImageButton mToggle`
     * play/pause button depending on whether the movie is playing or paused. We then call the method
     * `setKeepScreenOn(false)` to allow the screen to turn off. Finally if our field
     * `MovieListener mMovieListener` is not null we call its `onMovieStopped` override.
     */
    fun pause() {
        if (mMediaPlayer == null) {
            adjustToggleState()
            return
        }
        mMediaPlayer!!.pause()
        adjustToggleState()
        keepScreenOn = false
        if (mMovieListener != null) {
            mMovieListener!!.onMovieStopped()
        }
    }

    /**
     * Creates a new `MediaPlayer mMediaPlayer`, sets its surface and starts the movie playing.
     * If our field `int mVideoResourceId` is zero we return having done nothing. Otherwise we
     * initialize `MediaPlayer mMediaPlayer` with a new instance, set its surface to our parameter
     * `Surface surface` and call our method `startVideo` to start the movie playing.
     *
     * @param surface `Surface` of our `SurfaceView mSurfaceView`
     */
    fun openVideo(surface: Surface?) {
        if (mVideoResourceId == 0) {
            return
        }
        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setSurface(surface)
        startVideo()
    }

    /**
     * Restarts playback of the video. First we call the `reset` method of our field
     * `MediaPlayer mMediaPlayer` (Resets the MediaPlayer to its uninitialized state. After
     * calling this method, you will have to initialize it again by setting the data source and
     * calling prepare().)
     *
     *
     * Then wrapped in a try with the resource `AssetFileDescriptor fd` opened to read the raw
     * resource with id `mVideoResourceId`, we set the data source of `mMediaPlayer` to
     * `fd`, set its `OnPreparedListener` to an anonymous class whose `onPrepared`
     * override adjusts the aspect ratio of our view, seeks to `mSavedCurrentPosition` if it
     * is not zero (setting it then to zero), and plays the video whether the seek was necessary or
     * not. We then set the `OnCompletionListener` of `mMediaPlayer` to an anonymous class
     * whose `onCompletion` override which changes our play/pause control appropriately, allows
     * our display to turn off, and if our field `MovieListener mMovieListener` is not null
     * calls its `onMovieStopped` override.
     *
     *
     * Finally we call the `prepare` method of `MediaPlayer mMediaPlayer`, with our catch
     * clause logging "Failed to open video" if it catches an IOException.
     */
    fun startVideo() {
        mMediaPlayer!!.reset()
        try {
            resources.openRawResourceFd(mVideoResourceId).use { fd ->
                mMediaPlayer!!.setDataSource(fd)
                mMediaPlayer!!.setOnPreparedListener { mediaPlayer: MediaPlayer ->
                    // Adjust the aspect ratio of this view
                    requestLayout()
                    if (mSavedCurrentPosition > 0) {
                        mediaPlayer.seekTo(mSavedCurrentPosition)
                        mSavedCurrentPosition = 0
                    } else {
                        // Start automatically
                        play()
                    }
                }
                mMediaPlayer!!.setOnCompletionListener { mediaPlayer: MediaPlayer? ->
                    adjustToggleState()
                    keepScreenOn = false
                    if (mMovieListener != null) {
                        mMovieListener!!.onMovieStopped()
                    }
                }
                mMediaPlayer!!.prepare()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open video", e)
        }
    }

    /**
     * Releases our field `MediaPlayer mMediaPlayer` and sets it to null if it is not already
     * null.
     */
    fun closeVideo() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    /**
     * Toggles between play and pause. If our field `MediaPlayer mMediaPlayer` is null we return
     * having done nothing. If `mMediaPlayer` is playing we call its `pause` method if it
     * is not playing we call its `play` method.
     */
    fun toggle() {
        if (mMediaPlayer == null) {
            return
        }
        if (mMediaPlayer!!.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Toggles the controls between visible and invisible. If our field `View mShade` is currently
     * visible, we call our method `hideControls`, if it is not visible we call our method
     * `showControls`.
     */
    fun toggleControls() {
        if (mShade.visibility == VISIBLE) {
            hideControls()
        } else {
            showControls()
        }
    }

    /**
     * Sets the content description and image of `ImageButton mToggle` depending on whether
     * the movie is playing or not. If `MediaPlayer mMediaPlayer` is not null, and its
     * `isPlaying` method indicates that the movie is currently playing by returning true we
     * set the content description of `mToggle` to the string with id R.string.pause ("Pause"),
     * and set its image to the drawable with resource id R.drawable.ic_pause_64dp (the double bar
     * pause icon), otherwise we set the content description of `mToggle` to the string with
     * id R.string.play ("Play"), and set its image to the drawable with resource id
     * R.drawable.ic_play_arrow_64dp (the right pointing triangle play icon).
     */
    fun adjustToggleState() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
            mToggle.contentDescription = resources.getString(R.string.pause)
            mToggle.setImageResource(R.drawable.ic_pause_64dp)
        } else {
            mToggle.contentDescription = resources.getString(R.string.play)
            mToggle.setImageResource(R.drawable.ic_play_arrow_64dp)
        }
    }

    /**
     * `Handler` we use to hide the controls after a timeout of 3000ms
     */
    class TimeoutHandler internal constructor(view: MovieView) : Handler(Looper.myLooper()!!) {
        /**
         * Weak reference to the `MovieView` whose controls we are supposed to hide.
         */
        val mMovieViewRef: WeakReference<MovieView>

        /**
         * Our constructor. We simply save a weak reference to our parameter `MovieView view`
         * in our field `WeakReference<MovieView> mMovieViewRef`.
         *
         * param view `MovieView` whose controls we are supposed to hide.
         */
        init {
            mMovieViewRef = WeakReference(view)
        }

        /**
         * We implement this to receive messages. We switch on the `what` field of our parameter
         * `Message msg`:
         *
         *  *
         * MESSAGE_HIDE_CONTROLS - we retrieve `MovieView movieView` from our field
         * `WeakReference<MovieView> mMovieViewRef`, and if it is not null we call its
         * `hideControls` method to hide the controls, then break.
         *
         *  *
         * default - we call our super's implementation of `handleMessage(msg)`.
         *
         *
         *
         * @param msg `Message` we are supposed to handle.
         */
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_HIDE_CONTROLS) {
                val movieView = mMovieViewRef.get()
                movieView?.hideControls()
            } else {
                super.handleMessage(msg)
            }
        }

        companion object {
            /**
             * The "what" value we use for the hide controls message.
             */
            const val MESSAGE_HIDE_CONTROLS: Int = 1
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "MovieView"

        /**
         * The amount of time we are stepping forward or backward for fast-forward and fast-rewind.
         */
        const val FAST_FORWARD_REWIND_INTERVAL: Int = 5000 // ms

        /**
         * The amount of time until we fade out the controls.
         */
        const val TIMEOUT_CONTROLS: Int = 3000 // ms
    }
}