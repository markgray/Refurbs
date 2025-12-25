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
@file:Suppress(
    "JoinDeclarationAndAssignment",
    "ReplaceNotNullAssertionWithElvisReturn"
)

package com.example.android.messagingservice

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper.getMainLooper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.android.messagingservice.MessagingFragment.Companion.POST_NOTIFICATIONS
import com.example.android.messagingservice.MessagingService.Companion.CONVERSATION_ID
import com.example.android.messagingservice.MessagingService.Companion.EXTRA_REMOTE_REPLY
import com.example.android.messagingservice.MessagingService.Companion.MSG_SEND_NOTIFICATION
import com.example.android.messagingservice.MessagingService.Companion.READ_ACTION
import com.example.android.messagingservice.MessagingService.Companion.REPLY_ACTION
import com.example.android.messagingservice.R.string
import java.lang.ref.WeakReference

/**
 * [Service] defined in AndroidManifest.xml used to create and send remote notifications.
 */
class MessagingService : Service() {
    /**
     * [NotificationManagerCompat] instance for the Application [Context]. Created in [onCreate].
     */
    private var mNotificationManager: NotificationManagerCompat? = null

    /**
     * [Messenger] pointing to our [IncomingHandler] custom [Handler] whose [IBinder] recieves and
     * forwards incoming messages from clients. Any Message objects sent through this [Messenger]
     * will appear in the [IncomingHandler] as if [Handler.sendMessage] had been called directly.
     */
    private val mMessenger = Messenger(IncomingHandler(this))

    /**
     * Called by the system when the service is first created. We just initialize our
     * [NotificationManagerCompat] field [mNotificationManager] with a new instance of
     * [NotificationManagerCompat] using  the [Context] of the single, global Application
     * object of the current process (This generally should only be used if you need a [Context]
     * whose lifecycle is separate from the current context, that is tied to the lifetime of
     * the process rather than the current component.)
     */
    override fun onCreate() {
        Log.d(TAG, "onCreate")
        mNotificationManager = NotificationManagerCompat.from(applicationContext)
    }

    /**
     * Return the communication channel to the service. We simply return the [IBinder] that our
     * [Messenger] field [mMessenger] is using to communicate with its associated [Handler] (which
     * is an instance of our [IncomingHandler] class in our case).
     *
     * @param intent The [Intent] that was used to bind to this service,
     * @return Return an [IBinder] through which clients can call on to the service.
     */
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind")
        return mMessenger.binder
    }

    /**
     * Creates an [Intent] that will be triggered when a message is marked as read. This method is a
     * convenience method to produce the [Intent] which will be received by the broadcast receiver
     * [MessageReadReceiver]. We simply return the result of creating a new instance of [Intent]
     * which we modify by chaining to it a call to [Intent.addFlags] to add the flag
     * [Intent.FLAG_INCLUDE_STOPPED_PACKAGES], followed by a chain call to [Intent.setAction]
     * to set the action to [READ_ACTION], and finishing up with a call to [Intent.putExtra] which
     * stores our [Int] parameter [id] as an extra under the key [CONVERSATION_ID].
     *
     * @param id Conversation ID of message in question
     * @return READ_ACTION [Intent] with our Conversation ID parameter [id] as an [Int] extra.
     */
    private fun getMessageReadIntent(id: Int): Intent {
        return Intent()
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            .setAction(READ_ACTION)
            .putExtra(CONVERSATION_ID, id)
    }

    /**
     * Creates an [Intent] that will be triggered when a voice reply is received. This method is a
     * convenience method to produce the [Intent] which will be received by the broadcast
     * receiver [MessageReplyReceiver]. We simply return the result of creating a new instance
     * of [Intent] which we modify by chaining to it a call to [Intent.addFlags] for the flag
     * [Intent.FLAG_INCLUDE_STOPPED_PACKAGES], followed by a chained call to [Intent.setAction] to
     * set the action to [REPLY_ACTION], and finishing up with a chained call to [Intent.putExtra]
     * which stores our [Int] parameter [conversationId] as an extra under the key [CONVERSATION_ID].
     *
     * @param conversationId Conversation ID of message in question
     * @return [REPLY_ACTION] action [Intent] with our parameter [conversationId] as an [Int] extra.
     */
    private fun getMessageReplyIntent(conversationId: Int): Intent {
        return Intent()
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            .setAction(REPLY_ACTION)
            .putExtra(CONVERSATION_ID, conversationId)
    }

    /**
     * Creates and sends notifications for requested number of conversations and messages. First we
     * use the method [Conversations.getUnreadConversations] to create an array of the number
     * of conversations and messages requested to initialize [Array] of [Conversation] variable
     * `val conversations`. Then for each [Conversation] variable `var conv` in that [Array] we call
     * our method [sendNotificationForConversation].
     *
     * @param howManyConversations    how many conversations to create and notify about
     * @param messagesPerConversation how many messages in each conversation
     */
    private fun sendNotification(howManyConversations: Int, messagesPerConversation: Int) {
        val conversations: Array<Conversation> = Conversations.getUnreadConversations(
            howManyConversations, messagesPerConversation
        )
        for (conv in conversations) {
            sendNotificationForConversation(conversation = conv)
        }
    }

    /**
     * This method builds a remote notification for its [Conversation] parameter [conversation]
     * and sends it. First we check to make sure we have permission to post notifications.
     * When we create a broadcast [Intent] to initialize [PendingIntent] variable
     * `val readPendingIntent` which will be dispatched to [MessageReadReceiver] when the
     * notification is read. We build a [RemoteInput] variable `val remoteInput` to receive voice
     * input from the remote device with the reply to be contained in the [Bundle] under the key
     * [EXTRA_REMOTE_REPLY] ("extra_remote_reply"), and the label on the remote device set to the
     * [String] with ID [string.reply] ("Reply"). We create a Broadcast intent to initialize
     * [PendingIntent] variable `val replyIntent` for the reply using the [Conversation.conversationId]
     * of [Conversation] parameter [conversation] as the requestCode, a message reply [Intent]
     * created by our method [getMessageReplyIntent], and the flag [PendingIntent.FLAG_UPDATE_CURRENT]
     * (Flag indicating that if the described [PendingIntent] already exists, then keep it but
     * replace its extra data with what is in this new [Intent].) We next build a Remote Input
     * enabled action which includes `R.drawable.notification_icon` as its icon, the String with
     * ID [string.reply] ("Reply") as its label, and our `replyIntent` then add `remoteInput`
     * as the input to be collected from the user when this action is sent. We next create an
     * [NotificationCompat.CarExtender.UnreadConversation.Builder] to initiaizle variable
     * `val unreadConvBuilder`, set the timestamp of the most recent message to the timestamp of our
     * [Conversation] parameter [conversation], set its Read Pending Intent to `readPendingIntent`
     * and its Reply Action to the pending intent and remote input which will convey the reply to
     * this notification: `replyIntent`, and `remoteInput`. We create [StringBuilder] variable
     * `val messageForNotification`, then proceed to iterate over all of the messages in
     * [Conversation] parameter [conversation] adding them to the Builder `unreadConvBuilder` and
     * appending them to `messageForNotification`. Next we create [NotificationCompat.Builder]
     * variable `val builder`, set its small icon to `R.drawable.notification_icon`, set its large
     * icon to `R.drawable.android_contact`, set its content text to the [String] returned by the
     * [toString] method of [StringBuilder] variable `messageForNotification`, set the time the
     * event occurred to the [Conversation.timestamp] field of [conversation], set its content title
     * to the [Conversation.participantName] of the [conversation], set its content [Intent] to
     * `readPendingIntent`, extend it with a [NotificationCompat.CarExtender] whose unread
     * conversation is set to `unreadConvBuilder.build`, whose color is set to `R.color.default_color_light`,
     * and lastly add the action `actionReplyByRemoteInput` to [NotificationCompat.Builder] `builder`.
     * We log that we are sending the notification, then use [NotificationManagerCompat] field
     * [mNotificationManager] to post the notification using the [Conversation.conversationId] of
     * the [conversation] as the notification ID and `builder.build()` as the `Notification`.
     *
     * @param conversation Class containing a conversation consisting of [Int] field
     * [Conversation.conversationId], [String] field [Conversation.participantName], one or more
     * [List] of [String] field [Conversation.messages] and a [Long] field [Conversation.timestamp].
     */
    private fun sendNotificationForConversation(conversation: Conversation) {
        if (ContextCompat.checkSelfPermission(applicationContext, POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "We do not have permission to post Notifications")
            throw IllegalStateException("We do not have permission to post Notifications")
        }
        // A pending Intent for reads
        val readPendingIntent: PendingIntent
        readPendingIntent = if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent.getBroadcast(
                applicationContext,
                conversation.conversationId,
                getMessageReadIntent(conversation.conversationId),
                PendingIntent.FLAG_UPDATE_CURRENT
                    or PendingIntent.FLAG_MUTABLE
                    or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
            )
        } else {
            PendingIntent.getBroadcast(
                applicationContext,
                conversation.conversationId,
                getMessageReadIntent(conversation.conversationId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        // Build a RemoteInput for receiving voice input in a Car Notification or text input on
        // devices that support text input (like devices on Android N and above).
        val remoteInput = RemoteInput.Builder(EXTRA_REMOTE_REPLY)
            .setLabel(getString(string.reply))
            .build()

        // Building a Pending Intent for the reply action to trigger
        val replyIntent = if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent.getBroadcast(
                /* context = */ applicationContext,
                /* requestCode = */ conversation.conversationId,
                /* intent = */ getMessageReplyIntent(conversation.conversationId),
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT
                    or PendingIntent.FLAG_MUTABLE
                    or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
            )
        } else {
            PendingIntent.getBroadcast(
                /* context = */ applicationContext,
                /* requestCode = */ conversation.conversationId,
                /* intent = */ getMessageReplyIntent(conversation.conversationId),
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        // Build an Android N compatible Remote Input enabled action.
        val actionReplyByRemoteInput = NotificationCompat.Action.Builder(
            R.drawable.notification_icon, getString(string.reply), replyIntent
        ).addRemoteInput(remoteInput).build()

        // Create the UnreadConversation and populate it with the participant name,
        // read and reply intents.
        @Suppress("DEPRECATION") // TODO: Use NotificationCompat.MessagingStyle
        val unreadConvBuilder =
            NotificationCompat.CarExtender.UnreadConversation.Builder(conversation.participantName)
                .setLatestTimestamp(conversation.timestamp)
                .setReadPendingIntent(readPendingIntent)
                .setReplyAction(replyIntent, remoteInput)

        // Note: Add messages from oldest to newest to the UnreadConversation.Builder
        val messageForNotification = StringBuilder()
        val messages: Iterator<String> = conversation.messages.iterator()
        while (messages.hasNext()) {
            val message = messages.next()
            unreadConvBuilder.addMessage(message)
            messageForNotification.append(message)
            if (messages.hasNext()) {
                messageForNotification.append(EOL)
            }
        }
        @Suppress("DEPRECATION") // TODO: Use NotificationCompat.MessagingStyle
        val builder = NotificationCompat.Builder(applicationContext, "default")
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources, R.drawable.android_contact
                )
            )
            .setContentText(messageForNotification.toString())
            .setWhen(conversation.timestamp)
            .setContentTitle(conversation.participantName)
            .setContentIntent(readPendingIntent)
            .extend(
                NotificationCompat.CarExtender()
                    .setUnreadConversation(unreadConvBuilder.build())
                    .setColor(applicationContext.getColor(R.color.default_color_light))
            )
            .addAction(actionReplyByRemoteInput)
        MessageLogger.logMessage(
            applicationContext, "Sending notification "
                + conversation.conversationId + " conversation: " + conversation
        )
        mNotificationManager!!.notify(conversation.conversationId, builder.build())
    }

    /**
     * Constructor for our [Handler] for incoming messages from clients.
     * It just initializes the [WeakReference] to [MessagingService] field [mReference]
     * with its [MessagingService] parameter `service` in its `init` block.
     *
     * @param service The instance of [MessagingService] we will be Handling (called using
     * `this` in the initialization of its [Messenger] field [mMessenger]
     */
    private class IncomingHandler(service: MessagingService) : Handler(getMainLooper()) {
        /**
         * Weak reference to our [MessagingService] class
         */
        private val mReference: WeakReference<MessagingService>

        init {
            mReference = WeakReference(/* referent = */ service)
        }

        /**
         * Handle messages here. These are messages sent from [MessagingFragment]'s method
         * [MessagingFragment.sendMsg]. First we retrieve our [MessagingService] reference from the
         * [WeakReference] of [MessagingService] field [mReference] into [MessagingService] variable
         * `val service`. Then if the [Message.what] message code of our [Message] parameter [msg]
         * is not [MSG_SEND_NOTIFICATION] we pass the call to our super's implementation of
         * `handleMessage`, if it is a [MSG_SEND_NOTIFICATION] message we use the [Message.arg1]
         * property of [msg] to initialize [Int] variable `val howManyConversations` (defaulting to
         * 1 if it is 0 or less), and use the [Message.arg2] property to initialize [Int] variable
         * `val messagesPerConversation` (defaulting to 1 if it is 0 or less). If `service` is not
         * `null` ([WeakReference.get] returns `null` if the [WeakReference] has been garbage
         * collected), we use `service` to send notifications as instructed by the arguments in our
         * [Message] parameter [msg].
         *
         * @param msg A [Message] object
         */
        override fun handleMessage(msg: Message) {
            val service: MessagingService? = mReference.get()
            if (msg.what == MSG_SEND_NOTIFICATION) {
                val howManyConversations: Int = if (msg.arg1 <= 0) 1 else msg.arg1
                val messagesPerConversation: Int = if (msg.arg2 <= 0) 1 else msg.arg2
                service?.sendNotification(howManyConversations, messagesPerConversation)
            } else {
                super.handleMessage(msg)
            }
        }
    }

    companion object {
        /**
         * TAG for logging
         */
        private val TAG = MessagingService::class.java.simpleName

        /**
         * Constant for end of line character
         */
        private const val EOL = "\n"

        /**
         * [Intent] action for remote reply that message has been read
         */
        private const val READ_ACTION = "com.example.android.messagingservice.ACTION_MESSAGE_READ"

        /**
         * [Intent] action for [Intent] that will be triggered when a voice reply is received.
         */
        const val REPLY_ACTION: String = "com.example.android.messagingservice.ACTION_MESSAGE_REPLY"

        /**
         * Key for [Intent] extra containing conversation ID.
         */
        const val CONVERSATION_ID: String = "conversation_id"

        /**
         * The [Bundle] key that refers to input collected from the user as a reply.
         */
        const val EXTRA_REMOTE_REPLY: String = "extra_remote_reply"

        /**
         * Used for [Message.what] message code to define what service is to be performed.
         */
        const val MSG_SEND_NOTIFICATION: Int = 1
    }
}
