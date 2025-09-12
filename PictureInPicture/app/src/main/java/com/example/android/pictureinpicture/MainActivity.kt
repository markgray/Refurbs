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

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.pictureinpicture.widget.MovieView
import com.example.android.pictureinpicture.widget.MovieView.MovieListener
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

/**
 * Demonstrates usage of Picture-in-Picture mode on phones and tablets.
 */
class MainActivity : AppCompatActivity() {
    /**
     * The arguments to be used for Picture-in-Picture mode.
     */
    private val mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()

    /**
     * This shows the video.
     */
    private var mMovieView: MovieView? = null

    /**
     * The bottom half of the screen; hidden on landscape
     */
    private var mScrollView: ScrollView? = null

    /**
     * A [BroadcastReceiver] to receive action item events from Picture-in-Picture mode.
     */
    private var mReceiver: BroadcastReceiver? = null

    /**
     * The string with resource id `R.string.play` ("Play") that we use for the title of the "play"
     * action item.
     */
    private var mPlay: String? = null

    /**
     * The string with resource id `R.string.pause` ("Pause") that we use for the title of the
     * "pause" action item.
     */
    private var mPause: String? = null

    private var player: ExoPlayer? = null

    private val mOnClickListener = View.OnClickListener { view: View ->
        if (view.id == R.id.pip) {
            minimize()
        }
    }

    /**
     * Callbacks from the [MovieView] showing the video playback.
     */
    private val mMovieListener: MovieListener = object : MovieListener() {
        /**
         * Called when the video is started or resumed. Since we are now playing the video
         * we call our method [updatePictureInPictureActions] to change the action
         * item to pause the video, using `R.drawable.ic_pause_24dp` as the icon, [mPause]
         * ("Pause") as the title, [CONTROL_TYPE_PAUSE] as the type of action, and [REQUEST_PAUSE]
         * as the request code for the pending intent.
         */
        override fun onMovieStarted() {
            // We are playing the video now. In PiP mode, we want to show an action item to
            // pause the video.
            updatePictureInPictureActions(
                iconId = R.drawable.ic_pause_24dp,
                title = mPause,
                controlType = CONTROL_TYPE_PAUSE,
                requestCode = REQUEST_PAUSE
            )
        }

        /**
         * Called when the video is paused or finished. Since the video has stopped playing we call
         * our method [updatePictureInPictureActions] to change the action item to play the video,
         * using `R.drawable.ic_play_arrow_24dp` as the icon, [mPlay] ("Play") as the title,
         * [CONTROL_TYPE_PLAY] as the type of action, and [REQUEST_PLAY] as the request code for
         * the pending intent.
         */
        override fun onMovieStopped() {
            updatePictureInPictureActions(
                iconId = R.drawable.ic_play_arrow_24dp,
                title = mPlay,
                controlType = CONTROL_TYPE_PLAY,
                requestCode = REQUEST_PLAY
            )
        }

        /**
         * Called when this view should be minimized. We just call our method [minimize]
         * to enter Picture-in-Picture mode.
         */
        override fun onMovieMinimized() {
            minimize()
        }
    }

    fun updatePictureInPictureActions(
        @DrawableRes iconId: Int,
        title: String?,
        controlType: Int,
        requestCode: Int
    ) {
        val actions: ArrayList<RemoteAction> = ArrayList()
        val intent: PendingIntent = PendingIntent.getBroadcast(
            /* context = */ this@MainActivity,
            /* requestCode = */ requestCode,
            /* intent = */ Intent(ACTION_MEDIA_CONTROL)
                .putExtra(EXTRA_CONTROL_TYPE, controlType),
            /* flags = */ PendingIntent.FLAG_IMMUTABLE
        )
        val icon: Icon = Icon.createWithResource(this@MainActivity, iconId)
        actions.add(RemoteAction(icon, title!!, title, intent))

        actions.add(
            RemoteAction(
                Icon.createWithResource(this@MainActivity, R.drawable.ic_info_24dp),
                getString(R.string.info),
                getString(R.string.info_description),
                PendingIntent.getActivity(
                    this@MainActivity,
                    REQUEST_INFO,
                    Intent(
                        Intent.ACTION_VIEW,
                        getString(R.string.info_uri).toUri()),
                    PendingIntent.FLAG_IMMUTABLE)))
        mPictureInPictureParamsBuilder.setActions(actions)
        setPictureInPictureParams(mPictureInPictureParamsBuilder.build())
    }

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.activity_main`. We
     * then initialize our [String] field [mPlay] by fetching the string with id `R.string.play`
     * ("Play"), and [String] field [mPause] by fetching the string with id `R.string.pause`
     * ("Pause"). We initialize our [MovieView] field [mMovieView] by finding the view with id
     * `R.id.movie`, and [ScrollView] field [mScrollView] by finding the view with id `R.id.scroll`.
     *
     * We initialize [Button] variable `val switchExampleButton` by finding the view with id
     * `R.id.switch_example`, set its text to the string with id `R.string.switch_media_session`
     * ("Switch to using MediaSession"), and set its [View.OnClickListener] to [mOnClickListener].
     * We call the [MovieView.setMovieListener]  method of [MovieView] field [mMovieView] to set its
     * [MovieView.MovieListener] to our [MovieView.MovieListener] field [mMovieListener]. Finally
     * we find the view with id `R.id.pip` ("Enter Picture-in-Picture mode") and set its
     * [View.OnClickListener] to our field [mOnClickListener].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<LinearLayout>(R.id.activity_main)
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

        // Prepare string resources for Picture-in-Picture actions.
        mPlay = getString(R.string.play)
        mPause = getString(R.string.pause)

        // View references
        mMovieView = findViewById(R.id.movie)
        mScrollView = findViewById(R.id.scroll)
        val switchExampleButton = findViewById<Button>(R.id.switch_example)
        switchExampleButton.text = getString(R.string.switch_media_session)
        switchExampleButton.setOnClickListener(SwitchActivityOnClick())

        mMovieView?.setMovieListener(mMovieListener)
        findViewById<View>(R.id.pip).setOnClickListener(mOnClickListener)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            mMovieView?.setPlayer(player) // Pass player to MovieView

            if (mMovieView != null && mMovieView!!.mVideoResourceId != 0) {
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
                player?.playWhenReady = true // Mimic original autoplay behavior
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause() // Ensure player is paused before release
        releasePlayer()
    }

    private fun releasePlayer() {
        mMovieView?.setPlayer(null) // Detach player from MovieView
        player?.release()
        player = null
    }

    override fun onRestart() {
        super.onRestart()
        // Player will be re-initialized in onStart if it was released.
        if (!isInPictureInPictureMode) {
            mMovieView?.showControls()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(config = newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            adjustFullScreen(config = resources.configuration)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent?) {
                    if (intent == null || ACTION_MEDIA_CONTROL != intent.action) {
                        return
                    }
                    val controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
                    when (controlType) {
                        CONTROL_TYPE_PLAY -> mMovieView?.play()
                        CONTROL_TYPE_PAUSE -> mMovieView?.pause()
                    }
                }
            }
            registerReceiver(
                mReceiver,
                IntentFilter(ACTION_MEDIA_CONTROL),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            unregisterReceiver(mReceiver)
            mReceiver = null
            if (mMovieView != null && player?.isPlaying == false) { // Check player state
                mMovieView!!.showControls()
            }
        }
    }

    fun minimize() {
        if (mMovieView == null) { // Player null check is implicitly handled by MovieView
            return
        }
        mMovieView!!.hideControls()

        val movieViewWidth: Int = mMovieView!!.width
        val movieViewHeight: Int = mMovieView!!.height
        val rational: Rational = if (movieViewWidth > 0 && movieViewHeight > 0) {
            Rational(movieViewWidth, movieViewHeight)
        } else {
            val videoSize: VideoSize? = player?.videoSize
            if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
                Rational(videoSize.width, videoSize.height)
            } else {
                Rational(16, 9) // Default aspect ratio
            }
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

    private inner class SwitchActivityOnClick : View.OnClickListener {
        override fun onClick(view: View) {
            startActivity(Intent(view.context, MediaSessionPlaybackActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val ACTION_MEDIA_CONTROL = "media_control"
        private const val EXTRA_CONTROL_TYPE = "control_type"
        private const val REQUEST_PLAY = 1
        private const val REQUEST_PAUSE = 2
        private const val REQUEST_INFO = 3
        private const val CONTROL_TYPE_PLAY = 1
        private const val CONTROL_TYPE_PAUSE = 2
    }
}
