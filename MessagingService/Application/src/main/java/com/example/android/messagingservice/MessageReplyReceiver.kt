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
package com.example.android.messagingservice

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.android.messagingservice.MessagingFragment.Companion.POST_NOTIFICATIONS

/**
 * A Broadcast receiver for "com.example.android.messagingservice.ACTION_MESSAGE_REPLY" that gets
 * called when a reply is sent to a given conversationId.
 */
class MessageReplyReceiver : BroadcastReceiver() {
    /**
     * TODO: Figure out if this is ever getting called and if not why not.
     * This method is called when the [BroadcastReceiver] is receiving an [Intent] broadcast for the
     * "com.example.android.messagingservice.ACTION_MESSAGE_REPLY" action [Intent]. First we
     * check to see if the action of the Intent we are receiving is [MessagingService.REPLY_ACTION]
     * ("com.example.android.messagingservice.ACTION_MESSAGE_REPLY") and ignore it if it is not.
     * If it is, we fetch the [Int] variable `val conversationId` stored in our [Intent] parameter
     * [intent] under the key [MessagingService.CONVERSATION_ID] (defaulting to -1), then we call
     * our method [getMessageText] to extract the [CharSequence] variable `val reply` stored in the
     * [Intent] parameter [intent] in a [Bundle] under the key [MessagingService.EXTRA_REMOTE_REPLY].
     * If our [intent] parameter contained a `conversationId` we log the reply, and add it to our
     * [MessageLogger] preference file.
     *
     * Finally we update the notification to stop the progress spinner. This involves getting
     * a [NotificationManagerCompat] variable `val notificationManager` instance from our [Context]
     * parameter [context], building a [Notification] variable `val repliedNotification` using a
     * [NotificationCompat.Builder] for our [Context] parameter [context] whose `channelId` is the
     * "default" channel ID, with the drawable with ID `R.drawable.notification_icon` as its small
     * icon, and with the drawable with ID `R.drawable.android_contact` as its large icon, and the
     * [String] "Replied" as the text. We then use the `notificationManager` to post the
     * `repliedNotification` using the same `conversationId` (This has the effect of replacing
     * the notification that was replied to with our new "Replied" notification.)
     *
     * @param context The [Context] in which the receiver is running.
     * @param intent  The [Intent] being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        MessageLogger.logMessage(context, "Message reply received Intent")
        if (MessagingService.REPLY_ACTION == intent.action) {
            val conversationId: Int = intent.getIntExtra(
                /* name = */ MessagingService.CONVERSATION_ID,
                /* defaultValue = */ -1
            )
            val reply: CharSequence? = getMessageText(intent = intent)
            if (conversationId != -1) {
                Log.d(TAG, "Got reply ($reply) for ConversationId $conversationId")
                MessageLogger.logMessage(
                    context, "ConversationId: " + conversationId +
                        " received a reply: [" + reply + "]"
                )

                if (ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "We do not have permission to post Notifications")
                    throw IllegalStateException("We do not have permission to post Notifications")
                }
                // Update the notification to stop the progress spinner.
                val notificationManager = NotificationManagerCompat.from(context)
                val repliedNotification = NotificationCompat.Builder(context, "default")
                    .setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(
                        BitmapFactory.decodeResource(
                            /* res = */ context.resources,
                            /* id = */ R.drawable.android_contact
                        )
                    ).setContentText(context.getString(R.string.replied))
                    .build()
                notificationManager.notify(conversationId, repliedNotification)
            }
        }
    }

    /**
     * Get the message text from the [Intent] parameter [intent]. First we get the remote input
     * results [Bundle] from our [Intent] parameter [intent] which was received by the [onReceive]
     * callback of our [MessageReplyReceiver]. The returned [Bundle] contains a key/value for every
     * result key populated by the remote input collector. Then we use the [Bundle.getCharSequence]
     * method to retrieve the value stored under the key [MessagingService.EXTRA_REMOTE_REPLY]
     * ("extra_remote_reply") and return that [CharSequence] to the caller.
     *
     * @param intent this is the [Intent] our [BroadcastReceiver] received.
     * @return The [CharSequence] stored in the [Intent]'s results [Bundle] under the key
     * [MessagingService.EXTRA_REMOTE_REPLY] ("extra_remote_reply") or `null`.
     */
    private fun getMessageText(intent: Intent): CharSequence? {
        val remoteInput: Bundle? = RemoteInput.getResultsFromIntent(intent)
        return remoteInput?.getCharSequence(MessagingService.EXTRA_REMOTE_REPLY)
    }

    companion object {
        /**
         * TAG for logging
         */
        private val TAG: String = MessageReplyReceiver::class.java.simpleName
    }
}
