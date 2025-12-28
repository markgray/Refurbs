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
import android.content.res.Resources
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.core.graphics.Insets
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
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommands

/**
 * Demonstrates usage of Picture-in-Picture when using Media3.
 */
class MediaSessionPlaybackActivity : AppCompatActivity() {

    /**
     * The [MediaSession] that is used to integrate with the system's media controls,
     * like the lock screen and notification shade. It connects the media player's state
     * with the rest of the Android system.
     */
    private var mSession: MediaSession? = null
    
    /**
     * The [ExoPlayer] instance used for media playback. It is initialized in [initializeMediaSession]
     * and released in [onStop]. It can be `null` if the media session is not active.
     */
    private var player: ExoPlayer? = null

    /**
     * A [PictureInPictureParams.Builder] to build picture-in-picture params.
     * This is kept as a field so it can be reused whenever [minimize] is called.
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
     * [View.OnClickListener] for the button with id `R.id.pip` ("Enter Picture-in-Picture mode"),
     * it consists of an anonymous class whose `onClick` override calls our method [minimize]
     * if the view that was clicked had the id `R.id.pip`.
     */
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
        /**
         * Called when the value of [Player.isPlaying] changes.
         * This is part of the [Player.Listener] interface and is used to react
         * to the player's state changes at the Activity level. For example,
         * this could be used to update UI elements that are not part of the
         * main player view. In this specific implementation, it's a placeholder
         * for any future activity-specific reactions to play/pause events.
         * We ignore it.
         *
         * @param isPlaying `true` if the player is playing, `false` otherwise.
         */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Activity specific reactions to play/pause, if any.
        }

        /**
         * Called when the [MediaMetadata] of the current media item or the playlist changes.
         * This is part of the [Player.Listener] interface and is used to react to changes
         * in the media's metadata, such as the title or artist.
         * In this specific implementation, it's a placeholder for any future activity-specific
         * reactions to metadata changes, like updating the activity's title bar.
         * We ignore it.
         *
         * @param mediaMetadata The new [MediaMetadata].
         */
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // Activity specific reactions to metadata change, if any.
        }
    }

    /**
     * Listener for callbacks from MovieView, specifically for minimize action.
     */
    private val movieViewListener = object : MovieView.MovieListener() {
        /**
         * This is called when the user taps on the minimize button in the [MovieView]'s controls.
         * We just call our method [minimize] to enter Picture-in-Picture mode.
         */
        override fun onMovieMinimized() {
            minimize()
        }
        // onMovieStarted and onMovieStopped are now handled by MovieView's internal Player.Listener
    }

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display then we call super's implementation of `onCreate`, and set our content view
     * to our layout file `R.layout.activity_main`.
     *
     * We intialize our [LinearLayout] variable `rootView` by finding the [LinearLayout] with
     * resource id `R.id.activity_main` then call [ViewCompat.setOnApplyWindowInsetsListener] to
     * take over the policy for applying window insets to `rootView`, with the `listener` argument
     * a lambda that accepts the [View] passed the lambda in variable `v` and the
     * [WindowInsetsCompat] passed the lambda in variable `windowInsets`. It initializes its
     * [Insets] variable `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets`
     * with [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates the layout parameters
     * of `v` to be a [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`,
     * the right margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the
     * bottom margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * Next we initialize our [MovieView] variable `mMovieView` by finding the [MovieView] with
     * resource id `R.id.movie`. We initialize our [ScrollView] variable `mScrollView` by finding
     * the [ScrollView] with resource id `R.id.scroll`. We initialize our [Button] variable
     * `switchExampleButton` by finding the [Button] with resource id `R.id.switch_example`. We set
     * the text of `switchExampleButton` to the string with resource id `R.string.switch_custom`
     * ("Switch to custom actions example"). We set the [View.OnClickListener] of `switchExampleButton`
     * to an instance of [SwitchActivityOnClick]. We set the [MovieView.MovieListener] of [MovieView]
     * proprety [mMovieView] to our property [movieViewListener]. We set the [View.OnClickListener]
     * of the [View] with resource id `R.id.pip` to our property [mOnClickListener].
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down then this [Bundle] contains the data it most recently supplied in [onSaveInstanceState].
     * We do not override [onSaveInstanceState] so it is not used.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<LinearLayout>(R.id.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        mMovieView = findViewById(R.id.movie)
        mScrollView = findViewById(R.id.scroll)
        val switchExampleButton = findViewById<Button>(R.id.switch_example)
        switchExampleButton.text = getString(R.string.switch_custom)
        switchExampleButton.setOnClickListener(SwitchActivityOnClick())

        mMovieView?.setMovieListener(movieListener = movieViewListener) // Set listener for minimize callback
        findViewById<View>(R.id.pip).setOnClickListener(mOnClickListener)
    }

    /**
     * Called when the activity is becoming visible to the user. First we call super's
     * implementation of `onStart`. Then we initialize our [MediaSession] by calling our
     * method [initializeMediaSession].
     */
    override fun onStart() {
        super.onStart()
        initializeMediaSession()
    }

    /**
     * Initializes the [MediaSession] that is used to integrate with the system's media controls.
     * We initialize our [ExoPlayer] variable `player` by building a new [ExoPlayer.Builder] with
     * the [ExoPlayer.Builder] constructor, and then calling its [build] method, and then use the
     * [apply] extension function to add our [Player.Listener] property [activityPlayerListener]
     * as a listener to the [ExoPlayer] property [player]. We call the [MovieView.setPlayer] method
     * of our [MovieView] property [mMovieView] with the [ExoPlayer] property [player] as the
     * argument. We initialize our [MediaSession] property [mSession] by using an instance of
     * [MediaSession.Builder] whose `context` argument is this@MediaSessionPlaybackActivity, and
     * whose `player` argument is the [ExoPlayer] property [player]. We use the
     * [MediaSession.Builder.setCallback] method to set its `callback` property to an instance of
     * [Media3SessionCallback]. We use the [MediaSession.Builder.build] method to build the
     * [MediaSession]. Then if the [MovieView] property [mMovieView] is not `null` and the
     * [ExoPlayer] property [player] is not `null`:
     *  - We initialize our [String] variable `videoUri` to a string constructed by concatenating
     *  the string "android.resource://" with the package name of the current activity, and the
     *  value of [MovieView.mVideoResourceId]. We initialize our [MediaItem] variable `mediaItem`
     *  by using an instance of [MediaItem.Builder] to which we use the [MediaItem.Builder.setMediaId]
     *  method to set its `mediaId` property to the value of [MovieView.mVideoResourceId]. We use the
     *  [MediaItem.Builder.setUri] method to set its `uri` property to `videoUri`, and the
     *  [MediaItem.Builder.setMediaMetadata] method to set its `mediaMetadata` property to a new
     *  [MediaMetadata.Builder] constructed using the [MediaMetadata.Builder.setTitle] method to
     *  set its `title` property to the string "Untitled Video" if the [MovieView.title] property
     *  is `null`, or the [MovieView.title] property otherwise. We use the [MediaItem.Builder.build]
     *  method to build the [MediaItem]. We use the [ExoPlayer.setMediaItem] method to set the
     *  [MediaItem] of [ExoPlayer] property [player] to the [MediaItem] variable `mediaItem`, to
     *  clear its playlist, and reset the position to the default position. We use the
     *  [ExoPlayer.prepare] method to prepare the [ExoPlayer] property [player].
     *  - If the video resource is not available, we do nothing.
     */
    private fun initializeMediaSession() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(activityPlayerListener) // Activity general player listener
        }

        mMovieView?.setPlayer(player) // Provide the player instance to MovieView

        mSession = MediaSession.Builder(this, player!!)
            .setCallback(Media3SessionCallback()) // TODO: Consider chaining .setAttributionTag("your_attribution_tag")
            .build()

        if (mMovieView != null && mMovieView!!.mVideoResourceId != 0) { // Use mVideoResourceId from MovieView
            val videoUri = "android.resource://${packageName}/${mMovieView!!.mVideoResourceId}"
            val mediaItem: MediaItem = MediaItem.Builder()
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

    /**
     * Called when the activity is no longer visible to the user. This is followed by either
     * `onRestart` or `onDestroy`. This implementation handles the cleanup of media-related
     * resources.
     *
     * It performs the following actions:
     *  - Calls our super's implementation of `onStop`.
     *  - Pauses the video playback by calling the [ExoPlayer.pause] method of [ExoPlayer] property
     *  [player] to conserve resources.
     *  - Releases the [MediaSession] to unregister it from the system by calling the
     *  [MediaSession.release] method of the [MediaSession] property [mSession] and sets [mSession]
     *  to `null` to ensure it is garbage collected and re-initialized correctly if the activity is
     *  restarted.
     *  - Removes the listener from the [ExoPlayer] instance to prevent memory leaks by calling the
     *  [ExoPlayer.removeListener] method with the [Player.Listener] property [activityPlayerListener].
     *  - Releases the [ExoPlayer] instance itself to free up all associated resources,
     *  including decoders and media buffers by calling the [ExoPlayer.release] method of the
     *  [ExoPlayer] property [player].
     *  - Sets the [player] reference to `null` to ensure it is garbage collected and
     *  re-initialized correctly if the activity is restarted.
     */
    override fun onStop() {
        super.onStop()
        player?.pause()
        mSession?.release()
        mSession = null
        player?.removeListener(activityPlayerListener)
        player?.release()
        player = null
    }

    /**
     * Called when the activity will be restarted. First we call super's implementation of
     * `onRestart`. Then we check if the [ExoPlayer] property [player] is `null`. If it is, we
     * initialize it by calling our method [initializeMediaSession]. In any case, we check if
     * [isInPictureInPictureMode] is `false`. If it is, we call the [MovieView.showControls]
     * method of the [MovieView] property [mMovieView] to show the controls if [mMovieView]
     * is not `null`.
     */
    override fun onRestart() {
        super.onRestart()
        if (player == null) { // Basic re-initialization
            initializeMediaSession()
        }
        if (!isInPictureInPictureMode) {
            mMovieView?.showControls()
        }
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     * Note that this will only be called if you have selected configurations you would like to
     * handle through the `android:configChanges` attribute in your manifest.
     *
     * We first call our super's implementation of `onConfigurationChanged`, then we call our method
     * [adjustFullScreen] to adjust our UI for the new configuration.
     *
     * @param newConfig The new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(config = newConfig)
    }

    /**
     * Called when the current [Window] of the activity gains or loses focus. First we call our
     * super's implementation of `onWindowFocusChanged`, then we check if the `hasFocus` parameter
     * is `true`. If it is, we call our method [adjustFullScreen] to adjust our UI for the
     * [Configuration] of the [Resources] instance for the application's package returned by
     * [getResources].
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            adjustFullScreen(config = resources.configuration)
        }
    }

    /**
     * Called when the activity enters or exits Picture-in-Picture mode. First we call our super's
     * implementation of `onPictureInPictureModeChanged`, then we check if the `isInPictureInPictureMode`
     * parameter is `false`. If it is, we call the [MovieView.showControls] method of the
     * [MovieView] property [mMovieView] to show the controls if [mMovieView] is not `null` and
     * the [ExoPlayer] property [player] is not `null` and its `isPlaying` property is `false`.
     * If [isInPictureInPictureMode] is `true`, we call the [MovieView.hideControls] method of
     * the [MovieView] property [mMovieView] to hide the controls if [mMovieView] is not `null`.
     *
     * @param isInPictureInPictureMode `true` if the activity is now in Picture-in-Picture mode,
     * `false` otherwise.
     * @param newConfig The new device configuration.
     */
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

    /**
     * Called when the activity wants to enter Picture-in-Picture mode. If our [MovieView] property
     * [mMovieView] is `null` or our [ExoPlayer] property [player] is `null`, we return having done
     * nothing. Otherwise we call the [MovieView.hideControls] method of the [MovieView] property
     * [mMovieView] to hide the controls. We initialize our [Int] variable `movieViewWidth` to the
     * width of the [MovieView] property [mMovieView], and our [Int] variable `movieViewHeight` to
     * the height of the [MovieView] property [mMovieView]. If both `movieViewWidth` and
     * `movieViewHeight` are greater than 0, we initialize our [Rational] variable `rational`
     * to the ratio of `movieViewWidth` to `movieViewHeight`. Otherwise we initialize it to a
     * rational with a numerator of 16 and a denominator of 9. We then call the
     * [PictureInPictureParams.Builder.setAspectRatio] method of our [PictureInPictureParams.Builder]
     * property [mPictureInPictureParamsBuilder] with the [Rational] variable `rational` as the
     * argument. We then call the [enterPictureInPictureMode] method with the [PictureInPictureParams]
     * built from our [PictureInPictureParams.Builder] property [mPictureInPictureParamsBuilder].
     */
    fun minimize() {
        if (mMovieView == null || player == null) {
            return
        }
        mMovieView!!.hideControls()
        val movieViewWidth: Int = mMovieView!!.width
        val movieViewHeight: Int = mMovieView!!.height
        val rational: Rational = if (movieViewWidth > 0 && movieViewHeight > 0) {
            Rational(movieViewWidth, movieViewHeight)
        } else {
            Rational(16, 9)
        }
        mPictureInPictureParamsBuilder.setAspectRatio(rational)
        enterPictureInPictureMode(mPictureInPictureParamsBuilder.build())
    }

    /**
     * Adjusts the activity's UI based on the device's orientation.
     *
     * This function is called when the device's configuration changes, such as during an orientation
     * change, or when the window focus changes. It modifies the system UI visibility and the layout
     * to provide an optimal viewing experience for both landscape and portrait modes.
     *
     * In landscape mode, the activity enters a full-screen, immersive state. The system navigation
     * and status bars are hidden, and the video player ([mMovieView]) is configured to fill the
     * entire screen. The scrollable content area ([mScrollView]) containing other UI elements
     * is hidden to maximize the video playback area.
     *
     * In portrait mode, the activity reverts to a standard layout. The system UI elements (like the
     * status bar) are made visible again. The scrollable content area ([mScrollView]) is shown,
     * and the video player's bounds are adjusted to fit within its layout constraints, allowing
     * other UI elements to be visible on the screen.
     *
     * We initialize our [View] variable `decorView` to the [Window.getDecorView] of the current
     * [Window] of the activity. We then branch on the value of the [Configuration.orientation]:
     *  - If it is [Configuration.ORIENTATION_LANDSCAPE], we set the `systemUiVisibility` of
     *  `decorView` to the result of oring the flags [View.SYSTEM_UI_FLAG_LAYOUT_STABLE],
     *  [View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION], [View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN],
     *  [View.SYSTEM_UI_FLAG_HIDE_NAVIGATION], [View.SYSTEM_UI_FLAG_FULLSCREEN], and
     *  [View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY]. We then set the `visibility` of the [ScrollView]
     *  property [mScrollView] to [View.GONE], and call the [MovieView.setAdjustViewBounds] method
     *  of the [MovieView] property [mMovieView] with the value `false`.
     *  - Otherwise, we set the `systemUiVisibility` of `decorView` to the value
     *  [View.SYSTEM_UI_FLAG_LAYOUT_STABLE]. We then set the `visibility` of the [ScrollView]
     *  property [mScrollView] to [View.VISIBLE], and call the [MovieView.setAdjustViewBounds]
     *  method of the [MovieView] property [mMovieView] with the value `true`.
     *
     * @param config The current [Configuration] of the device, which includes orientation information.
     */
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

    /**
     * The [MediaSession.Callback] we use to handle incoming requests from [MediaController].
     */
    private class Media3SessionCallback : MediaSession.Callback {
        /**
         * Called when a [MediaController] is created for this session.
         *
         * This method is the entry point for a controller connecting to this session. It allows the
         * session to accept or reject the connection and to specify the commands that the controller
         * is allowed to use.
         *
         * In this implementation, we accept all connections and provide a default, empty set of
         * available session and player commands. This means that by default, controllers will be
         * able to use any commands that the underlying player supports, as the session will
         * automatically forward them.
         *
         * For more fine-grained control, you could inspect the `controller`'s information (e.g.,
         * its package name) to decide whether to accept the connection or to limit the commands
         * it can use.
         *
         * @param session The [MediaSession] this callback is associated with.
         * @param controller Information about the connecting [MediaController].
         * @return A [MediaSession.ConnectionResult] indicating whether the connection is accepted and
         * which commands are available to the controller. We use [MediaSession.ConnectionResult.accept]
         * to allow the connection with default commands.
         */
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands: SessionCommands = SessionCommands.Builder().build()
            val playerCommands: Commands = Commands.Builder().build()
            return MediaSession.ConnectionResult.accept(
                /* availableSessionCommands = */ sessionCommands,
                /* availablePlayerCommands = */ playerCommands
            )
        }

        // onPlay, onPause, onSetMediaItem, etc., are not typically overridden here
        // as MediaSession forwards these to the player if commands are available.
        // Custom commands or more complex connection logic would go here.
    }

    /**
     * The [View.OnClickListener] we use to handle the click on the "Switch to custom actions"
     * button. When the button is clicked, we start the [MainActivity] activity and finish this
     * activity.
     */
    private inner class SwitchActivityOnClick : View.OnClickListener {
        override fun onClick(view: View) {
            startActivity(Intent(view.context, MainActivity::class.java))
            finish()
        }
    }
}
