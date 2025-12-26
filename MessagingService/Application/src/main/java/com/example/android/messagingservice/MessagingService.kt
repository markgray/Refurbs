@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "JoinDeclarationAndAssignment")

package com.example.android.messagingservice

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.android.messagingservice.MessagingService.Companion.CONVERSATION_ID
import com.example.android.messagingservice.MessagingService.Companion.EXTRA_REMOTE_REPLY
import com.example.android.messagingservice.MessagingService.Companion.MSG_SEND_NOTIFICATION
import com.example.android.messagingservice.MessagingService.Companion.READ_ACTION
import com.example.android.messagingservice.MessagingService.Companion.REPLY_ACTION
import com.example.android.messagingservice.R.string
import java.lang.ref.WeakReference

/**
 * [Service] defined in AndroidManifest.xml used to create and send remote notifications.
 *
 * It receives its instructions on what to do when its [IncomingHandler.handleMessage] override
 * is called with a [Message] whose [Message.what] field is [MSG_SEND_NOTIFICATION]. The number
 * of conversations to send and the number of messages in each conversation are encoded in the
 * [Message.arg1] and [Message.arg2] fields of the [Message]. The work is done in the method
 * [sendNotification] which calls [sendNotificationForConversation] for each conversation.
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
    private val mMessenger = Messenger( /* target = */ IncomingHandler( /* service = */this))

    /**
     * Called by the system when the service is first created. It is used to perform one-time
     * setup procedures. We just call our super's implementation of `onCreate` then initialize
     * our [NotificationManagerCompat] field [mNotificationManager] with an instance for our
     * application's package.
     */
    override fun onCreate() {
        super.onCreate()
        mNotificationManager = NotificationManagerCompat.from(applicationContext)
    }

    /**
     * Called by the system every time a client starts the service by calling `startService`.
     * We just log the fact that we were started. The system calls this method on the main thread
     * of the application. The Intent supplied to `startService(Intent)` is passed to this method.
     * May be called pending the delivery of the previous `onStartCommand()` call to this service,
     * so be careful when using global state in your implementation. Other applications that you
     * interact with that are associated with your overall application may begin starting up now.
     * For started services, there are two additional major modes of operation they can decide to
     * run in, controlled by the return value of this function. One is represented by
     * `START_STICKY` and the other is represented by `START_NOT_STICKY`.
     *
     * We just log the fact that we were called then return `START_STICKY` to the caller (tells the
     * system to create a fresh copy of the service, when sufficient memory is available, after it
     * is killed and that we want to continue running until we are explicitly stopped, so we should
     * receive a call to `onStartCommand` again with a `null` `Intent` object).
     *
     * @param intent The [Intent] supplied to `startService(Intent)`.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's
     * current started state. It may be one of the constants associated with the
     * `START_CONTINUATION_MASK` bits. We return `START_STICKY`.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY_COMPATIBILITY
    }

    /**
     * This is the ancient on-demand binding mechanism. It was simplified in HONEYCOMB to just
     * return a Binder object from onBind() that the client can use to directly access the service.
     * When binding to the service, we return an interface to our messenger for sending messages
     * to the service. We return the [IBinder] that our [Messenger] field [mMessenger] uses to
     * communicate with its [Handler].
     *
     * @param intent The [Intent] that was used to bind to this service, as given to
     * `bindService`. Note that any extras that were included with the Intent at that point will
     * not be seen here.
     * @return Return an [IBinder] through which clients can call on to the service.
     */
    override fun onBind(intent: Intent): IBinder = mMessenger.binder

    /**
     * Creates an [Intent] for reading a conversation. We create an [Intent] with the action
     * [READ_ACTION], add an extra to it under the key [CONVERSATION_ID] whose value is our
     * [Int] parameter [conversationId], and set its component to a new [ComponentName] for our
     * application context and the class [MessageReadReceiver]. Finally we return the [Intent]
     * to the caller.
     *
     * @param conversationId ID of the conversation to be read.
     * @return an [Intent] that can be used to launch our [MessageReadReceiver] broadcast receiver.
     */
    private fun getMessageReadIntent(conversationId: Int): Intent {
        return Intent()
            .setAction(READ_ACTION)
            .putExtra(CONVERSATION_ID, conversationId)
            .setComponent(ComponentName(applicationContext, MessageReadReceiver::class.java))
    }

    /**
     * Creates an [Intent] for replying to a conversation. We create an [Intent] with the action
     * [REPLY_ACTION], add an extra to it under the key [CONVERSATION_ID] whose value is our
     * [Int] parameter [conversationId], and set its component to a new [ComponentName] for our
     * application context and the class [MessageReplyReceiver]. Finally we return the [Intent]
     * to the caller.
     *
     * @param conversationId ID of the conversation to be replied to.
     * @return an [Intent] that can be used to launch our [MessageReplyReceiver] broadcast receiver.
     */
    private fun getMessageReplyIntent(conversationId: Int): Intent {
        return Intent()
            .setAction(REPLY_ACTION)
            .putExtra(CONVERSATION_ID, conversationId)
            .setComponent(ComponentName(applicationContext, MessageReplyReceiver::class.java))
    }

    /**
     * Sends a notification with a variable number of conversations and messages per conversation.
     * First we call the [Conversations.getUnreadConversations] method to generate the fake messages
     * we will be displaying and save the [Array] of [Conversation] objects it returns in our variable
     * `val conversations`. Then we loop through all the [Conversation] objects `conv` in `conversations`
     * calling our method [sendNotificationForConversation] for each of them.
     *
     * @param howManyConversations how many conversations to create and notify about
     * @param messagesPerConversation how many messages in each conversation
     */
    private fun sendNotification(howManyConversations: Int, messagesPerConversation: Int) {
        val conversations: Array<Conversation> =
            Conversations.getUnreadConversations(
                howManyConversations = howManyConversations,
                messagesPerConversation = messagesPerConversation
            )
        for (conv in conversations) {
            sendNotificationForConversation(conversation = conv)
        }
    }

    /**
     * This method builds a remote notification for its [Conversation] parameter [conversation]
     * and sends it. First we check to make sure we have permission to post notifications, throwing
     * [IllegalStateException] if we do not.
     *
     * Then we create a broadcast [Intent] to initialize [PendingIntent] variable
     * `val readPendingIntent` which will be dispatched to [MessageReadReceiver] when the
     * notification is read, adding the additional flag [PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT]
     * for SDK 34 or newer. We build a [RemoteInput] variable `val remoteInput` to receive voice
     * input from the remote device with the reply to be contained in the [Bundle] under the key
     * [EXTRA_REMOTE_REPLY] ("extra_remote_reply"), and the label on the remote device set to the
     * [String] with ID [string.reply] ("Reply"). We create a Broadcast intent to initialize
     * [PendingIntent] variable `val replyIntent` for the reply using the [Conversation.conversationId]
     * of [Conversation] parameter [conversation] as the requestCode, a message reply [Intent]
     * created by our method [getMessageReplyIntent], and the flag [PendingIntent.FLAG_UPDATE_CURRENT]
     * (Flag indicating that if the described [PendingIntent] already exists, then keep it but
     * replace its extra data with what is in this new [Intent]) adding the additional flag
     * [PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT] for SDK 34 or newer. We next build a Remote
     * Input enabled action which includes `R.drawable.notification_icon` as its icon, the String with
     * ID [string.reply] ("Reply") as its label, and our `replyIntent` then add `remoteInput`
     * as the input to be collected from the user when this action is sent. We create a [Person]
     * for the sender and one for the user. We create a [NotificationCompat.MessagingStyle] for
     * the current user, then iterate through all the messages in the [Conversation] parameter
     * [conversation], adding them to the `messagingStyle`. We then create a
     * [NotificationCompat.Builder], set its style to `messagingStyle`, small icon, large icon,
     * content intent, and reply action. Finally, we use the [NotificationManagerCompat] field
     * [mNotificationManager] to post the notification.
     *
     * @param conversation Class containing a conversation consisting of [Int] field
     * [Conversation.conversationId], [String] field [Conversation.participantName], one or more
     * [List] of [String] field [Conversation.messages] and a [Long] field [Conversation.timestamp].
     */
    private fun sendNotificationForConversation(conversation: Conversation) {
        if (ActivityCompat.checkSelfPermission(applicationContext, POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "We do not have permission to post Notifications")
            throw IllegalStateException("We do not have permission to post Notifications")
        }
        // A pending Intent for reads
        val readPendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent.getBroadcast(
                /* context = */ applicationContext,
                /* requestCode = */ conversation.conversationId,
                /* intent = */ getMessageReadIntent(conversation.conversationId),
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT
                    or PendingIntent.FLAG_MUTABLE
                    or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
            )
        } else {
            PendingIntent.getBroadcast(
                /* context = */ applicationContext,
                /* requestCode = */ conversation.conversationId,
                /* intent = */ getMessageReadIntent(conversation.conversationId),
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        // Build a RemoteInput for receiving voice input in a Car Notification or text input on
        // devices that support text input (like devices on Android N and above).
        val remoteInput = RemoteInput.Builder(EXTRA_REMOTE_REPLY)
            .setLabel(getString(string.reply))
            .build()

        // Building a Pending Intent for the reply action to trigger
        val replyIntent: PendingIntent = if (Build.VERSION.SDK_INT >= 34) {
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

        val sender = Person.Builder()
            .setName(conversation.participantName)
            .build()

        val user = Person.Builder()
            .setName("Me")
            .setKey("user")
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(user)

        for (message in conversation.messages) {
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    /* text = */ message,
                    /* timestamp = */ conversation.timestamp,
                    /* person = */ sender
                )
            )
        }

        val builder = NotificationCompat.Builder(applicationContext, "default")
            .setStyle(messagingStyle)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources, R.drawable.android_contact
                )
            )
            .setContentIntent(readPendingIntent)
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
            mReference = WeakReference( /* referent = */service)
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
        @Suppress("unused")
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
