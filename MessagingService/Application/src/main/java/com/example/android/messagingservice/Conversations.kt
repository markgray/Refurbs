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
     * Returns an [Array] of [Conversation] containing [Int] parameter [howManyConversations], with
     * each [Conversation] in the array containing a [List] of [String] field `messages` with [Int]
     * parameter [messagesPerConversation] members in it. First we create [MutableList] of
     * [Conversation] variable `val conversationsList` using [arrayListOf], then `repeat` [Int]
     * parameter [howManyConversations] times creating a [Conversation] with a random
     * [Conversation.conversationId], a random [Conversation.participantName] and [Int] parameter
     * [messagesPerConversation] random messages for its [Conversation.messages] field. We then
     * return the [Array] of [Conversation] created by the [toTypedArray] extension function from
     * `conversationsList` to the caller.
     *
     * @param howManyConversations how many separate conversations to create
     * @param messagesPerConversation how many messages in each conversation
     * @return an [Array] of one or more [Conversation]'s
     */
    fun getUnreadConversations(
        howManyConversations: Int,
        messagesPerConversation: Int
    ): Array<Conversation> {
        val conversationsList: MutableList<Conversation> = arrayListOf()
        repeat (times = howManyConversations) {
            conversationsList += Conversation(
                conversationId = ThreadLocalRandom.current().nextInt(),
                participantName = name(),
                messages = makeMessages(messagesPerConversation)
            )
        }
        return conversationsList.toTypedArray()
    }

    /**
     * Constructs and returns a [List] of [String] with [Int] parameter [messagesPerConversation]
     * random messages chosen from our [Array] of [String] field [MESSAGES].
     *
     * @param messagesPerConversation number of messages to create in the [List] of [String] we return.
     * @return a [List] of [String]` with [messagesPerConversation] random messages in it.
     */
    fun makeMessages(messagesPerConversation: Int): List<String> {
        val maxLen = MESSAGES.size
        val messages: MutableList<String> = ArrayList(messagesPerConversation)
        repeat (times = messagesPerConversation) {
            messages.add(MESSAGES[ThreadLocalRandom.current().nextInt(0, maxLen)])
        }
        return messages
    }

    /**
     * Returns a random name from the [Array] of [String] field [PARTICIPANTS] to use as the
     * [String] field  [Conversation.participantName] of a [Conversation].
     *
     * @return a random name chosen from the `String[] PARTICIPANTS` array.
     */
    fun name(): String {
        return PARTICIPANTS[ThreadLocalRandom.current().nextInt(0, PARTICIPANTS.size)]
    }
}