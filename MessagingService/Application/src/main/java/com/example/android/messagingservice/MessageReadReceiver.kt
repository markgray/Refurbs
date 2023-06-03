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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/**
 * Broadcast receiver for "com.example.android.messagingservice.ACTION_MESSAGE_READ"
 */
class MessageReadReceiver : BroadcastReceiver() {
    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast for the
     * "com.example.android.messagingservice.ACTION_MESSAGE_READ" action `Intent`. First we
     * try to fetch the `int conversationId` which is stored in our `Intent intent`
     * under the key CONVERSATION_ID ("conversation_id") defaulting to -1. If there is such an
     * `int` we log the fact that the conversation was received, add it to our
     * `MessageLogger` preference file, and cancel the notification for the `conversationId`.
     * (This broadcast is created when the "Open app on phone" `Button` is pressed.)
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        val conversationId = intent.getIntExtra(CONVERSATION_ID, -1)
        if (conversationId != -1) {
            Log.d(TAG, "Conversation $conversationId was read")
            MessageLogger.logMessage(context, "Conversation $conversationId was read.")
            NotificationManagerCompat.from(context)
                .cancel(conversationId)
        }
    }

    companion object {
        /**
         * TAG for logging
         */
        private val TAG = MessageReadReceiver::class.java.simpleName

        /**
         * Key for `Intent` extra for an `int` conversation ID.
         */
        private const val CONVERSATION_ID = "conversation_id"
    }
}