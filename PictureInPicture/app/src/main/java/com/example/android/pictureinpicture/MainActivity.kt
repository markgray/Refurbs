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
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.pictureinpicture.MainActivity.Companion.ACTION_MEDIA_CONTROL
import com.example.android.pictureinpicture.MainActivity.Companion.CONTROL_TYPE_PAUSE
import com.example.android.pictureinpicture.MainActivity.Companion.CONTROL_TYPE_PLAY
import com.example.android.pictureinpicture.MainActivity.Companion.EXTRA_CONTROL_TYPE
import com.example.android.pictureinpicture.MainActivity.Companion.REQUEST_INFO
import com.example.android.pictureinpicture.MainActivity.Companion.REQUEST_PAUSE
import com.example.android.pictureinpicture.MainActivity.Companion.REQUEST_PLAY
import com.example.android.pictureinpicture.widget.MovieView
import com.example.android.pictureinpicture.widget.MovieView.MovieListener

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

    /**
     * [View.OnClickListener] for the button with id `R.id.pip` ("Enter Picture-in-Picture mode").
     * When this button is clicked we call our method [minimize] (Enters Picture-in-Picture mode).
     */
    private val mOnClickListener = View.OnClickListener { view: View ->

        /**
         * Called when the button with id R.id.pip ("Enter Picture-in-Picture mode") is
         * clicked. We switch on the id of the `View view` parameter, and if the id is
         * R.id.pip we call our method `minimize` to enter Picture-in-Picture mode and
         * then break.
         *
         * @param view View that was clicked
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
                R.drawable.ic_pause_24dp, mPause, CONTROL_TYPE_PAUSE, REQUEST_PAUSE)
        }

        /**
         * Called when the video is paused or finished. Since the video has stopped playing we call
         * our method [updatePictureInPictureActions] to change the action item to play the video,
         * using `R.drawable.ic_play_arrow_24dp` as the icon, [mPlay] ("Play") as the title,
         * [CONTROL_TYPE_PLAY] as the type of action, and [REQUEST_PLAY] as the request code for
         * the pending intent.
         */
        override fun onMovieStopped() {
            // The video stopped or reached its end. In PiP mode, we want to show an action
            // item to play the video.
            updatePictureInPictureActions(
                R.drawable.ic_play_arrow_24dp, mPlay, CONTROL_TYPE_PLAY, REQUEST_PLAY)
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
     * Update the state of pause/resume action item in Picture-in-Picture mode. First we allocate an
     * [ArrayList] for the [ArrayList] of [RemoteAction] variable `val actions`. We create a
     * broadcast intent to initialize [PendingIntent] variable `val intent` using our [Int] parameter
     * [requestCode] as the request code, with the intent to be broadcast having the action
     * [ACTION_MEDIA_CONTROL] ("media_control" - Intent action for media controls from
     * Picture-in-Picture mode), and [PendingIntent.FLAG_IMMUTABLE] for the flags. We create [Icon]
     * variable `val icon` from the drawable resource with the id given by our [Int] parameter
     * [iconId]. We add a new [RemoteAction] to our list of remote actions in `actions` created
     * using `icon` as the icon, our [String] parameter [title] as the title and the content
     * description, and `intent` as the pending intent that will be used when the action item is
     * clicked. Next we add a fixed [RemoteAction] to `actions` that is constructed the drawable
     * `R.drawable.ic_info_24dp` as the icon, the string with the id `R.string.info` ("Info") as the
     * title, the string with the id `R.string.info_description` ("Information about this video") as
     * the content description, and a pending intent constructed with the request code [REQUEST_INFO],
     * an intent with the action [Intent.ACTION_VIEW], and a Intent data  URI parsed from the string
     * with id `R.string.info_uri` ("[...](https://peach.blender.org/)"). We then set the actions of
     * our [PictureInPictureParams.Builder] field [mPictureInPictureParamsBuilder] to `actions`. We
     * then build [mPictureInPictureParamsBuilder] to supply the argument to the method
     * [setPictureInPictureParams] which updates the properties of the picture in picture activity,
     * or sets it to be used later when [enterPictureInPictureMode] is called.
     *
     * @param iconId      The icon to be used.
     * @param title       The title text.
     * @param controlType The type of the action. either [CONTROL_TYPE_PLAY] or [CONTROL_TYPE_PAUSE].
     * @param requestCode The request code for the [PendingIntent].
     */
    fun updatePictureInPictureActions(
        @DrawableRes iconId: Int, title: String?, controlType: Int, requestCode: Int) {
        val actions = ArrayList<RemoteAction>()

        // This is the PendingIntent that is invoked when a user clicks on the action item.
        // You need to use distinct request codes for play and pause, or the PendingIntent won't
        // be properly updated.
        val intent = PendingIntent.getBroadcast(
            this@MainActivity,
            requestCode,
            Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
            PendingIntent.FLAG_IMMUTABLE)
        val icon = Icon.createWithResource(this@MainActivity, iconId)
        actions.add(RemoteAction(icon, title!!, title, intent))

        // Another action item. This is a fixed action.
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

        // This is how you can update action items (or aspect ratio) for Picture-in-Picture mode.
        // Note this call can happen even when the app is not in PiP mode. In that case, the
        // arguments will be used for at the next call of #enterPictureInPictureMode.
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

        // Prepare string resources for Picture-in-Picture actions.
        mPlay = getString(R.string.play)
        mPause = getString(R.string.pause)

        // View references
        mMovieView = findViewById(R.id.movie)
        mScrollView = findViewById(R.id.scroll)
        val switchExampleButton = findViewById<Button>(R.id.switch_example)
        switchExampleButton.text = getString(R.string.switch_media_session)
        switchExampleButton.setOnClickListener(SwitchActivityOnClick())

        // Set up the video; it automatically starts.
        mMovieView!!.setMovieListener(mMovieListener)
        findViewById<View>(R.id.pip).setOnClickListener(mOnClickListener)
    }

    /**
     * Called when you are no longer visible to the user (called when entering PIP mode when using
     * Media Session example but not in Custom Actions example). We call the [MovieView.pause]
     * method of our [MovieView] field [mMovieView], then call our super's implementation of
     * `onStop`.
     */
    override fun onStop() {
        // On entering Picture-in-Picture mode, onPause is called, but not onStop.
        // For this reason, this is the place where we should pause the video playback.
        mMovieView!!.pause()
        super.onStop()
    }

    /**
     * Called after [onStop] when the current activity is being re-displayed to the user (the user
     * has navigated back to it). It will be followed by [onStart] and then [onResume]. First we
     * call through to our super's implementation of `onRestart`, then if we are not in Picture in
     * Picture mode we call the [MovieView.showControls] method of our [MovieView] field [mMovieView]
     * to show the video controls so the video can be easily resumed.
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
     * Note that this will only be called if you have selected configurations you would like to
     * handle with the [android.R.attr.configChanges] attribute in your manifest. (Our manifest
     * uses:
     *
     * android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
     *
     *  * `screenSize` - The current available screen size has changed. If applications don't target
     *  at least HONEYCOMB_MR2 then the activity will always handle this itself (the change will not
     *  result in a restart). This represents a change in the currently available size, so will
     *  change when the user switches between landscape and portrait.
     *
     *  * `smallestScreenSize` - The physical screen size has changed. If applications don't target
     *  at least HONEYCOMB_MR2 then the activity will always handle this itself (the change will not
     *  result in a restart). This represents a change in size regardless of orientation, so will
     *  only change when the actual physical screen size has changed such as switching to an
     *  external display.
     *
     *  * `screenLayout` - The screen layout has changed. This might be caused by a different
     *  display being activated.
     *
     *  * `orientation` - The screen orientation has changed, that is the user has rotated the device.
     *
     * If any configuration change occurs that is not selected to be reported by that attribute,
     * then instead of reporting it the system will stop and restart the activity (to have it
     * launched with the new configuration). At the time that this function has been called, your
     * Resources object will have been updated to return resource values matching the new
     * configuration.
     *
     * First we call our super's implementation of `onConfigurationChanged`, then we call our
     * method [adjustFullScreen] to adjust immersive full-screen flags depending on the new
     * screen orientation.
     *
     * @param newConfig The new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(config = newConfig)
    }

    /**
     * Called when the current [Window] of the activity gains or loses focus. This is the best
     * indicator of whether this activity is visible to the user. First we call through to our
     * super's implementation of `onWindowFocusChanged`, then if we now have focus we call our
     * method [adjustFullScreen] with the current configuration of our package [Resources] to
     * adjust immersive full-screen flags depending on the new screen orientation.
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
     * call through to our super's implementation of `onPictureInPictureModeChanged`. Then if
     * our [Boolean] parameter [isInPictureInPictureMode] is:
     *
     *  * `true` - we set our [BroadcastReceiver] field [mReceiver] to a new instance whose override
     * of [BroadcastReceiver.onReceive] starts or pauses [MovieView] field [mMovieView] iff the
     * action of the intent received is [ACTION_MEDIA_CONTROL], and the extra stored under the key
     * [EXTRA_CONTROL_TYPE] is [CONTROL_TYPE_PLAY] or [CONTROL_TYPE_PAUSE] respectively. Finally we
     * call [registerReceiver] to register [mReceiver] as a broadcast receiver for the action
     * [ACTION_MEDIA_CONTROL].
     *
     *  * `false` - we unregister [BroadcastReceiver] field [mReceiver] as a broadcast receiver
     *  and set it to `null`. If our [MovieView] field [mMovieView] is not `null`, and its
     *  [MovieView.isPlaying] method returns `false`, we call its [MovieView.showControls] method.
     *
     * @param isInPictureInPictureMode `true` if the activity is in picture-in-picture mode.
     * @param newConfig            The new configuration of the activity with the state
     * `isInPictureInPictureMode`.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Starts receiving events from action items in PiP mode
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent?) {
                    if (intent == null
                        || ACTION_MEDIA_CONTROL != intent.action) {
                        return
                    }

                    // This is where we are called back from Picture-in-Picture action
                    // items.
                    val controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
                    when (controlType) {
                        CONTROL_TYPE_PLAY -> mMovieView!!.play()
                        CONTROL_TYPE_PAUSE -> mMovieView!!.pause()
                    }
                }
            }
            registerReceiver(
                mReceiver,
                IntentFilter(ACTION_MEDIA_CONTROL),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            // We are out of PiP mode. We can stop receiving events from it.
            unregisterReceiver(mReceiver)
            mReceiver = null
            // Show the video controls if the video is not playing
            if (mMovieView != null && !mMovieView!!.isPlaying) {
                mMovieView!!.showControls()
            }
        }
    }

    /**
     * Enters Picture-in-Picture mode. If our [MovieView] field [mMovieView] is `null` we return
     * having done nothing. Otherwise we call its [MovieView.hideControls] method to hide the
     * controls before entering picture-in-picture mode. Then we initialize [Rational] variable
     * `val aspectRatio` with the width over height of [mMovieView] and use it to set the aspect
     * ratio of our [PictureInPictureParams.Builder] field [mPictureInPictureParamsBuilder] (which
     * we build for no apparent reason and throw away the results). Finally we call the method
     * [enterPictureInPictureMode] with the [PictureInPictureParams] that result from once again
     * building our [PictureInPictureParams.Builder] field [mPictureInPictureParamsBuilder].
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

    /**
     * Adjusts immersive full-screen flags depending on the screen orientation. First we initialize
     * [View] variable `val decorView` by retrieving the top-level window decor view (containing the
     * standard window frame/decorations and the client's content inside of that) of the current
     * window of our activity. Then if:
     *
     *  * The [Configuration.orientation] field of [Configuration] parameter [config] is
     *  [Configuration.ORIENTATION_LANDSCAPE] - we set the system UI visibility of [View] variable
     *  `decorView` to the inclusive or of the flags that are necessary to totally minimise the
     *  system UI (our movie will occupy the entire screen). Then we set the visibility of our
     *  [ScrollView] field [mScrollView] to [View.GONE], and call the [MovieView.setAdjustViewBounds]
     *  method of [MovieView] field [mMovieView] with the argument `false` in order to set its
     *  background to BLACK and schedule a layout pass of the view tree.
     *
     *  * If the orientation is *not* [Configuration.ORIENTATION_LANDSCAPE] - we set the system UI
     *  visibility flags to [View.SYSTEM_UI_FLAG_LAYOUT_STABLE], set the visibility of our [ScrollView]
     *  field [mScrollView] to [View.VISIBLE], and call the [MovieView.setAdjustViewBounds] method
     *  of [MovieView] field [mMovieView] with the argument `true` in order to set its background to
     *  `null` and schedule a layout pass of the view tree.
     *
     * @param config The current [Configuration].
     */
    private fun adjustFullScreen(config: Configuration) {
        val decorView: View = window.decorView
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            @Suppress("DEPRECATION") // TODO: replace with WindowInsetsController
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            mScrollView!!.visibility = View.GONE
            mMovieView!!.setAdjustViewBounds(false)
        } else {
            @Suppress("DEPRECATION") // TODO: replace with WindowInsetsController
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            mScrollView!!.visibility = View.VISIBLE
            mMovieView!!.setAdjustViewBounds(true)
        }
    }

    /**
     * Launches [MediaSessionPlaybackActivity] and closes this activity. This is the
     * [View.OnClickListener] for the button with id `R.id.switch_example` ("Switch to
     * using MediaSession").
     */
    private inner class SwitchActivityOnClick : View.OnClickListener {
        override fun onClick(view: View) {
            startActivity(Intent(view.context, MediaSessionPlaybackActivity::class.java))
            finish()
        }
    }

    companion object {
        /**
         * Intent action for media controls from Picture-in-Picture mode.
         */
        private const val ACTION_MEDIA_CONTROL = "media_control"

        /**
         * Intent extra for media controls from Picture-in-Picture mode.
         */
        private const val EXTRA_CONTROL_TYPE = "control_type"

        /**
         * The request code for play action [PendingIntent].
         */
        private const val REQUEST_PLAY = 1

        /**
         * The request code for pause action [PendingIntent].
         */
        private const val REQUEST_PAUSE = 2

        /**
         * The request code for info action [PendingIntent].
         */
        private const val REQUEST_INFO = 3

        /**
         * The intent extra value for play action.
         */
        private const val CONTROL_TYPE_PLAY = 1

        /**
         * The intent extra value for pause action.
         */
        private const val CONTROL_TYPE_PAUSE = 2
    }
}
