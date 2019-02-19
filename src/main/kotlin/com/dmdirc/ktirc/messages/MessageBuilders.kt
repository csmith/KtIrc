package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.model.MessageTag

/** Sends a message to ask the server to list capabilities. */
internal fun IrcClient.sendCapabilityList() = send("CAP", "LS", "302")

/** Sends a message indicating the end of capability negotiation. */
internal fun IrcClient.sendCapabilityEnd() = send("CAP", "END")

/** Sends a message requesting the specified caps are enabled. */
internal fun IrcClient.sendCapabilityRequest(capabilities: List<String>) = send("CAP", "REQ", capabilities.joinToString(" "))

/** Sends a request to join the given channel. */
fun IrcClient.sendJoin(channel: String) = send("JOIN", channel)

/** Sends a request to see the modes of a given target. */
fun IrcClient.sendModeRequest(target: String) = send("MODE", target)

/** Sends a request to change to the given nickname. */
fun IrcClient.sendNickChange(nick: String) = send("NICK", nick)

/** Sends the connection password to the server. */
internal fun IrcClient.sendPassword(password: String) = send("PASS", password)

/** Sends a response to a PING event. */
internal fun IrcClient.sendPong(nonce: ByteArray) = send("PONG", String(nonce))

/** Sends a CTCP message of the specified [type] and with optional [data] to [target] (a user or a channel). */
fun IrcClient.sendCtcp(target: String, type: String, data: String? = null) =
        sendMessage(target, "\u0001${type.toUpperCase()}${data?.let { " $it" } ?: ""}\u0001")

/** Sends an action to the given [target] (a user or a channel). */
fun IrcClient.sendAction(target: String, action: String) = sendCtcp(target, "ACTION", action)

/** Sends a private message to a user or channel. */
fun IrcClient.sendMessage(target: String, message: String, inReplyTo: String? = null) =
        send(
                inReplyTo?.let { tagMap(MessageTag.Reply to inReplyTo) } ?: emptyMap(),
                "PRIVMSG",
                target,
                message)

/**
 * Sends a tag-only message.
 *
 * If [inReplyTo] is specified then the [MessageTag.Reply] tag will be automatically added.
 */
fun IrcClient.sendTagMessage(target: String, tags: Map<MessageTag, String>, inReplyTo: String? = null) {
    send(inReplyTo?.let { tags + (MessageTag.Reply to inReplyTo) } ?: tags, "TAGMSG", target)
}

/** Sends a message to register a user with the server. */
internal fun IrcClient.sendUser(userName: String, realName: String) = send("USER", userName, "0", "*", realName)

/** Starts an authentication request. */
internal fun IrcClient.sendAuthenticationMessage(data: String = "+") = send("AUTHENTICATE", data)

/**
 * Utility method for creating a map of tags to avoid type inference problems.
 */
fun tagMap(vararg tags: Pair<MessageTag, String>) = mapOf(*tags)
