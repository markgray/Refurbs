/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.messagingservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

/**
 * This sample shows a simple service that sends notifications using
 * `NotificationCompat`. It also extends the notification with Remote
 * Input to allow Android N devices to reply via text directly from
 * the notification without having to open an App. The same Remote
 * Input object also allows Android Auto users to respond by voice
 * when the notification is presented there.
 *
 * Note: Each unread conversation from a user is sent as a distinct notification.
 */
class MainActivity : FragmentActivity() {

    /**
     * Handle to the `NOTIFICATION_SERVICE` system level service, used to notify the user of events
     * that happen.
     */
    var mNM : NotificationManager? = null

    /**
     * Called when the activity is starting (or restarting after an orientation change).
     * First we call [enableEdgeToEdge] to enable edge to edge display, then we call our
     * super's implementation of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_main`.
     *
     * We initialize our [FrameLayout] variable `rootView` to the view with ID `R.id.container` then
     * call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying window
     * insets to `rootView`, with the `listener` argument a lambda that accepts the [View] passed
     * the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
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
     * TODO: Continue here.
     * We initialize our [NotificationManager] field [mNM] by using the [getSystemService]
     * method to get a handle to the system-level service with the name
     * [Context.NOTIFICATION_SERVICE] ("notification"). We initialize our [NotificationChannel]
     * variable `val chan1` to a new instance whose `id` is our [PRIMARY_CHANNEL] constant
     * ("default"), whose `name` is [PRIMARY_CHANNEL], and whose `importance` is
     * [NotificationManager.IMPORTANCE_DEFAULT]. We call the [NotificationChannel.setLightColor]
     * method of `chan1` (kotlin `lightColor` property) to set the notification light color for
     * notifications posted to the channel to [Color.GREEN], and call its
     * [NotificationChannel.setLockscreenVisibility] method (kotlin `lockscreenVisibility`
     * property) to set whether notifications posted to this channel appear on the lockscreen to
     * [Notification.VISIBILITY_PRIVATE] (Show this notification on all lockscreens, but conceal
     * sensitive or private information on secure lockscreens). We then call the method
     * [NotificationManager.createNotificationChannel] of [mNM] to create a notification channel
     * from `chan1`. Then if our [Bundle] parameter [savedInstanceState] is `null` (first time we
     * are running) we use the [FragmentManager] for interacting with fragments associated with
     * this activity to begin a [FragmentTransaction] which we use to add a new instance of our
     * fragment [MessagingFragment] to the container view with ID `R.id.container` and commit
     * that [FragmentTransaction].
     *
     * @param savedInstanceState we use it only to decide if we are running for the first time (it
     * is `null` then, otherwise it contains info that the [FragmentManager] is concerned with.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<FrameLayout>(R.id.container)
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
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val chan1 = NotificationChannel(
            /* id = */ PRIMARY_CHANNEL,
            /* name = */ PRIMARY_CHANNEL,
            /* importance = */ NotificationManager.IMPORTANCE_DEFAULT
        )
        chan1.lightColor = Color.GREEN
        chan1.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        mNM!!.createNotificationChannel(chan1)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, MessagingFragment(), "MessagingFragment")
                .commit()
        } else {
            Log.i(TAG, "we have been this way before")
        }
    }

    companion object {
        /**
         * The id of the primary notification channel
         */
        const val PRIMARY_CHANNEL: String = "default"

        /**
         * TAG used for logging
         */
        const val TAG: String = "MessagingService"
    }
}
