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
import android.net.Uri
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
     * The bottom half of the screen, hidden on landscape
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

    /**
     * The [ExoPlayer] instance used for playing the video.
     * It is initialized in [onStart] and released in [onStop].
     */
    private var player: ExoPlayer? = null

    /**
     * A [View.OnClickListener] that enters Picture-in-Picture mode when the view with
     * id `R.id.pip` is clicked.
     */
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

    /**
     * Updates the action items to be shown in Picture-in-Picture mode. These are the buttons that
     * are displayed in the Picture-in-Picture mode window.
     *
     * We start by initializing our [ArrayList] of [RemoteAction] variable `actions` to a new
     * instance. We initialize our [PendingIntent] variable `val intent` by calling
     * [PendingIntent.getBroadcast] with the following parameters:
     *  - `context`: this@MainActivity
     *  - `requestCode` our [Int] parameter [requestCode].
     *  - `intent`: an [Intent] whose action is [ACTION_MEDIA_CONTROL] with an extra storing our
     *  [Int] parameter [controlType] under the key [EXTRA_CONTROL_TYPE].
     *  - `flags`: [PendingIntent.FLAG_IMMUTABLE]
     *
     * The we initialize our [Icon] variable `val icon` by calling [Icon.createWithResource] with the
     * `context` argument this@MainActivity and the `resid` argument our [Int] parameter [iconId].
     * We add a [RemoteAction] to our [ArrayList] variable `actions` constructed using the following
     * arguments:
     *  - `icon`: the [Icon] we just created, variable `icon`.
     *  - `title`: our [String] parameter [title].
     *  - `contentDescription`: our [String] parameter [title].
     *  - `intent`: the [PendingIntent] we just created, variable `intent`.
     *
     * We add a [RemoteAction] to our [ArrayList] variable `actions` constructed using the following
     * arguments:
     *  - `icon`: an [Icon] created using [Icon.createWithResource] with the `context` argument
     *  this@MainActivity and the `resid` argument `R.drawable.ic_info_24dp`.
     *  - `title`: the string with resource id `R.string.info` ("Info").
     *  - `contentDescription`: the string with resource id `R.string.info_description`
     *  ("Information about this video").
     *  - `intent`: a [PendingIntent] constructed using this@MainActivity as the `context`, the
     *  `requestCode` [REQUEST_INFO], the `intent` [Intent] constructed using [Intent.ACTION_VIEW]
     *  as the `action` and the `uri` [Uri] constructed using the string with resource id
     *  `R.string.info_uri` ("https://peach.blender.org") as the `uri`, and the `flags` argument of
     *  the [PendingIntent] is [PendingIntent.FLAG_IMMUTABLE].
     *
     * Next we call the [PictureInPictureParams.Builder.setActions] method of our
     * [PictureInPictureParams.Builder] property [mPictureInPictureParamsBuilder] with the
     * [ArrayList] of [RemoteAction] variable `actions` as the argument. Finally we call the
     * [setPictureInPictureParams] method of this@MainActivity with the [PictureInPictureParams]
     * built by calling the [PictureInPictureParams.Builder.build] method of
     * [mPictureInPictureParamsBuilder].
     *
     * @param iconId The icon for the main action item.
     * @param title The title for the main action item.
     * @param controlType The type of the control, either [CONTROL_TYPE_PLAY] or [CONTROL_TYPE_PAUSE].
     * @param requestCode The request code for the [PendingIntent] of the main action item.
     */
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
        actions.add(
            RemoteAction(
                /* icon = */ icon,
                /* title = */ title!!,
                /* contentDescription = */ title,
                /* intent = */ intent
            )
        )

        actions.add(
            RemoteAction(
                /* icon = */ Icon.createWithResource(
                    this@MainActivity,
                    R.drawable.ic_info_24dp
                ),
                /* title = */ getString(R.string.info),
                /* contentDescription = */ getString(R.string.info_description),
                /* intent = */ PendingIntent.getActivity(
                    /* context = */ this@MainActivity,
                    /* requestCode = */ REQUEST_INFO,
                    /* intent = */ Intent(
                        /* action = */ Intent.ACTION_VIEW,
                        /* uri = */ getString(R.string.info_uri).toUri()
                    ),
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE
                )
            )
        )
        mPictureInPictureParamsBuilder.setActions(actions)
        setPictureInPictureParams(mPictureInPictureParamsBuilder.build())
    }

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge-to-edge
     * display then we call super's implementation of `onCreate`. We set our content view to our
     * layout file `R.layout.activity_main`. We intialize our [LinearLayout] variable `rootView`
     * by finding the [LinearLayout] with resource id `R.id.activity_main`. We call the
     * [ViewCompat.setOnApplyWindowInsetsListener] method with the arguments `rootView` and a
     * lambda that accepts the [View] passed it in variable `v` and the [WindowInsetsCompat]
     * passed it in variable `windowInsets`, then it initializes its [Insets] variable `insets`
     * to the [WindowInsetsCompat.getInsets] of the [WindowInsetsCompat] variable `windowInsets`
     * for the `typeMask` [WindowInsetsCompat.Type.systemBars], and finally it updates the
     * [ViewGroup.MarginLayoutParams] of the [View] variable `v` by calling its [updateLayoutParams]
     * method with a lambda that sets the `leftMargin` to `insets.left`, the `rightMargin` to
     * `insets.right`, the `topMargin` to `insets.top`, and the `bottomMargin` to `insets.bottom`,
     * and returns [WindowInsetsCompat.CONSUMED] to indicate that the insets have been consumed.
     *
     * Next we initialize our [String] property `mPlay` to the string with resource id `R.string.play`
     * ("Play") and our [String] property `mPause` to the string with resource id `R.string.pause`
     * ("Pause"). We initialize our [MovieView] variable `mMovieView` by finding the view with
     * resource id `R.id.movie`. We initialize our [ScrollView] variable `mScrollView` by finding
     * the view with id `R.id.scroll`. We initialize our [Button] variable `switchExampleButton`
     * by finding the view with with the id `R.id.switch_example`. We set the text of the
     * [Button] variable `switchExampleButton` to the string with resource id `R.string.switch_media_session`
     * ("Switch to using MediaSession"). We set the [View.OnClickListener] of the [Button]
     * variable `switchExampleButton` to an instance of [SwitchActivityOnClick]. We call the
     * [MovieView.setMovieListener] method of the [MovieView] property [mMovieView] with the
     * [MovieListener] property [mMovieListener]. Finally we find the [View] whose `id` is
     * `R.id.pip` and set its [View.OnClickListener] to the [View.OnClickListener] property
     * [mOnClickListener].
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

    /**
     * Called when the activity is becoming visible to the user. We call our super's implementation
     * of `onStart`, then call our method [initializePlayer] to initialize or re-initialize the
     * [ExoPlayer] and start playback.
     */
    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    /**
     * Initializes the [ExoPlayer] instance if it's not already created.
     * TODO: Continue here.
     *
     * This function performs the following steps:
     *  1. Checks if the `player` is null. If it is, proceeds with initialization, if it is not
     *  it returns early.
     *  2. Creates a new [ExoPlayer] instance using `ExoPlayer.Builder`.
     *  3. Associates the newly created player with the `mMovieView`.
     *  4. Checks if `mMovieView` is available and has a valid video resource ID.
     *  5. Constructs a [MediaItem] with the video's URI (derived from the resource ID),
     *  a unique media ID, and metadata such as the title.
     *  6. Sets the [MediaItem] on the player.
     *  7. Prepares the player for playback.
     *  8. Sets `playWhenReady` to true to start playback automatically, mimicking the
     *  original `VideoView` autoplay behavior.
     */
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
