package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import java.time.Instant
import java.time.LocalDateTime

/**
 * Represents an IRC protocol message.
 */
class IrcMessage(val tags: Map<MessageTag, String>, val prefix: ByteArray?, val command: String, val params: List<ByteArray>) {

    /** The time at which the message was sent, or our best guess at it. */
    val time: LocalDateTime = if (MessageTag.ServerTime in tags) {
        LocalDateTime.ofInstant(Instant.parse(tags[MessageTag.ServerTime]), currentTimeZoneProvider())
    } else {
        currentTimeProvider()
    }

    /** The user that generated the message, if any. */
    val sourceUser by lazy {
        prefix?.asUser()?.apply {
            tags[MessageTag.AccountName]?.let { account = it }
        }
    }

}

/**
 * Supported tags that may be applied to messages.
 */
sealed class MessageTag(val name: String) {
    /** Specifies the account name of the user, if the `account-tag` capability is negotiated. */
    object AccountName : MessageTag("account")
    /** Specifies the time the server received the message, if the `server-time` capability is negotiated. */
    object ServerTime : MessageTag("time")
    /** A unique ID for the message, used to reply, react, edit, delete, etc. */
    object MessageId : MessageTag("draft/msgid")
    /** Used to identify a message ID that was replied to, to enable threaded conversations. */
    object Reply : MessageTag("+draft/reply")
    /** Used to specify a slack-like reaction to another message. */
    object React : MessageTag("+draft/react")
}

internal val messageTags: Map<String, MessageTag> by lazy {
    MessageTag::class.nestedClasses.map { it.objectInstance as MessageTag }.associateBy { it.name }
}
