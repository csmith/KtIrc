package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.model.ChannelState
import com.dmdirc.ktirc.model.ChannelUser
import com.dmdirc.ktirc.util.logger

class ChannelStateHandler : EventHandler {

    private val log by logger()

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ChannelJoined -> handleJoin(client, event)
            is ChannelParted -> handlePart(client, event)
            is ChannelNamesReceived -> handleNamesReceived(client, event)
            is ChannelNamesFinished -> handleNamesFinished(client, event)
            is UserQuit -> handleQuit(client, event)
        }
    }

    private fun handleJoin(client: IrcClient, event: ChannelJoined) {
        if (client.isLocalUser(event.user)) {
            log.info { "Joined new channel: ${event.channel}" }
            client.channelState += ChannelState(event.channel) { client.caseMapping }
        }

        client.channelState[event.channel]?.let { it.users += ChannelUser(event.user.nickname) }
    }

    private fun handlePart(client: IrcClient, event: ChannelParted) {
        if (client.isLocalUser(event.user)) {
            log.info { "Left channel: ${event.channel}" }
            client.channelState -= event.channel
        } else {
            client.channelState[event.channel]?.let {
                it.users -= event.user.nickname
            }
        }
    }

    private fun handleNamesReceived(client: IrcClient, event: ChannelNamesReceived) {
        val channel = client.channelState[event.channel] ?: return

        if (!channel.receivingUserList) {
            log.finer { "Started receiving names list for ${channel.name}" }
            channel.users.clear()
            channel.receivingUserList = true
        }

        event.toModesAndUsers(client).forEach { (modes, user) ->
            channel.users += ChannelUser(user.nickname, modes)
        }
    }

    private fun handleNamesFinished(client: IrcClient, event: ChannelNamesFinished) {
        client.channelState[event.channel]?.let {
            it.receivingUserList = false
            log.finest { "Finished receiving names in ${event.channel}. Users: ${it.users.toList()}" }
        }
    }

    private fun handleQuit(client: IrcClient, event: UserQuit) {
        client.channelState.forEach { it.users -= event.user.nickname }
    }

}