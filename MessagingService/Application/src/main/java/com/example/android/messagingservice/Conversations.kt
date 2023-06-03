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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.messagingservice

import java.util.concurrent.ThreadLocalRandom

/**
 * A simple class that denotes unread conversations and messages. In a real world application,
 * this would be replaced by a content provider that actually gets the unread messages to be
 * shown to the user.
 */
object Conversations {
    /**
     * Set of strings used as messages by the sample.
     */
    val MESSAGES: Array<String> = arrayOf(
        "Are you at home?",
        "Can you give me a call?",
        "Hey yt?",
        "Don't forget to get some milk on your way back home",
        "Is that project done?",
        "Did you finish the Messaging app yet?"
    )

    /**
     * Senders of the said messages.
     */
    val PARTICIPANTS: Array<String> = arrayOf(
        "John Smith",
        "Robert Lawrence",
        "James Smith",
        "Jane Doe"
    )

    /**
     * Returns an `Conversation[]` array containing `howManyConversations`, with each
     * `Conversation` in the array containing an array of `List<String> messages` with
     * `messagesPerConversation` members in it. First we create `Conversation[] conversations`
     * intended to hold `howManyConversations`, then for each of these we create a `Conversation`
     * with a random `conversationId`, a random `participantName` and `messagesPerConversation`
     * random messages. We then return `Conversation[] conversations` to the caller.
     *
     * @param howManyConversations how many separate conversations to create
     * @param messagesPerConversation how many messages in each conversation
     * @return an array of one or more conversations
     */
    fun getUnreadConversations(howManyConversations: Int, messagesPerConversation: Int): Array<Conversation> {
        val conversationsList: MutableList<Conversation> = arrayListOf()
        for (i in 0 until howManyConversations) {
            conversationsList += Conversation(
                ThreadLocalRandom.current().nextInt(),
                name(), makeMessages(messagesPerConversation))
        }
        return conversationsList.toTypedArray()
    }

    /**
     * Constructs and returns a `List<String>` with `messagesPerConversation` random
     * messages chosen from our `String[] MESSAGES` array.
     *
     * @param messagesPerConversation number messages to create in the `List<String>` we return.
     * @return a `List<String>` with `messagesPerConversation` random messages in it.
     */
    fun makeMessages(messagesPerConversation: Int): List<String> {
        val maxLen = MESSAGES.size
        val messages: MutableList<String> = ArrayList(messagesPerConversation)
        for (i in 0 until messagesPerConversation) {
            messages.add(MESSAGES[ThreadLocalRandom.current().nextInt(0, maxLen)])
        }
        return messages
    }

    /**
     * Returns a random name from the `String[] PARTICIPANTS` array to use as the
     * `String participantName` of a `Conversation`.
     *
     * @return a random name chosen from the `String[] PARTICIPANTS` array.
     */
    fun name(): String {
        return PARTICIPANTS[ThreadLocalRandom.current().nextInt(0, PARTICIPANTS.size)]
    }
}