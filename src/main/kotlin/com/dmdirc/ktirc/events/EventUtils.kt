package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.sendMessage
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
 */
fun IrcClient.reply(message: MessageReceived, response: String, prefixWithNickname: Boolean = false) {
    if (caseMapping.areEquivalent(message.target, serverState.localNickname)) {
        sendMessage(message.user.nickname, response)
    } else {
        sendMessage(message.target, if (prefixWithNickname) "${message.user.nickname}: $response" else response)
    }
}