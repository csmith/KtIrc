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

fun IrcClient.reply(message: MessageReceived, response: String, prefixWithNickname: Boolean = false) {
    if (caseMapping.areEquivalent(message.target, serverState.localNickname)) {
        sendMessage(message.user.nickname, response)
    } else {
        sendMessage(message.target, if (prefixWithNickname) "${message.user.nickname}: $response" else response)
    }
}