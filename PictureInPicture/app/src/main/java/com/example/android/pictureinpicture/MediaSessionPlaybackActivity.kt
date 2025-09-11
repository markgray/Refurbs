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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.pictureinpicture

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.pictureinpicture.widget.MovieView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.Commands
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommands
//Futures import is no longer needed as we are not returning ListenableFuture from callback methods other than custom ones.

/**
 * Demonstrates usage of Picture-in-Picture when using Media3.
 */
class MediaSessionPlaybackActivity : AppCompatActivity() {

    private var mSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()
    private var mMovieView: MovieView? = null
    private var mScrollView: ScrollView? = null

    private val mOnClickListener = View.OnClickListener { view: View ->
        if (view.id == R.id.pip) {
            minimize()
        }
    }

    /**
     * Listener for general player events in the Activity.
     * MovieView has its own internal listener for its UI updates.
     */
    private val activityPlayerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Activity specific reactions to play/pause, if any.
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // Activity specific reactions to metadata change, if any.
        }
    }

    /**
     * Listener for callbacks from MovieView, specifically for minimize action.
     */
    private val movieViewListener = object : MovieView.MovieListener() {
        override fun onMovieMinimized() {
            minimize()
        }
        // onMovieStarted and onMovieStopped are now handled by MovieView's internal Player.Listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<LinearLayout>(R.id.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        mMovieView = findViewById(R.id.movie)
        mScrollView = findViewById(R.id.scroll)
        val switchExampleButton = findViewById<Button>(R.id.switch_example)
        switchExampleButton.text = getString(R.string.switch_custom)
        switchExampleButton.setOnClickListener(SwitchActivityOnClick())

        mMovieView?.setMovieListener(movieViewListener) // Set listener for minimize callback
        findViewById<View>(R.id.pip).setOnClickListener(mOnClickListener)
    }

    override fun onStart() {
        super.onStart()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(activityPlayerListener) // Activity general player listener
        }

        mMovieView?.setPlayer(player) // Provide the player instance to MovieView

        mSession = MediaSession.Builder(this, player!!)
            .setCallback(Media3SessionCallback())
            .build()

        if (mMovieView != null && mMovieView!!.mVideoResourceId != 0) { // Use mVideoResourceId from MovieView
            val videoUri = "android.resource://${packageName}/${mMovieView!!.mVideoResourceId}"
            val mediaItem = MediaItem.Builder()
                .setMediaId(mMovieView!!.mVideoResourceId.toString())
                .setUri(videoUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(mMovieView!!.title ?: "Untitled Video")
                        .build()
                )
                .build()
            player?.setMediaItem(mediaItem)
            player?.prepare()
            // player?.playWhenReady = true // Uncomment to autoplay
        } else {
            // Handle case where video resource is not available
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
        mSession?.release()
        mSession = null
        player?.removeListener(activityPlayerListener)
        player?.release()
        player = null
    }

    override fun onRestart() {
        super.onRestart()
        if (player == null) { // Basic re-initialization
            initializeMediaSession()
        }
        if (!isInPictureInPictureMode) {
            mMovieView?.showControls()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            adjustFullScreen(resources.configuration)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            if (mMovieView != null && player?.isPlaying == false) {
                mMovieView!!.showControls()
            }
        } else {
            mMovieView?.hideControls()
        }
    }

    fun minimize() {
        if (mMovieView == null || player == null) {
            return
        }
        mMovieView!!.hideControls()
        val movieViewWidth = mMovieView!!.width
        val movieViewHeight = mMovieView!!.height
        val rational = if (movieViewWidth > 0 && movieViewHeight > 0) {
            Rational(movieViewWidth, movieViewHeight)
        } else {
            Rational(16, 9)
        }
        mPictureInPictureParamsBuilder.setAspectRatio(rational)
        enterPictureInPictureMode(mPictureInPictureParamsBuilder.build())
    }

    private fun adjustFullScreen(config: Configuration) {
        val decorView: View = window.decorView
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            mScrollView!!.visibility = View.GONE
            mMovieView!!.setAdjustViewBounds(false)
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            mScrollView!!.visibility = View.VISIBLE
            mMovieView!!.setAdjustViewBounds(true)
        }
    }

    private inner class Media3SessionCallback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val sessionCommands = SessionCommands.Builder().build()
            val playerCommands = Commands.Builder().build()
            return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
        }

        // onPlay, onPause, onSetMediaItem, etc., are not typically overridden here
        // as MediaSession forwards these to the player if commands are available.
        // Custom commands or more complex connection logic would go here.
    }

    private inner class SwitchActivityOnClick : View.OnClickListener {
        override fun onClick(view: View) {
            startActivity(Intent(view.context, MainActivity::class.java))
            finish()
        }
    }
}
