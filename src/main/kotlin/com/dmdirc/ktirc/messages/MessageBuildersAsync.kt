package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.ExperimentalIrcClient
import com.dmdirc.ktirc.events.ChannelParted

/** Sends a request to part the given channel. */
internal fun ExperimentalIrcClient.sendPartAsync(channel: String, reason: String? = null) =
        sendAsync("PART", reason?.let { arrayOf(channel, reason) } ?: arrayOf(channel)) {
            it is ChannelParted && isLocalUser(it.user) && it.target == channel
        }
