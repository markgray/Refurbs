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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.pictureinpicture.widget.MovieView
import com.example.android.pictureinpicture.widget.MovieView.MovieListener

/**
 * Demonstrates usage of Picture-in-Picture when using [MediaSessionCompat].
 */
class MediaSessionPlaybackActivity : AppCompatActivity() {
    /**
     * The [MediaSessionCompat] instance we use for interaction with media controllers, volume
     * keys, media buttons, and transport controls.
     */
    private var mSession: MediaSessionCompat? = null

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
     * [View.OnClickListener] for the button with id `R.id.pip` ("Enter Picture-in-Picture mode"),
     * it consists of an anonymous class whose `onClick` override calls our method [minimize]
     * if the view that was clicked had the id `R.id.pip`.
     */
    private val mOnClickListener = View.OnClickListener { view: View ->
        /**
         * Called when the button with id R.id.pip ("Enter Picture-in-Picture mode") is
         * clicked. We switch on the id of the view, and if it is R.id.pip we call our method
         * `minimize` then break.
         *
         * @param view view that was clicked
         */
        if (view.id == R.id.pip) {
            minimize()
        }
    }

    /**
     * Callbacks from the [MovieView] showing the video playback.
     */
    private val mMovieListener: MovieListener = object : MovieListener() {
        /**
         * Called when the video is started or resumed. We call our method [updatePlaybackState]
         * to update the media session `state` to [PlaybackStateCompat.STATE_PLAYING], the current
         * `position` of the movie to the [MovieView.currentPosition] field of [MovieView] field
         * [mMovieView], and `mediaId` the raw resource id of the video that is playing to the
         * [MovieView.videoResourceId] field of [MovieView] field [mMovieView].
         */
        override fun onMovieStarted() {
            // We are playing the video now. Update the media session state and the PiP
            // window will
            // update the actions.
            updatePlaybackState(
                state = PlaybackStateCompat.STATE_PLAYING,
                position = mMovieView!!.currentPosition,
                mediaId = mMovieView!!.videoResourceId
            )
        }

        /**
         * Called when the video is paused or finished. We call our method [updatePlaybackState]
         * to update the media session state to [PlaybackStateCompat.STATE_PAUSED], the current
         * `position` of the movie to the [MovieView.currentPosition] field of [MovieView] field
         * [mMovieView], and `mediaId` the raw resource id of the video that is playing to the
         * [MovieView.videoResourceId] field of [MovieView] field [mMovieView].
         */
        override fun onMovieStopped() {
            // The video stopped or reached its end. Update the media session state and the
            // PiP window will
            // update the actions.
            updatePlaybackState(
                state = PlaybackStateCompat.STATE_PAUSED,
                position = mMovieView!!.currentPosition,
                mediaId = mMovieView!!.videoResourceId
            )
        }

        /**
         * Called when this view should be minimized. We just call our method [minimize]
         * to enter Picture-in-Picture mode.
         */
        override fun onMovieMinimized() {
            // The MovieView wants us to minimize it. We enter Picture-in-Picture mode now.
            minimize()
        }
    }

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.activity_main`. We
     * initialize our [MovieView] field [mMovieView] by finding the view with id `R.id.movie`, and
     * [ScrollView] field [mScrollView] by finding the view with id `R.id.scroll`. We initialize our
     * [Button] variable `val switchExampleButton` by finding the view with id `R.id.switch_example`,
     * set its text to the string with id `R.string.switch_custom` ("Switch to custom actions
     * example"), and set its [View.OnClickListener] to a new instance of [SwitchActivityOnClick]
     * (Starts the activity [MainActivity] running instead of us). We set the [MovieListener] of
     * [MovieView] field [mMovieView] to [MovieListener] field [mMovieListener], then find the
     * button with the id `R.id.pip` ("Enter Picture-in-Picture mode") and set its
     * [View.OnClickListener] to our [View.OnClickListener] field [mOnClickListener].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<LinearLayout>(R.id.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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

        // View references
        mMovieView = findViewById(R.id.movie)
        mScrollView = findViewById(R.id.scroll)
        val switchExampleButton = findViewById<Button>(R.id.switch_example)
        switchExampleButton.text = getString(R.string.switch_custom)
        switchExampleButton.setOnClickListener(SwitchActivityOnClick())

        // Set up the video; it automatically starts.
        mMovieView!!.setMovieListener(mMovieListener)
        findViewById<View>(R.id.pip).setOnClickListener(mOnClickListener)
    }

    /**
     * Called after [.onCreate]  or after [onRestart] when the activity had been stopped, but is now
     * again being displayed to the user. We call our super's implementation of `onStart`, then call
     * our method [initializeMediaSession] to create and configure our [MediaSessionCompat] field
     * [mSession].
     */
    override fun onStart() {
        super.onStart()
        initializeMediaSession()
    }

    /**
     * Called from our [onStart] override to create and configure our [MediaSessionCompat] field
     * [mSession]. First we initialize our field [mSession] with a new instance, then we set its
     * flags [MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS] (indicates that it can handle media
     * button events), and [MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS] (indicates that it
     * handles transport control commands through its [MediaSessionCompat.Callback]). Then we set
     * it to be in the active state. Then we set the media controller for our activity to a
     * controller instance of [mSession].
     *
     * We initialize [MediaMetadataCompat] variable `val metadata` by constructing a
     * [MediaMetadataCompat.Builder], put the [String] title of the movie being played
     * in the metadata under the key [MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE], and
     * then building that builder. We then set the meta data of [mSession] to `metadata`.
     *
     * We initialize [MediaSessionCallback] variable `val mMediaSessionCallback` with an instance
     * using our [MovieView] field [mMovieView] as its [MovieView], and set the callback of
     * [mSession] to it.
     *
     * If [MovieView] field [mMovieView] is currently playing we initialize [Int] variable
     * `val state` to [PlaybackStateCompat.STATE_PLAYING], otherwise we initialize it to
     * [PlaybackStateCompat.STATE_PAUSED].
     *
     * Finally we call our method [updatePlaybackState] to update the playback `state` of
     * [MediaSessionCompat] field [mSession] to `state`, with the `playbackActions`
     * [MEDIA_ACTIONS_ALL], the current `position` of [mMovieView], and as the `mediaId`
     * the video resource ID of [mMovieView].
     */
    private fun initializeMediaSession() {
        mSession = MediaSessionCompat(this, TAG)
        @Suppress("DEPRECATION")
        mSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mSession!!.isActive = true
        MediaControllerCompat.setMediaController(this, mSession!!.controller)
        val metadata: MediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mMovieView!!.title)
            .build()
        mSession!!.setMetadata(metadata)
        val mMediaSessionCallback = MediaSessionCallback(movieView = mMovieView)
        mSession!!.setCallback(mMediaSessionCallback)
        val state = if (mMovieView!!.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        updatePlaybackState(
            state = state,
            playbackActions = MEDIA_ACTIONS_ALL,
            position = mMovieView!!.currentPosition,
            mediaId = mMovieView!!.videoResourceId
        )
    }

    /**
     * Called when you are no longer visible to the user. First we call our super's implementation
     * of `onStop`, then we call the [MovieView.pause] method of [MovieView] field [mMovieView],
     * release our [MediaSessionCompat] fiedl [mSession], and set it to `null`.
     */
    override fun onStop() {
        super.onStop()
        // On entering Picture-in-Picture mode, onPause is called, but not onStop.
        // For this reason, this is the place where we should pause the video playback.
        mMovieView!!.pause()
        mSession!!.release()
        mSession = null
    }

    /**
     * Called after [onStop] when the current activity is being re-displayed to the user (the user
     * has navigated back to it). It will be followed by [onStart] and then [onResume]. First we
     * call our super's implementation of `onRestart`, then if we are not in picture in picture mode
     * we call the [MovieView.showControls] method of [MovieView] field [mMovieView] to show the
     * video controls so the video can be easily resumed.
     */
    override fun onRestart() {
        super.onRestart()
        if (!isInPictureInPictureMode) {
            // Show the video controls so the video can be easily resumed.
            mMovieView!!.showControls()
        }
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     * This is called because our activity has as an attribute in the manifest file:
     * android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation", and we will
     * be called whenever one of these configuration changes occurs. First we call our super's
     * implementation of `onConfigurationChanged`, then we call our method [adjustFullScreen]
     * with our [Configuration] parameter [newConfig] to adjust the immersive full-screen flags
     * depending on the screen orientation.
     *
     * @param newConfig The new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(newConfig)
    }

    /**
     * Called when the current [Window] of the activity gains or loses focus. First we call our
     * super's implementation of `onWindowFocusChanged`, then if we now have focus we use an
     * [Resources] instance for our application's package to retrieve the current [Configuration]
     * that is in effect and pass that to our method [adjustFullScreen] to adjust the immersive
     * full-screen flags depending on the screen orientation.
     *
     * @param hasFocus Whether the window of this activity has focus.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            adjustFullScreen(resources.configuration)
        }
    }

    /**
     * Called by the system when the activity changes to and from picture-in-picture mode. First we
     * call our super's implementation of `onPictureInPictureModeChanged`, then if we are not in
     * picture-in-picture mode AND [MovieView] field [mMovieView] is not `null`, AND it is playing
     * we call the [MovieView.showControls] method of [mMovieView] to show the video controls.
     *
     * @param isInPictureInPictureMode `true` if the activity is in picture-in-picture mode.
     * @param newConfig            The new [Configuration] of the activity with the state
     * [isInPictureInPictureMode].
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            // Show the video controls if the video is not playing
            if (mMovieView != null && !mMovieView!!.isPlaying) {
                mMovieView!!.showControls()
            }
        }
    }

    /**
     * Enters Picture-in-Picture mode. If our [MovieView] field [mMovieView] is `null` we return
     * having done nothing. Otherwise we call the [MovieView.hideControls] method of [mMovieView]
     * to hide the controls of the movie. We initialize [Rational] variable `val aspectRatio` with
     * the width and height of [mMovieView], set the aspect ratio of our
     * [PictureInPictureParams.Builder] field [mPictureInPictureParamsBuilder] to `aspectRatio`,
     * (then build it and throw away the result), and finally call the [enterPictureInPictureMode]
     * with the rebuilt [mPictureInPictureParamsBuilder] to put our activity into picture in
     * picture mode.
     */
    fun minimize() {
        if (mMovieView == null) {
            return
        }
        // Hide the controls in picture-in-picture mode.
        mMovieView!!.hideControls()
        // Calculate the aspect ratio of the PiP screen.
        val aspectRatio = Rational(mMovieView!!.width, mMovieView!!.height)
        mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build()
        enterPictureInPictureMode(mPictureInPictureParamsBuilder.build())
    }

    private fun adjustFullScreen(config: Configuration) {
        val decorView: View = window.decorView
        /**
         * Adjusts immersive full-screen flags depending on the screen orientation. First we initialize
         * [View] variable `val decorView` with the top-level window decor view of the current [Window]
         * of the activity. If the [Configuration.orientation] field of our [Configuration] parameter
         * [config] is:
         *
         *  * [Configuration.ORIENTATION_LANDSCAPE] - we set the system UI visibility to the inclusive
         *  or of the bitmaps: [View.SYSTEM_UI_FLAG_LAYOUT_STABLE] (we would like a stable view of the
         *  content insets), [View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION] (we would like the window to
         *  be laid out as if it has requested [View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION], even if it
         *  currently hasn't), [View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN] (we would like the window to be
         *  laid out as if it has requested [View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN], even if it currently
         *  hasn't), [View.SYSTEM_UI_FLAG_HIDE_NAVIGATION] (request that the system navigation be
         *  temporarily hidden), [View.SYSTEM_UI_FLAG_FULLSCREEN] (go into the normal fullscreen mode so
         *  that our content can take over the screen while still allowing the user to interact with the
         *  application), and [View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY] (we would like to remain interactive
         *  when hiding the status bar). We then set the visibility of [ScrollView] field [mScrollView]
         *  to [View.GONE], and call the [MovieView.setAdjustViewBounds] method of [mMovieView] with
         *  `false` as the argument to set the background to BLACK.
         *
         *  * all other orientations - we set the system UI visibility to [View.SYSTEM_UI_FLAG_LAYOUT_STABLE]
         *  (we would like a stable view of the content insets), set the visibility of [ScrollView] field
         *  [mScrollView] to [View.VISIBLE], and call the [MovieView.setAdjustViewBounds] method of
         *  [mMovieView] with `true` as the argument to set the background to `null`.
         *
         * @param config The current [Configuration].
         */
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
     * Overloaded method that persists previously set media actions. We retrieve the current actions
     * from the controller of [MediaSessionCompat] field [mSession] to [Long] variable `val actions`
     * then call the 4 argument version of this method using `actions` as the `playbackActions`
     * argument and the arguments passed us as the other three arguments.
     *
     * @param state    The state of the video, e.g. playing, paused, etc.
     * @param position The position of playback in the video.
     * @param mediaId  The media id related to the video in the media session.
     */
    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        position: Int,
        mediaId: Int
    ) {
        val actions: Long = mSession!!.controller.playbackState.actions
        updatePlaybackState(state, actions, position, mediaId)
    }

    /**
     * Updates the playback state of [MediaSessionCompat] field [mSession]. We initialize our
     * [PlaybackStateCompat.Builder] variable `val builder` with a new instance, set its actions to
     * our [Long] parameter [playbackActions], its active item to our [Int] parameter [mediaId], and
     * its current state to the [Int] parameter [state] for its state, [Int] parameter [position]
     * for its position, and 1.0 as the playback speed. We then build `builder` and use the
     * [MediaSessionCompat.setPlaybackState] method of our [MediaSessionCompat] field [mSession] to
     * set its playback state to the result of the build.
     *
     * @param state           The state of the video, e.g. playing, paused, etc.
     * @param playbackActions capabilities present for this session
     * @param position        The position of playback in the video.
     * @param mediaId         The media id related to the video in the media session.
     */
    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        playbackActions: Long,
        position: Int,
        mediaId: Int
    ) {
        val builder = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setActiveQueueItemId(mediaId.toLong())
            .setState(state, position.toLong(), 1.0f)
        mSession!!.setPlaybackState(builder.build())
    }

    /**
     * Updates the [MovieView] based on the callback actions. Simulates a playlist that will disable
     * actions when you cannot skip through the playlist in a certain direction. Our constructor
     * just saves its [MovieView] parameter as its field [movieView] and sets our [Int] field
     * [indexInPlaylist] to 1.
     *
     * @param movieView the [MovieView] instance that is playing our video
     */
    private inner class MediaSessionCallback(
        private val movieView: MovieView?
    ) : MediaSessionCompat.Callback() {
        /**
         * Which video in our simulated playlist is currently playing (should always be 1?)
         */
        private var indexInPlaylist = 1

        /**
         * Override to handle requests to begin playback. First we call our super's implementation
         * of `onPlay`, then we call the [MovieView.play] method of [MovieView] field [movieView] to
         * start the video playing.
         */
        override fun onPlay() {
            super.onPlay()
            movieView!!.play()
        }

        /**
         * Override to handle requests to pause playback. First we call our super's implementation
         * of `onPause`, then we call the [MovieView.pause] method of [MovieView] field [movieView]
         * to pause the video currently playing.
         */
        override fun onPause() {
            super.onPause()
            movieView!!.pause()
        }

        /**
         * Override to handle requests to skip to the next media item. First we call our super's
         * implementation of `onSkipToNext`, then we call the [MovieView.startVideo] method of
         * [MovieView] field [movieView] to load and start the video playing. If our [Int] field
         * [indexInPlaylist] is NOT less than [PLAYLIST_SIZE] we are done. Otherwise we increment
         * [indexInPlaylist] and if:
         *
         *  * [indexInPlaylist] >= [PLAYLIST_SIZE] - we call our method [updatePlaybackState] to set
         *  the `state` to [PlaybackStateCompat.STATE_PLAYING], the `playbackActions` to the or of
         *  [MEDIA_ACTIONS_PLAY_PAUSE] and [PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS], the current
         *  `position` to that of [MovieView] field [movieView], and as the `mediaId` the current
         *  resource ID of the video being played (we can now skip to the previous video but not to
         *  a nonexistent next video).
         *
         *  * [indexInPlaylist] < [PLAYLIST_SIZE] - we call our method [updatePlaybackState] to set
         *  the `state` to STATE_PLAYING, the `playbackActions` to MEDIA_ACTIONS_ALL, the current
         *  `position` to that of [MovieView] [movieView], and as the `mediaId` the current resource
         *  ID of the video being played (we can now use all controls).
         */
        override fun onSkipToNext() {
            super.onSkipToNext()
            movieView!!.startVideo()
            if (indexInPlaylist < PLAYLIST_SIZE) {
                indexInPlaylist++
                if (indexInPlaylist >= PLAYLIST_SIZE) {
                    updatePlaybackState(
                        state = PlaybackStateCompat.STATE_PLAYING,
                        playbackActions = MEDIA_ACTIONS_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                        position = movieView.currentPosition,
                        mediaId = movieView.videoResourceId)
                } else {
                    updatePlaybackState(
                        state = PlaybackStateCompat.STATE_PLAYING,
                        playbackActions = MEDIA_ACTIONS_ALL,
                        position = movieView.currentPosition,
                        mediaId = movieView.videoResourceId)
                }
            }
        }

        /**
         * Override to handle requests to skip to the previous media item. First we call our super's
         * implementation of `onSkipToPrevious`, then we call the [MovieView.startVideo] method of
         * [MovieView] field [movieView] to load and start the video playing. If our [Int] field
         * [indexInPlaylist] is *not* greater than 0 we are done. Otherwise we decrement
         * [indexInPlaylist] and if:
         *
         *  * [indexInPlaylist] <= 0 - we call our method [updatePlaybackState] to set the `state`
         *  to [PlaybackStateCompat.STATE_PLAYING], the `playbackActions` to the or of
         *  [MEDIA_ACTIONS_PLAY_PAUSE] and [PlaybackStateCompat.ACTION_SKIP_TO_NEXT], the current
         *  `position` to that of [MovieView] field [movieView], and as the `mediaId` the current
         *  resource ID of the video being played (we can now skip to the next video but not to a
         *  nonexistent previous video).
         *
         *  * [indexInPlaylist] > 0 - we call our method [updatePlaybackState] to set the `state` to
         *  [PlaybackStateCompat.STATE_PLAYING], the `playbackActions` to [MEDIA_ACTIONS_ALL], the
         *  current `position` to that of [MovieView] field [movieView], and as the `mediaId` the
         *  current resource ID of the video being played (we can now use all controls).
         */
        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            movieView!!.startVideo()
            if (indexInPlaylist > 0) {
                indexInPlaylist--
                if (indexInPlaylist <= 0) {
                    updatePlaybackState(
                        state = PlaybackStateCompat.STATE_PLAYING,
                        playbackActions = MEDIA_ACTIONS_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
                        position = movieView.currentPosition,
                        mediaId = movieView.videoResourceId)
                } else {
                    updatePlaybackState(
                        state = PlaybackStateCompat.STATE_PLAYING,
                        playbackActions = MEDIA_ACTIONS_ALL,
                        position = movieView.currentPosition,
                        mediaId = movieView.videoResourceId)
                }
            }
        }

    }

    /**
     * Switches to the activity [MainActivity], it is the [View.OnClickListener] for the button
     * with the id `R.id.switch_example` (its text is set to "Switch to custom actions example" in
     * the [onCreate] override of [MediaSessionPlaybackActivity]). First we start the activity
     * [MainActivity], then we call [finish] to close this activity.
     */
    private inner class SwitchActivityOnClick : View.OnClickListener {
        override fun onClick(view: View) {
            startActivity(Intent(view.context, MainActivity::class.java))
            finish()
        }
    }

    companion object {
        /**
         * Size of the simulated playlist.
         */
        const val PLAYLIST_SIZE: Int = 2

        /**
         * TAG used for logging by our [MediaSessionCompat] instance.
         */
        const val TAG: String = "MediaSessionPlaybackActivity"

        /**
         * Used in call to [PlaybackStateCompat.Builder.setActions] in our override of the
         * [MediaSessionCallback.onSkipToNext] and [MediaSessionCallback.onSkipToPrevious]
         * callbacks, it is a bitmask of these available capabilities:
         *
         *  * [PlaybackStateCompat.ACTION_PLAY] - Indicates this session supports the play command.
         *
         *  * [PlaybackStateCompat.ACTION_PAUSE] - Indicates this session supports the pause command.
         *
         *  * [PlaybackStateCompat.ACTION_PLAY_PAUSE] - Indicates this session supports the
         *  play/pause toggle command.
         */
        const val MEDIA_ACTIONS_PLAY_PAUSE: Long = (PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE)

        /**
         * Used in call to [updatePlaybackState] from our method [initializeMediaSession], and in
         * our override of the [MediaSessionCallback.onSkipToNext] and
         * [MediaSessionCallback.onSkipToPrevious] callbacks, it adds to [MEDIA_ACTIONS_PLAY_PAUSE]
         * the two bitmasks:
         *
         *  * [PlaybackStateCompat.ACTION_SKIP_TO_NEXT] - Indicates this session supports the next
         *  command.
         *
         *  * [PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS] - Indicates this session supports the
         *  previous command.
         */
        const val MEDIA_ACTIONS_ALL: Long = (MEDIA_ACTIONS_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    }
}
