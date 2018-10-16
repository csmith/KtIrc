package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.model.ChannelState
import com.dmdirc.ktirc.util.logger

class ChannelStateHandler : EventHandler {

    private val log by logger()

    override suspend fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ChannelJoined -> handleJoin(client, event)
        }
    }

    private fun handleJoin(client: IrcClient, event: ChannelJoined) {
        if (client.isLocalUser(event.user)) {
            log.info { "Joined new channel: ${event.channel}" }
            client.channelState += ChannelState(event.channel)
        }
        // TODO: Add user to channel
    }

}