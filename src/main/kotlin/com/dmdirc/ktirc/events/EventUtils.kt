package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.asUser

internal fun ChannelNamesReceived.toModesAndUsers(client: IrcClient) = sequence {
    val modePrefixes = client.serverState.features[ServerFeature.ModePrefixes]!!
    for (user in names) {
        user.takeWhile { modePrefixes.isPrefix(it) }.let { prefix ->
            yield(Pair(modePrefixes.getModes(prefix), user.substring(prefix.length).asUser()))
        }
    }
}.toList()
