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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNUSED_ANONYMOUS_PARAMETER",
    "UnusedImport"
)

package com.example.android.messagingservice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
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
import androidx.fragment.app.FragmentActivity

/**
 * The main fragment that shows the buttons and the text view containing the log.
 */
class MessagingFragment : Fragment(), View.OnClickListener {
    /**
     * [Button] in layout with ID `R.id.send_1_conversation` ("Send 1 conversation with 1 message")
     */
    private var mSendSingleConversation: Button? = null

    /**
     * [Button] in layout with ID `R.id.send_2_conversations` ("Send 2 conversations with 1 message")
     */
    private var mSendTwoConversations: Button? = null

    /**
     * [Button] in layout with ID `R.id.send_1_conversation_3_messages` ("Send 1 conversation with
     * 3 messages")
     */
    private var mSendConversationWithThreeMessages: Button? = null

    /**
     * [TextView] in layout with ID `R.id.data_port` used to display conversations
     */
    private var mDataPortView: TextView? = null

    /**
     * [Button] in layout with ID `R.id.clear` ("Clear Log")
     */
    private var mClearLogButton: Button? = null

    /**
     * Set in the [ServiceConnection.onServiceConnected] override in [ServiceConnection] field
     * [mConnection], it is used to send a `message` to [MessagingService] using [Messenger.send].
     * The [IBinder] parameter `service` used when constructing it in [mConnection] is generated
     * after we use the [FragmentActivity.bindService] method to connect [mConnection] to our
     * [MessagingService] in our [onStart] override. It is set back to `null` in its
     * [ServiceConnection.onServiceDisconnected] override.
     */
    private var mService: Messenger? = null

    /**
     * Flag indicating that we are bound to our service `MessagingService`, it is set to `true` in
     * the [ServiceConnection.onServiceConnected] override in [ServiceConnection] field [mConnection],
     * and cleared in its [ServiceConnection.onServiceDisconnected] override.
     */
    private var mBound = false

    /**
     * This is the [ServiceConnection] passed to the [FragmentActivity.bindService] method in our
     * [onStart] override when we connect to the [MessagingService] Service. It receives information
     * as the service is started and stopped (in particular, the [IBinder] `service` for the service
     * we are bound to, which it uses to create [Messenger] field [mService] which we will use to
     * `send` [Message]'s to the `Service`).
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        /**
         * Called when a connection to the Service has been established, with the [IBinder] of the
         * communication channel to the Service in its [service] parameter which we use to create
         * [Messenger] field [mService], we then set the [Boolean] field [mBound] flag to `true`,
         * and finally we call our method [setButtonsState] with `true` to enable the [Button]'s
         * in our layout.
         *
         * @param name    The concrete [ComponentName] of the service that has been connected.
         * @param service The [IBinder] of the Service's communication channel, which you can now
         * make calls on.
         */
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = Messenger(service)
            mBound = true
            setButtonsState(true)
        }

        /**
         * Called when a connection to the Service has been lost. This typically happens when the
         * process hosting the service has crashed or been killed. This does *not* remove the
         * [ServiceConnection] itself -- this binding to the service will remain active, and you
         * will receive a call to [ServiceConnection.onServiceConnected] when the Service is next
         * running.
         *
         * We set our [Messenger] field  [mService] to `null`, set our [Boolean] field [mBound]
         * flag to `false`, and call our method  [setButtonsState] with `false` to disable the
         * [Button]'s in our layout.
         *
         * @param name The concrete [ComponentName] of the service whose connection has been lost.
         */
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
            setButtonsState(false)
        }
    }

    /**
     * Whenever the contents our our [SharedPreferences] file changes, this
     * [OnSharedPreferenceChangeListener] will check whether the change was
     * to the [String] stored under the [MessageLogger.LOG_KEY] key ("message_data")
     * and if so will update the text in [TextView] field [mDataPortView] to it.
     */
    private val listener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (MessageLogger.LOG_KEY == key) {
            mDataPortView!!.text = MessageLogger.getAllMessages(activity as Context)
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view. First we use our [LayoutInflater]
     * parameter [inflater] to inflate our layout file `R.layout.fragment_message_me` into our [View]
     * variable `val rootView`. Then we locate the [Button] with ID `R.id.send_1_conversation` to
     * initialize our [Button] field [mSendSingleConversation] and set its [View.OnClickListener] to
     * "this", we locate the [Button] with ID `R.id.send_2_conversations` to initialize our [Button]
     * field [mSendTwoConversations] and set its [View.OnClickListener] to "this", and locate the
     * [Button] with ID `R.id.send_1_conversation_3_messages` to initialize our [Button] field
     * [mSendConversationWithThreeMessages] and set its [View.OnClickListener] to "this". We
     * initialize our [TextView] field [mDataPortView] by locating the [TextView] with ID
     * `R.id.data_port` and set its movement method to a new instance of [ScrollingMovementMethod]
     * (A movement method that interprets movement keys by scrolling the text buffer). We initialize
     * our [Button] field [mClearLogButton] by locating the [Button] with ID `R.id.clear` and set its
     * [View.OnClickListener] to `this`. We call our method [setButtonsState] with `false` to disable
     * the [Button]'s until after we connect to our [MessagingService]. Finally we return [View]
     * variable `rootView` to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the fragment
     * @param container If non-`null`, this is the parent view that the fragment's UI will be attached to.
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

    /**
     * This method checks whether our app has been granted [POST_NOTIFICATIONS]
     * ("android.permission.POST_NOTIFICATIONS") and if not it calls the
     * [ActivityResultLauncher.launch] method of [ActivityResultLauncher] field
     * [actionRequestPermission] to request the permission.
     */
    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            actionRequestPermission.launch(arrayOf(POST_NOTIFICATIONS))
            return
        }
    }

    /**
     * This [ActivityResultLauncher] calls the [registerForActivityResult] method to register a
     * request to start an activity for a result, designated by the contract
     * [ActivityResultContracts.RequestMultiplePermissions] which requests the permissions
     * passed it in the the [Array] of [String] passed to its [ActivityResultLauncher.launch]
     * method by our [requestNotificationPermission] method.
     */
    private val actionRequestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

        }

    /**
     * Called when a view whose [View.OnClickListener] has been set to `this` has been clicked.
     * We branch based on which [Button] has been clicked"
     *
     *  * [mSendSingleConversation] we send 1 conversation with 1 message
     *  * [mSendTwoConversations] we send 2 conversations with 1 message each
     *  * [mSendConversationWithThreeMessage[mSendConversationWithThreeMessages] we send 1
     *  conversation with 3 messages
     *  * [mClearLogButton] we clear our log from our shared preference file and set the
     *  text of our [TextView] field [mDataPortView] to the cleared value returned to us
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
     * Called when the [Fragment] is visible to the user. First we call through to our super's
     * implementation of `onStart`, then we connect to the application service [MessagingService],
     * creating it if needed. This defines a dependency between the application and the service.
     * The [ServiceConnection] field [mConnection] will receive the service object when it is
     * created and be told if it dies and restarts. The service will be considered required by the
     * system only for as long as the calling context exists. For example, if this [Context] is an
     * Activity that is stopped, the service will not be required to continue running until the
     * Activity is resumed. The [Context.BIND_AUTO_CREATE] flag will automatically create the
     * service as long as the binding exists.
     */
    override fun onStart() {
        super.onStart()
        requireActivity().bindService(
            Intent(activity, MessagingService::class.java),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Called when the [Fragment] is no longer resumed. First we call through to our super's
     * implementation of `onPause`, then we retrieve the shared preference file in order to
     * unregister our [SharedPreferences.OnSharedPreferenceChangeListener] field [listener].
     */
    override fun onPause() {
        super.onPause()
        MessageLogger.getPrefs(activity as Context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Called when the fragment is visible to the user and actively running. First we call through to
     * our super's implementation of `onResume`, then we retrieve the messages saved in our
     * shared preference file and set the text of [TextView] field [mDataPortView] to it.
     */
    override fun onResume() {
        super.onResume()
        mDataPortView!!.text = MessageLogger.getAllMessages(activity as Context)
        MessageLogger.getPrefs(activity as Context).registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Called when the [Fragment] is no longer started. First we call through to our super's
     * implementation of `onStop`, then if our [Boolean] field [mBound] it `true` we have bound to
     * our service [MessagingService] so we call the [FragmentActivity.unbindService] method to
     * disconnect [ServiceConnection] field [mConnection].
     */
    override fun onStop() {
        super.onStop()
        if (mBound) {
            requireActivity().unbindService(mConnection)
            mBound = false
        }
    }

    /**
     * Tells our [MessagingService] to send its dummy notifications to our devices (mobile and
     * paired smart watch, car etc.) If our [Boolean] field [mBound] is `true`, we are bound to
     * our service [MessagingService] so we create a [Message] variable `val msg` for a
     * [MessagingService.MSG_SEND_NOTIFICATION] action adding our [Int] parameter [howManyConversations]
     * as the `arg1` value and [Int] parameter [messagesPerConversation] as the `arg2` value. Then
     * wrapped in a try block intended to catch [RemoteException] we use the [Messenger.send] method
     * of our [Messenger] field [mService] to send `msg` to our [MessagingService] service.
     *
     * @param howManyConversations How many separate "conversations" to send notifications about
     * @param messagesPerConversation How many messages in each conversation
     */
    private fun sendMsg(howManyConversations: Int, messagesPerConversation: Int) {
        if (mBound) {
            val msg = Message.obtain(
                /* h = */ null,
                /* what = */ MessagingService.MSG_SEND_NOTIFICATION,
                /* arg1 = */ howManyConversations,
                /* arg2 = */ messagesPerConversation
            )
            try {
                mService!!.send(msg)
            } catch (e: RemoteException) {
                Log.e(TAG, "Error sending a message", e)
                MessageLogger.logMessage(activity as Context, "Error occurred while sending a message.")
            }
        }
    }

    /**
     * Convenience function to enable or disable the three [Button]'s in our UI:
     * [mSendSingleConversation], [mSendTwoConversations] and [mSendConversationWithThreeMessages]
     *
     * @param enable `true` to enable the [Button]'s `false` to disable
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
