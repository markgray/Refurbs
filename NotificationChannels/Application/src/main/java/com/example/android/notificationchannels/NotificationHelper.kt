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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.notificationchannels

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color

/**
 * Helper class to manage notification channels, and create notifications.
 * In the `init` block for our constructor we register two notification channels, which can be used
 * later by individual notifications. To do this we first create [NotificationChannel] variable
 * `val chan1` with the id [PRIMARY_CHANNEL] ("default"), the user visible name given by
 * `R.string.noti_channel_default` ("Primary Channel") and importance of the channel set to
 * `IMPORTANCE_DEFAULT` (shows everywhere, allowed to make noise, but does not visually intrude).
 * We set the notification light color for notifications posted to this channel to GREEN, and set
 * its lock screen visibility to VISIBILITY_PRIVATE (Show this notification on all lock-screens,
 * but conceal sensitive or private information on secure lock-screens). Then we instruct the
 * [NotificationManager] returned by our property [manager] to create notification channel `chan1`.
 *
 * Next we create [NotificationChannel] variable `val chan2` with the id [SECONDARY_CHANNEL]
 * ("second"), the user visible name given by `R.string.noti_channel_second` ("Secondary Channel")
 * and importance of the channel set to `IMPORTANCE_HIGH` (shows everywhere, allowed to make
 * noise and peek). We set the notification light color for notifications posted to this channel
 * to BLUE, and set its lock screen visibility to `VISIBILITY_PUBLIC` (Show this notification in
 * its entirety on all lock-screens). Finally we instruct the [NotificationManager] returned by
 * our property [manager] to create notification channel `chan2`.
 *
 * @param ctx the application [Context].
 */
internal class NotificationHelper(ctx: Context?) : ContextWrapper(ctx) {
    /**
     * Instance of [NotificationManager] we retrieve to access the system level service
     * [Context.NOTIFICATION_SERVICE].
     */
    private var manager: NotificationManager? = null
        /**
         * Get the notification manager. Utility method as this helper works with it a lot. If the
         * backing `field` of our [NotificationManager] field [manager] is `null` we initialize its
         * backing `field` with an instance of the system level service [Context.NOTIFICATION_SERVICE].
         * In either case we return the backing `field` of [manager] to the caller.
         */
        get() {
            if (field == null) {
                field = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            }
            return field
        }

    init {
        val chan1 = NotificationChannel(PRIMARY_CHANNEL,
            getString(R.string.noti_channel_default), NotificationManager.IMPORTANCE_DEFAULT)
        chan1.lightColor = Color.GREEN
        chan1.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        manager!!.createNotificationChannel(chan1)
        val chan2 = NotificationChannel(SECONDARY_CHANNEL,
            getString(R.string.noti_channel_second), NotificationManager.IMPORTANCE_HIGH)
        chan2.lightColor = Color.BLUE
        chan2.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager!!.createNotificationChannel(chan2)
    }

    /**
     * Get a [Notification.Builder] for primary notification channel. Provides a builder rather than
     * the notification itself so as to allow easier notification changes. First we create a
     * [Notification.Builder] for notification channel [PRIMARY_CHANNEL] ("default"), set its
     * title to our [String] parameter [title], its second line of text to our [String] parameter
     * [body], its small icon to the small icon for our app as returned by our property [smallIcon],
     * and make the notification be automatically dismissed when the user touches it (the
     * [PendingIntent] set with [Notification.Builder.setDeleteIntent] will be sent when this
     * happens). Finally we return this [Notification.Builder] to the caller.
     *
     * @param title the title of the notification
     * @param body the body text for the notification
     * @return the builder since it keeps a reference to the notification (since API 24)
     */
    fun getNotification1(title: String?, body: String?): Notification.Builder {
        return Notification.Builder(applicationContext, PRIMARY_CHANNEL)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(smallIcon)
            .setAutoCancel(true)
    }

    /**
     * Get a [Notification.Builder] for secondary notification channel. Provides a builder rather
     * than the notification itself so as to allow easier notification changes. First we create a
     * [Notification.Builder] for notification channel [SECONDARY_CHANNEL] ("second"), set its
     * title to our [String] parameter [title], its second line of text to our [String] parameter
     * [body], its small icon to the small icon for our app as returned by our property [smallIcon],
     * and make the notification be automatically dismissed when the user touches it (the
     * [PendingIntent] set with [Notification.Builder.setDeleteIntent] will be sent when this
     * happens). Finally we return this [Notification.Builder] to the caller.
     *
     * @param title Title for notification.
     * @param body Message for notification.
     * @return A [Notification.Builder] configured with the selected channel and details
     */
    fun getNotification2(title: String?, body: String?): Notification.Builder {
        return Notification.Builder(applicationContext, SECONDARY_CHANNEL)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(smallIcon)
            .setAutoCancel(true)
    }

    /**
     * Send a notification. We use our property [manager] to get our [NotificationManager]
     * and call its [NotificationManager.notify] method to post a notification built from our
     * [Notification.Builder] parameter [notification] and using our [Int] parameter [id] as
     * the identifier.
     *
     * @param id The identifier of the notification
     * @param notification The notification builder object
     */
    fun notify(id: Int, notification: Notification.Builder) {
        manager!!.notify(id, notification.build())
    }

    /**
     * Get the small icon for this app. We just return the system resource id
     * [android.R.drawable.stat_notify_chat] to the caller.
     *
     * @return The small icon resource id
     */
    @Suppress("SameReturnValue")
    private val smallIcon: Int
        get() = android.R.drawable.stat_notify_chat

    companion object {
        /**
         * The id of the primary notification channel
         */
        const val PRIMARY_CHANNEL = "default"

        /**
         * The id of the secondary notification channel
         */
        const val SECONDARY_CHANNEL = "second"
    }
}