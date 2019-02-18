package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import java.time.Instant
import java.time.LocalDateTime

/**
 * Represents an IRC protocol message.
 */
internal class IrcMessage(val tags: Map<MessageTag, String>, val prefix: ByteArray?, val command: String, val params: List<ByteArray>) {

    /** The time at which the message was sent, or our best guess at it. */
    val metadata = EventMetadata(
            time = time,
            batchId = tags[MessageTag.Batch],
            messageId = tags[MessageTag.MessageId],
            label = tags[MessageTag.Label])

    /** The user that generated the message, if any. */
    val sourceUser by lazy {
        prefix?.asUser()?.apply {
            tags[MessageTag.AccountName]?.let { account = it }
        }
    }

    private val time
        get() = when (MessageTag.ServerTime in tags) {
            true -> LocalDateTime.ofInstant(Instant.parse(tags[MessageTag.ServerTime]), currentTimeZoneProvider())
            false -> currentTimeProvider()
        }

}

/**
 * Supported tags that may be applied to messages.
 */
@Suppress("unused")
sealed class MessageTag(val name: String) {
    /** Specifies the account name of the user, if the `account-tag` capability is negotiated. */
    object AccountName : MessageTag("account")

    /** Specifies the ID that a batch message belongs to. */
    object Batch : MessageTag("batch")

    /** An arbitrary label to identify the response to messages we generate. */
    object Label : MessageTag("draft/label")

    /** A unique ID for the message, used to reply, react, edit, delete, etc. */
    object MessageId : MessageTag("draft/msgid")

    /** Used to identify a message ID that was replied to, to enable threaded conversations. */
    object Reply : MessageTag("+draft/reply")

    /** Used to specify a slack-like reaction to another message. */
    object React : MessageTag("+draft/react")

    /** Specifies the time the server received the message, if the `server-time` capability is negotiated. */
    object ServerTime : MessageTag("time")
}

internal val messageTags: Map<String, MessageTag> by lazy {
    MessageTag::class.nestedClasses.map { it.objectInstance as MessageTag }.associateBy { it.name }
}
