/*
* Copyright 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.notificationchannels

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Display main screen for sample. Displays controls for sending test notifications.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    /**
     * A view model for interacting with the UI elements. Its constructor sets the [OnClickListener]
     * of the buttons in our layout to "this", so it is actually used in spite of the lint warning.
     */
    private var ui: MainUi? = null

    /**
     * Our instance of [NotificationHelper], we use it to call its [NotificationHelper.getNotification1],
     * [NotificationHelper.getNotification2] and [NotificationHelper.notify] methods.
     */
    private var noti: NotificationHelper? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [RelativeLayout] variable `rootView` to the view with ID `R.id.activity_main`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [NotificationHelper] field [noti] with a new instance, and initialize our
     * [MainUi] field [ui] by passing our [RelativeLayout] variable `rootView` (the view with ID
     * `R.id.activity_main` recall) to the [MainUi] constructor (the constructor finds the buttons
     * in this view group and sets their [OnClickListener] to the [MainUi] instance being
     * constructed).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<RelativeLayout>(R.id.activity_main)
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
        noti = NotificationHelper(this)
        ui = MainUi(rootView)
    }

    /**
     * Send activity notifications. This is used by our [OnClickListener.onClick] override to create
     * and post the notification requested when the user clicks one of the four "Send" buttons in
     * our UI. We initialize [Notification.Builder] variable `val nb` to the value of a `when`
     * expression that switches on the value of our [Int] parameter [id]:
     * TODO: Continue here
     *
     *  * [NOTI_PRIMARY1] - we set `nb` to the [Notification.Builder] returned by the
     *  [NotificationHelper.getNotification1] method of our [NotificationHelper] field [noti] using
     *  our [String] parameter [title] as the title of the notification, and the string with
     *  resource id `R.string.primary1_body` ("The content") as the text body.
     *
     *  * [NOTI_PRIMARY2] - we set `nb` to the [Notification.Builder] returned by the
     *  [NotificationHelper.getNotification1] method of our [NotificationHelper] field [noti] using
     *  our [String] parameter [title] as the title of the notification, and the string with
     *  resource id `R.string.primary2_body` ("Second Notification for Primary Channel") as the text
     *  body.
     *
     *  * [NOTI_SECONDARY1] - we set `nb` to the [Notification.Builder] returned by the
     *  [NotificationHelper.getNotification2] method of our [NotificationHelper] field [noti] using
     *  our [String] parameter [title] as the title of the notification, and the string with
     *  resource id `R.string.secondary1_body` ("Notification body text.") as the text body.
     *
     *  * [NOTI_SECONDARY2] - we set `nb` to the [Notification.Builder] returned by the
     *  [NotificationHelper.getNotification2] method of our [NotificationHelper] field [noti] using
     *  our [String] parameter [title] as the title of the notification, and the string with
     *  resource id `R.string.secondary2_body` ("Second Notification for Secondary Channel") as the
     *  text body.
     *
     *  * `else` - we set `nb` to `null`.
     *
     * If `nb` is not `null`, we call the [NotificationHelper.notify] method of [noti] with the
     * notification id `id`, and the notification builder `nb`.
     *
     * @param id    The ID of the notification to create
     * @param title The title of the notification
     */
    fun sendNotification(id: Int, title: String?) {
        val nb: Notification.Builder? = when (id) {
            NOTI_PRIMARY1 -> noti!!.getNotification1(title, getString(R.string.primary1_body))
            NOTI_PRIMARY2 -> noti!!.getNotification1(title, getString(R.string.primary2_body))
            NOTI_SECONDARY1 -> noti!!.getNotification2(title, getString(R.string.secondary1_body))
            NOTI_SECONDARY2 -> noti!!.getNotification2(title, getString(R.string.secondary2_body))
            else -> null
        }
        if (nb != null) {
            noti!!.notify(id = id, notification = nb)
        }
    }

    /**
     * Send an Intent to load system Notification Settings for this app. We initialize our [Intent]
     * variable `val i` with an [Intent] whose action is [Settings.ACTION_APP_NOTIFICATION_SETTINGS]
     * ("android.settings.APP_NOTIFICATION_SETTINGS") which will show notification settings for a
     * single app. We add a [Settings.EXTRA_APP_PACKAGE] extra ("android.provider.extra.APP_PACKAGE")
     * with this application's package as its value. Then we call [startActivity] to launch the
     * [Intent] `i`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun goToNotificationSettings() {
        @SuppressLint("InlinedApi") val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        i.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(i)
    }

    /**
     * Send an [Intent] to load system Notification Settings UI for a particular channel. We
     * initialize [Intent] variable `val i` with an [Intent] whose action is
     * [Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS] (Show notification settings for a single
     * [NotificationChannel]). We add an extra to `i` with our package name stored under the key
     * [Settings.EXTRA_APP_PACKAGE] (package owner of the notification channel settings to display),
     * and an extra containing our [String] parameter [channel] stored under the key
     * [Settings.EXTRA_CHANNEL_ID] (the `getId` of the notification channel settings to display).
     * Finally we use `i` to start the settings activity.
     *
     * @param channel Name of channel to configure
     */
    fun goToNotificationSettings(channel: String?) {
        val i = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        i.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        i.putExtra(Settings.EXTRA_CHANNEL_ID, channel)
        startActivity(i)
    }

    /**
     * View model for interacting with Activity UI elements. (Keeps core logic for sample separate.)
     */
    internal inner class MainUi(root: View) : OnClickListener {
        /**
         * [EditText] with id `R.id.main_primary_title`, displays the text "Primary Channel" to
         * start with, we use its text as the title for the primary channel notifications sent.
         */
        @Suppress("JoinDeclarationAndAssignment") // It is better this way
        val titlePrimary: TextView?

        /**
         * [EditText] with id `R.id.main_secondary_title`, displays the text "Secondary Channel"
         * to start with, we use its text as the title for the secondary channel notifications sent.
         */
        val titleSecondary: TextView

        /**
         * Our constructor. We initialize our `TextView` field `titlePrimary` by finding the view
         * in `root` with the id `R.id.main_primary_title`. We find the buttons with the ids
         * `R.id.main_primary_send1` ("Send 1" in the primary channel section), `R.id.main_primary_send2`
         * ("Send 2" in the primary channel section), and `R.id.main_primary_config` (contains the
         * system settings icon) and set their `OnClickListener` to "this".
         *
         * We initialize our `TextView` field `titleSecondary` by finding the view in `root`
         * with the id `R.id.main_secondary_title`. We find the buttons with the ids
         * `R.id.main_secondary_send1` ("Send 1" in the secondary channel section),
         * `R.id.main_secondary_send2` ("Send 2" in the secondary channel section), and
         * `R.id.main_secondary_config` (contains the system settings icon) and set their
         * `OnClickListener` to "this".
         *
         * Finally we find the view in `root` with the id `R.id.btnA` ("Go to Settings") and set
         * its `OnClickListener` to this.
         *
         * param root View group holding our UI.
         */
        init {
            titlePrimary = root.findViewById<View>(R.id.main_primary_title) as TextView
            (root.findViewById<View>(R.id.main_primary_send1) as Button).setOnClickListener(this)
            (root.findViewById<View>(R.id.main_primary_send2) as Button).setOnClickListener(this)
            (root.findViewById<View>(R.id.main_primary_config) as ImageButton).setOnClickListener(this)
            titleSecondary = root.findViewById<View>(R.id.main_secondary_title) as TextView
            (root.findViewById<View>(R.id.main_secondary_send1) as Button).setOnClickListener(this)
            (root.findViewById<View>(R.id.main_secondary_send2) as Button).setOnClickListener(this)
            (root.findViewById<View>(R.id.main_secondary_config) as ImageButton).setOnClickListener(this)
            (root.findViewById<View>(R.id.btnA) as Button).setOnClickListener(this)
        }

        /**
         * If our [TextView] field [titlePrimary] is not `null` we return the string value of the
         * text it contains to the caller, otherwise we return the empty string "".
         *
         * @return the current text contents of our [TextView] field [titlePrimary]
         */
        private val titlePrimaryText: String
            get() = titlePrimary?.text?.toString() ?: ""

        /**
         * If our [TextView] field [titleSecondary] is not `null` we return the string value of the
         * text it contains to the caller, otherwise we return the empty string "".
         *
         * @return the current text contents of our [TextView] field [titleSecondary]
         */
        private val titleSecondaryText: String
            get() = if (titlePrimary != null) {
                titleSecondary.text.toString()
            } else ""

        /**
         * Called when a view whose [OnClickListener] we are has been clicked. We `when` switch on
         * the `id` of the [View] parameter [view]:
         *
         *  * `R.id.main_primary_send1` ("Send 1" in primary channel section) - we call our method
         *  [sendNotification] with the notification id [NOTI_PRIMARY1], and the title returned by
         *  our property [titlePrimaryText].
         *
         *  * `R.id.main_primary_send2` ("Send 1" in primary channel section) - we call our method
         *  [sendNotification] with the notification id [NOTI_PRIMARY2], and the title returned by
         *  our property [titlePrimaryText].
         *
         *  * `R.id.main_primary_config` (the setting wrench icon) - we call our method
         *  [goToNotificationSettings] with the notification channel
         *  [NotificationHelper.PRIMARY_CHANNEL] ("default").
         *
         *  * `R.id.main_secondary_send1` ("Send 1" in secondary channel section) - we call our
         *  method [sendNotification] with the notification id [NOTI_SECONDARY1], and the title
         *  returned by our property [titleSecondaryText].
         *
         *  * `R.id.main_secondary_send2` ("Send 2" in secondary channel section) - we call our
         *  method [sendNotification] with the notification id [NOTI_SECONDARY2], and the title
         *  returned by our property [titleSecondaryText].
         *
         *  * `R.id.main_secondary_config` (the setting wrench icon) - we call our method
         *  [goToNotificationSettings] with the notification channel
         *  [NotificationHelper.SECONDARY_CHANNEL] ("second").
         *
         *  * `R.id.btnA` ("Go to Settings") - we call the no parameter version of our method
         *  [goToNotificationSettings].
         *
         *  * `else` - we log the message "Unknown click event."
         *
         * @param view The [View] that was clicked.
         */
        @SuppressLint("NonConstantResourceId")
        override fun onClick(view: View) {
            when (view.id) {
                R.id.main_primary_send1 -> sendNotification(NOTI_PRIMARY1, titlePrimaryText)
                R.id.main_primary_send2 -> sendNotification(NOTI_PRIMARY2, titlePrimaryText)
                R.id.main_primary_config -> goToNotificationSettings(NotificationHelper.PRIMARY_CHANNEL)
                R.id.main_secondary_send1 -> sendNotification(NOTI_SECONDARY1, titleSecondaryText)
                R.id.main_secondary_send2 -> sendNotification(NOTI_SECONDARY2, titleSecondaryText)
                R.id.main_secondary_config -> goToNotificationSettings(NotificationHelper.SECONDARY_CHANNEL)
                R.id.btnA -> goToNotificationSettings()
                else -> Log.e(TAG, "Unknown click event.")
            }
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        private val TAG = MainActivity::class.java.simpleName

        /**
         * Notification id for primary notification channel "Send 1" notification
         */
        private const val NOTI_PRIMARY1 = 1100

        /**
         * Notification id for primary notification channel "Send 2" notification
         */
        private const val NOTI_PRIMARY2 = 1101

        /**
         * Notification id for secondary notification channel "Send 1" notification
         */
        private const val NOTI_SECONDARY1 = 1200

        /**
         * Notification id for primary notification channel "Send 2" notification
         */
        private const val NOTI_SECONDARY2 = 1201
    }
}
