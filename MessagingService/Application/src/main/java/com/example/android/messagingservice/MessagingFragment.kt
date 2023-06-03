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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNUSED_ANONYMOUS_PARAMETER")

package com.example.android.messagingservice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * The main fragment that shows the buttons and the text view containing the log.
 */
class MessagingFragment
/**
 * Empty constructor needed for `Fragment`.
 */
    : Fragment(), View.OnClickListener {
    private var mSendSingleConversation // Button in layout with ID R.id.send_1_conversation
        : Button? = null
    private var mSendTwoConversations // Button in layout with ID R.id.send_2_conversations
        : Button? = null
    private var mSendConversationWithThreeMessages // Button in layout with ID R.id.send_1_conversation_3_messages
        : Button? = null
    private var mDataPortView // TextView in layout used to display conversations
        : TextView? = null
    private var mClearLogButton // Button in layout with ID R.id.clear
        : Button? = null

    /**
     * Set in our `onServiceConnected` override in `ServiceConnection mConnection`, it
     * is used to send a message to `MessagingService` using `Messenger.send(Message)`.
     * The `IBinder service` used when constructing it in `ServiceConnection mConnection`
     * is generated after we connect `mConnection` to our service `MessagingService` in
     * our override in `onStart`. It is set back to null in `onServiceDisconnected`.
     */
    private var mService: Messenger? = null

    /**
     * Flag indicating that we are bound to our service `MessagingService`, it is set in the
     * `onServiceConnected` override in `ServiceConnection mConnection`, and cleared in
     * `onServiceDisconnected`.
     */
    private var mBound = false

    /**
     * This is the `ServiceConnection` passed to `bindService` in `onStart` when
     * we connect to the `MessagingService` `Service`. It receives information as the
     * service is started and stopped (in particular, the `IBinder service` for the service
     * we are bound to, which it uses to create `Messenger mService` which we will use to
     * `send` `Message`'s to the `Service`).
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        /**
         * Called when a connection to the Service has been established, with the
         * [android.os.IBinder] of the communication channel to the Service
         * which we use to create `Messenger mService`, we then set the
         * `boolean mBound` flag to true, and finally we call our method
         * `setButtonsState(true)` to enable the `Button`'s in our
         * layout.
         *
         * @param name    The concrete component name of the service that has been connected.
         * @param service The IBinder of the Service's communication channel, which you can now
         * make calls on.
         */
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = Messenger(service)
            mBound = true
            setButtonsState(true)
        }

        /**
         * Called when a connection to the Service has been lost.  This typically
         * happens when the process hosting the service has crashed or been killed.
         * This does *not* remove the ServiceConnection itself -- this
         * binding to the service will remain active, and you will receive a call
         * to [.onServiceConnected] when the Service is next running.
         *
         *
         * We set our field `Messenger mService` to null, set our `boolean mBound`
         * flag to false, and call our method  `setButtonsState(false)` to disable the
         * `Button`'s in our layout.
         *
         * @param name The concrete component name of the service whose
         * connection has been lost.
         */
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
            setButtonsState(false)
        }
    }

    /**
     * Whenever the contents our our `SharedPreferences` file changes, this
     * `OnSharedPreferenceChangeListener` will check whether the change was
     * to the String stored under the `MessageLogger.LOG_KEY` key ("message_data")
     * and if so will update the text in `TextView mDataPortView`.
     */
    private val listener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (MessageLogger.LOG_KEY == key) {
            mDataPortView!!.text = MessageLogger.getAllMessages(activity as Context)
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view. First we inflate our layout
     * file R.layout.fragment_message_me into `View rootView`. Then we locate the `Button`
     * R.id.send_1_conversation to initialize our field `Button mSendSingleConversation` and set
     * its `OnClickListener` to "this", we locate the `Button` R.id.send_2_conversations
     * to initialize our field `Button mSendTwoConversations` and set its `OnClickListener`
     * to "this", and locate the `Button` R.id.send_1_conversation_3_messages to initialize our
     * field `Button mSendConversationWithThreeMessages` and set its `OnClickListener`
     * to "this". We initialize our field `TextView mDataPortView` by locating the `TextView`
     * R.id.data_port and set its movement method to `new ScrollingMovementMethod()` (A movement
     * method that interprets movement keys by scrolling the text buffer). We initialize our field
     * `Button mClearLogButton` by locating the `Button` R.id.clear and set its
     * `OnClickListener` to this. We call our method `setButtonsState(false)` to disable
     * the `Button`'s until after we connect to our `MessagingService`. Finally we return
     * `View rootView` to the caller.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any
     * views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     * UI should be attached to.
     * @param savedInstanceState We do not override onSaveInstanceState so do not use.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requestNotificationPermission()
        val rootView = inflater.inflate(R.layout.fragment_message_me, container, false)
        mSendSingleConversation = rootView.findViewById(R.id.send_1_conversation)
        mSendSingleConversation!!.setOnClickListener(this)
        mSendTwoConversations = rootView.findViewById(R.id.send_2_conversations)
        mSendTwoConversations!!.setOnClickListener(this)
        mSendConversationWithThreeMessages = rootView.findViewById(R.id.send_1_conversation_3_messages)
        mSendConversationWithThreeMessages!!.setOnClickListener(this)
        mDataPortView = rootView.findViewById(R.id.data_port)
        mDataPortView!!.movementMethod = ScrollingMovementMethod()
        mClearLogButton = rootView.findViewById(R.id.clear)
        mClearLogButton!!.setOnClickListener(this)
        setButtonsState(false)
        return rootView
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            actionRequestPermission.launch(arrayOf(POST_NOTIFICATIONS))
            return
        }
    }

    private val actionRequestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

        }

    /**
     * Called when a view whose `OnClickListener` has been set to "this" has been clicked.
     * We branch based on which `Button` has been clicked"
     *
     *  * `mSendSingleConversation` we send 1 conversation with 1 message
     *  * `mSendTwoConversations` we send 2 conversations with 1 message each
     *  * `mSendConversationWithThreeMessages` we send 1 conversation with 3 messages
     *  * `mClearLogButton` we clear our log from our shared preference file and set the
     * text of our `TextView mDataPortView` to the cleared value returned to us
     *
     *
     * @param view The view that was clicked.
     */
    override fun onClick(view: View) {
        if (view === mSendSingleConversation) {
            sendMsg(1, 1)
        } else if (view === mSendTwoConversations) {
            sendMsg(2, 1)
        } else if (view === mSendConversationWithThreeMessages) {
            sendMsg(1, 3)
        } else if (view === mClearLogButton) {
            MessageLogger.clear(activity as Context)
            mDataPortView!!.text = MessageLogger.getAllMessages(activity as Context)
        }
    }

    /**
     * Called when the Fragment is visible to the user. First we call through to our super's implementation
     * of `onStart`, then we connect to the application service `MessagingService`, creating
     * it if needed. This defines a dependency between the application and the service. The field
     * `ServiceConnection mConnection` will receive the service object when it is created and be
     * told if it dies and restarts. The service will be considered required by the system only for as
     * long as the calling context exists. For example, if this Context is an Activity that is stopped,
     * the service will not be required to continue running until the Activity is resumed. The BIND_AUTO_CREATE
     * flag will automatically create the service as long as the binding exists.
     */
    override fun onStart() {
        super.onStart()
        requireActivity().bindService(Intent(activity, MessagingService::class.java),
                mConnection,
                Context.BIND_AUTO_CREATE)
    }

    /**
     * Called when the Fragment is no longer resumed. First we call through to our super's implementation
     * of `onPause`, then we retrieve the shared preference file in order to unregister our
     * `SharedPreferences.OnSharedPreferenceChangeListener listener`.
     */
    override fun onPause() {
        super.onPause()
        MessageLogger.getPrefs(activity as Context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Called when the fragment is visible to the user and actively running. First we call through to
     * our super's implementation of `onResume`, then we retrieve the messages saved in our
     * shared preference file and set the text of `TextView mDataPortView` to it.
     */
    override fun onResume() {
        super.onResume()
        mDataPortView!!.text = MessageLogger.getAllMessages(activity as Context)
        MessageLogger.getPrefs(activity as Context).registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Called when the Fragment is no longer started. First we call through to our super's implementation
     * of `onStop`, then if we have bound to our service `MessagingService` we disconnect
     * `ServiceConnection mConnection`.
     */
    override fun onStop() {
        super.onStop()
        if (mBound) {
            requireActivity().unbindService(mConnection)
            mBound = false
        }
    }

    /**
     * Tells our `MessagingService` to send its dummy notifications to our devices (mobile and
     * paired smart watch, car etc.) If we are bound to our service `MessagingService` we create
     * a `Message msg` for a `MessagingService.MSG_SEND_NOTIFICATION` action adding the
     * `int`'s `howManyConversations` and `messagesPerConversation` to it. Then
     * wrapped in a try block intended to catch `RemoteException` we use our field
     * `Messenger mService` to send `msg` to our service `MessagingService`.
     *
     * @param howManyConversations How many separate "conversations" to send notifications about
     * @param messagesPerConversation How many messages in each conversation
     */
    private fun sendMsg(howManyConversations: Int, messagesPerConversation: Int) {
        if (mBound) {
            val msg = Message.obtain(null, MessagingService.MSG_SEND_NOTIFICATION,
                howManyConversations, messagesPerConversation)
            try {
                mService!!.send(msg)
            } catch (e: RemoteException) {
                Log.e(TAG, "Error sending a message", e)
                MessageLogger.logMessage(activity as Context, "Error occurred while sending a message.")
            }
        }
    }

    /**
     * Convenience function to enable or disable the three `Button`'s in our UI:
     * `mSendSingleConversation`, `mSendTwoConversations` and
     * `mSendConversationWithThreeMessages`
     *
     * @param enable true to enable the `Button`'s false to disable
     */
    private fun setButtonsState(enable: Boolean) {
        mSendSingleConversation!!.isEnabled = enable
        mSendTwoConversations!!.isEnabled = enable
        mSendConversationWithThreeMessages!!.isEnabled = enable
    }

    companion object {
        /**
         * TAG for logging
         */
        private val TAG = MessagingFragment::class.java.simpleName
        /**
         * [String] used to request the "POST_NOTIFICATIONS" permission.
         */
        const val POST_NOTIFICATIONS: String = "android.permission.POST_NOTIFICATIONS"

    }
}