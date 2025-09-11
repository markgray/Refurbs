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

class MovieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    abstract class MovieListener {
        open fun onMovieStarted() {}
        open fun onMovieStopped() {}
        open fun onMovieMinimized() {}
    }

    val mSurfaceView: SurfaceView
    val mToggle: ImageButton
    val mShade: View
    val mFastForward: ImageButton
    val mFastRewind: ImageButton
    val mMinimize: ImageButton

    private var player: Player? = null
    private var mTimeoutHandler: TimeoutHandler? = null
    var mMovieListener: MovieListener? = null

    @RawRes
    var mVideoResourceId: Int = 0 // Primarily informational now
    var title: String? = null // Primarily informational now
    var mAdjustViewBounds: Boolean = false

    private val internalPlayerListener: Player.Listener = object : Player.Listener {
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

        override fun onPlaybackStateChanged(playbackState: Int) {
            adjustToggleState()
            if (playbackState == Player.STATE_ENDED) {
                keepScreenOn = false
                mMovieListener?.onMovieStopped()
                mTimeoutHandler?.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS) // Stop hiding controls when ended
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            requestLayout() // Trigger onMeasure when video size is known
        }
    }

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
            override fun surfaceCreated(holder: SurfaceHolder) {
                player?.setVideoSurface(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player?.clearVideoSurface(holder.surface)
            }
        })
        adjustToggleState() // Initial state based on no player
    }

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val videoWidth = player?.videoSize?.width ?: 0
        val videoHeight = player?.videoSize?.height ?: 0

        if (videoWidth != 0 && videoHeight != 0) {
            val aspectRatio: Float = videoHeight.toFloat() / videoWidth
            val width: Int = MeasureSpec.getSize(widthMeasureSpec)
            val widthMode: Int = MeasureSpec.getMode(widthMeasureSpec)
            val height: Int = MeasureSpec.getSize(heightMeasureSpec)
            val heightMode: Int = MeasureSpec.getMode(heightMeasureSpec)

            if (mAdjustViewBounds) {
                if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                    super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec((width * aspectRatio).toInt(), MeasureSpec.EXACTLY)
                    )
                } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                    super.onMeasure(
                        MeasureSpec.makeMeasureSpec((height / aspectRatio).toInt(), MeasureSpec.EXACTLY),
                        heightMeasureSpec
                    )
                } else {
                    super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec((width * aspectRatio).toInt(), MeasureSpec.EXACTLY)
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

    override fun onDetachedFromWindow() {
        mTimeoutHandler?.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
        mTimeoutHandler = null
        player?.removeListener(internalPlayerListener) // Clean up listener
        // Player lifecycle is managed by the Activity, so don't release player here.
        super.onDetachedFromWindow()
    }

    fun setMovieListener(movieListener: MovieListener?) {
        mMovieListener = movieListener
    }

    // videoResourceId getter remains for informational purposes
    // videoResourceId setter is removed as MovieView no longer directly loads media

    fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (mAdjustViewBounds == adjustViewBounds) return
        mAdjustViewBounds = adjustViewBounds
        background = if (adjustViewBounds) null else Color.BLACK.toDrawable() // Use ColorDrawable
        requestLayout()
    }

    fun showControls() {
        TransitionManager.beginDelayedTransition(this)
        mShade.visibility = VISIBLE
        mToggle.visibility = VISIBLE
        mFastForward.visibility = VISIBLE
        mFastRewind.visibility = VISIBLE
        mMinimize.visibility = VISIBLE
    }

    fun hideControls() {
        TransitionManager.beginDelayedTransition(this)
        mShade.visibility = INVISIBLE
        mToggle.visibility = INVISIBLE
        mFastForward.visibility = INVISIBLE
        mFastRewind.visibility = INVISIBLE
        mMinimize.visibility = INVISIBLE
    }

    fun fastForward() {
        // player?.seekForward() // Uses player's configured seek increment
        player?.seekTo( (player?.currentPosition ?: 0) + FAST_FORWARD_REWIND_INTERVAL ) // Keep original 5s logic
    }

    fun fastRewind() {
        // player?.seekBackward() // Uses player's configured seek increment
        player?.seekTo( (player?.currentPosition ?: 0) - FAST_FORWARD_REWIND_INTERVAL ) // Keep original 5s logic
    }

    fun play() {
        player?.play()
        // Listener will call mMovieListener.onMovieStarted()
    }

    fun pause() {
        player?.pause()
        // Listener will call mMovieListener.onMovieStopped()
    }

    // openVideo, startVideo, closeVideo methods are removed as player is managed externally.

    fun toggle() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun toggleControls() {
        if (mShade.isVisible) hideControls() else showControls()
    }

    fun adjustToggleState() {
        if (player?.isPlaying == true) {
            mToggle.contentDescription = resources.getString(R.string.pause)
            mToggle.setImageResource(R.drawable.ic_pause_64dp)
        } else {
            mToggle.contentDescription = resources.getString(R.string.play)
            mToggle.setImageResource(R.drawable.ic_play_arrow_64dp)
        }
    }

    class TimeoutHandler internal constructor(view: MovieView) : Handler(Looper.myLooper()!!) {
        private val mMovieViewRef: WeakReference<MovieView> = WeakReference(view)

        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_HIDE_CONTROLS) {
                mMovieViewRef.get()?.hideControls()
            } else {
                super.handleMessage(msg)
            }
        }
        companion object {
            const val MESSAGE_HIDE_CONTROLS: Int = 1
        }
    }

    companion object {
        @Suppress("unused")
        const val TAG: String = "MovieView"
        const val FAST_FORWARD_REWIND_INTERVAL: Int = 5000 // ms
        const val TIMEOUT_CONTROLS: Int = 3000 // ms
    }
}
