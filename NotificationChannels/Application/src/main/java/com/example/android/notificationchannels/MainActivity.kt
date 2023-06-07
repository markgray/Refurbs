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
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi

/**
 * Display main screen for sample. Displays controls for sending test notifications.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class MainActivity : Activity() {
    /**
     * A view model for interacting with the UI elements. Its constructor sets the `OnClickListener`
     * of the buttons in our layout to "this", so it is actually used in spite of the lint warning.
     */
    private var ui: MainUi? = null

    /**
     * Our instance of `NotificationHelper`, we use it to call its `getNotification1`,
     * `getNotification2` and `notify` methods
     */
    private var noti: NotificationHelper? = null

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_main. We
     * initialize our field `NotificationHelper noti` with a new instance, and initialize our
     * field `MainUi ui` by finding the view with id R.id.activity_main and passing it to the
     * `MainUi` constructor (the constructor finds the buttons in this view group and sets their
     * `OnClickListener` to the `MainUi` instance being constructed).
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        noti = NotificationHelper(this)
        ui = MainUi(findViewById(R.id.activity_main))
    }

    /**
     * Send activity notifications. This is used by our `onClick` override to create and post
     * the notification requested when the user clicks one of the four "Send" buttons in our UI.
     * First we initialize `Notification.Builder nb` to null, then we switch on our parameter
     * `int id`:
     *
     *  *
     * NOTI_PRIMARY1 - we set `nb` to the `Notification.Builder` returned by the
     * `getNotification1` method of our field `NotificationHelper noti` using
     * our parameter `title` as the title of the notification, and the string with
     * resource id R.string.primary1_body ("The content") as the text body.
     *
     *  *
     * NOTI_PRIMARY2 - we set `nb` to the `Notification.Builder` returned by the
     * `getNotification1` method of our field `NotificationHelper noti` using
     * our parameter `title` as the title of the notification, and the string with
     * resource id R.string.primary2_body ("Second Notification for Primary Channel") as the
     * text body.
     *
     *  *
     * NOTI_SECONDARY1 - we set `nb` to the `Notification.Builder` returned by the
     * `getNotification2` method of our field `NotificationHelper noti` using
     * our parameter `title` as the title of the notification, and the string with
     * resource id R.string.secondary1_body ("Notification body text.") as the text body.
     *
     *  *
     * NOTI_SECONDARY2 - we set `nb` to the `Notification.Builder` returned by the
     * `getNotification2` method of our field `NotificationHelper noti` using
     * our parameter `title` as the title of the notification, and the string with
     * resource id R.string.secondary2_body ("Second Notification for Secondary Channel")
     * as the text body.
     *
     *
     * If `nb` is not now null, we call the `notify` method of `noti` with the
     * notification id `id`, and the notification builder `nb`.
     *
     * @param id    The ID of the notification to create
     * @param title The title of the notification
     */
    fun sendNotification(id: Int, title: String?) {
        val nb = when (id) {
            NOTI_PRIMARY1 -> noti!!.getNotification1(title, getString(R.string.primary1_body))
            NOTI_PRIMARY2 -> noti!!.getNotification1(title, getString(R.string.primary2_body))
            NOTI_SECONDARY1 -> noti!!.getNotification2(title, getString(R.string.secondary1_body))
            NOTI_SECONDARY2 -> noti!!.getNotification2(title, getString(R.string.secondary2_body))
            else -> null
        }
        if (nb != null) {
            noti!!.notify(id, nb)
        }
    }

    /**
     * Send Intent to load system Notification Settings for this app.
     */
    @TargetApi(Build.VERSION_CODES.O)
    fun goToNotificationSettings() {
        @SuppressLint("InlinedApi") val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        i.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(i)
    }

    /**
     * Send intent to load system Notification Settings UI for a particular channel. We initialize
     * `Intent i` with an intent containing the action ACTION_CHANNEL_NOTIFICATION_SETTINGS
     * (Show notification settings for a single `NotificationChannel`). We add an extra to
     * `i` with our package name stored under the key EXTRA_APP_PACKAGE (package owner of the
     * notification channel settings to display), and an extra containing our parameter `channel`
     * stored under the key EXTRA_CHANNEL_ID (the `getId` of the notification channel settings
     * to display). Finally we use `i` to start the settings activity.
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
    internal inner class MainUi constructor(root: View) : View.OnClickListener {
        /**
         * `EditText` with id R.id.main_primary_title, displays the text "Primary Channel" to
         * start with, we use its text as the title for the primary channel notifications sent.
         */
        val titlePrimary: TextView?

        /**
         * `EditText` with id R.id.main_secondary_title, displays the text "Secondary Channel"
         * to start with, we use its text as the title for the secondary channel notifications sent.
         */
        val titleSecondary: TextView

        /**
         * Our constructor. We initialize our field `TextView titlePrimary` by finding the view
         * in `root` with the id R.id.main_primary_title. We find the buttons with the ids
         * R.id.main_primary_send1 ("Send 1" in the primary channel section), R.id.main_primary_send2
         * ("Send 2" in the primary channel section), and R.id.main_primary_config (contains the system
         * settings icon) and set their `OnClickListener` to "this".
         *
         *
         * We initialize our field `TextView titleSecondary` by finding the view in `root`
         * with the id R.id.main_secondary_title. We find the buttons with the ids R.id.main_secondary_send1
         * ("Send 1" in the secondary channel section), R.id.main_secondary_send2 ("Send 2" in the
         * secondary channel section), and R.id.main_secondary_config (contains the system settings
         * icon) and set their `OnClickListener` to "this".
         *
         *
         * Finally we find the view in `root` with the id R.id.btnA ("Go to Settings") and set
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
         * If our field `TextView titlePrimary` is not null we return the string value of the
         * text it contains to the caller, otherwise we return the empty string "".
         *
         * @return the current text contents of our field `TextView titlePrimary`
         */
        private val titlePrimaryText: String
            get() = titlePrimary?.text?.toString() ?: ""

        /**
         * If our field `TextView titleSecondary` is not null we return the string value of the
         * text it contains to the caller, otherwise we return the empty string "".
         *
         * @return the current text contents of our field `TextView titleSecondary`
         */
        private val titleSecondaryText: String
            get() = if (titlePrimary != null) {
                titleSecondary.text.toString()
            } else ""

        /**
         * Called when a view whose `OnClickListener` we are has been clicked. We switch on the
         * id of the parameter `View view`:
         *
         *  *
         * R.id.main_primary_send1 ("Send 1" in primary channel section) - we call our method
         * `sendNotification` with the notification id NOTI_PRIMARY1, and the title
         * returned by our method `getTitlePrimaryText` and break.
         *
         *  *
         * R.id.main_primary_send2 ("Send 1" in primary channel section) - we call our method
         * `sendNotification` with the notification id NOTI_PRIMARY2, and the title
         * returned by our method `getTitlePrimaryText` and break.
         *
         *  *
         * R.id.main_primary_config (the setting wrench icon) - we call our method
         * `goToNotificationSettings` with the notification channel PRIMARY_CHANNEL
         * ("default") and break.
         *
         *  *
         * R.id.main_secondary_send1 ("Send 1" in secondary channel section) - we call our
         * method `sendNotification` with the notification id NOTI_SECONDARY1, and the
         * title returned by our method `getTitleSecondaryText` and break.
         *
         *  *
         * R.id.main_secondary_send2 ("Send 2" in secondary channel section) - we call our
         * method `sendNotification` with the notification id NOTI_SECONDARY2, and the
         * title returned by our method `getTitleSecondaryText` and break.
         *
         *  *
         * R.id.main_secondary_config (the setting wrench icon) - we call our method
         * `goToNotificationSettings` with the notification channel SECONDARY_CHANNEL
         * ("second") and break.
         *
         *  *
         * R.id.btnA ("Go to Settings") - we call the no parameter version of our method
         * `goToNotificationSettings` and break.
         *
         *  *
         * default - we log the message "Unknown click event." and break.
         *
         *
         *
         * @param view The view that was clicked.
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