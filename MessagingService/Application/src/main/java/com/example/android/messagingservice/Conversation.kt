package com.example.android.messagingservice

/**
 * Class containing a conversation consisting of [Int] field [conversationId], [String] field
 * [participantName], one or more [List] of [String] field [messages] and a [Long] field [timestamp].
 *
 * Our `init` blocks initializes fields of this instance with parameters passed to our constructor.
 *
 * @param conversationId value to assign to `this.conversationId`
 * @param participantName value to assign to `this.participantName`
 * @param messages value to assign to `this.messages`
 */
class Conversation(
    /**
     * Random number that is just included in `toString` from what I see
     */
    val conversationId: Int,
    /**
     * Random name from PARTICIPANTS array
     */
    val participantName: String,
    messages: List<String>
) {
    /**
     * A given conversation can have a single or multiple messages.
     * Note that the messages are sorted from *newest* to *oldest*
     */
    @Suppress("JoinDeclarationAndAssignment") // It is better this way
    val messages: List<String>

    /**
     * Time from `System.currentTimeMillis()` when `Conversation` is constructed
     */
    val timestamp: Long

    init {
        this.messages = messages
        timestamp = System.currentTimeMillis()
    }

    /**
     * Returns a [String] representation of the [Conversation] instance suitable for logging.
     *
     * @return Loggable [String] representation of this [Conversation] instance.
     */
    override fun toString(): String {
        return "[Conversation: conversationId=" + conversationId +
            ", participantName=" + participantName +
            ", messages=" + messages +
            ", timestamp=" + timestamp + "]"
    }
}