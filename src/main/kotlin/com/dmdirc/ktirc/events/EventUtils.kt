package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.messages.sendTagMessage
import com.dmdirc.ktirc.model.MessageTag
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.asUser

internal fun ChannelNamesReceived.toModesAndUsers(client: IrcClient) = sequence {
    val modePrefixes = client.serverState.features[ServerFeature.ModePrefixes]!!
    for (user in names) {
        user.takeWhile { modePrefixes.isPrefix(it) }.let { prefix ->
            yield(modePrefixes.getModes(prefix) to user.substring(prefix.length).asUser())
        }
    }
}.toList()

/**
 * Replies in the appropriate place to a message received.
 *
 * Messages sent direct to the client will be responded to in direct message back; messages sent to a channel
 * will be replied to in the channel. If [prefixWithNickname] is `true`, channel messages will be prefixed
 * with the other user's nickname (separated from the message by a colon and space).
 *
 * If the given [message] has a [com.dmdirc.ktirc.model.MessageTag.MessageId] tag then the reply will include
 * the message ID to tell other IRCv3 capable clients what message is being replied to.
 */
fun IrcClient.reply(message: MessageReceived, response: String, prefixWithNickname: Boolean = false) {
    if (isToMe(message)) {
        sendMessage(message.user.nickname, response, message.metadata.messageId)
    } else {
        sendMessage(message.target, if (prefixWithNickname) "${message.user.nickname}: $response" else response, message.metadata.messageId)
    }
}

/**
 * "React" in the appropriate place to a message received.
 */
fun IrcClient.react(message: MessageReceived, reaction: String) = sendTagMessage(
        if (isToMe(message)) message.user.nickname else message.target,
        mapOf(MessageTag.React to reaction),
        message.metadata.messageId)

/**
 * Utility to determine whether the given message is to our local user or not.
 */
internal fun IrcClient.isToMe(message: MessageReceived) =
        caseMapping.areEquivalent(message.target, localUser.nickname)
