package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendModeRequest
import com.dmdirc.ktirc.model.ChannelState
import com.dmdirc.ktirc.model.ChannelTopic
import com.dmdirc.ktirc.model.ChannelUser
import com.dmdirc.ktirc.util.logger

internal class ChannelStateHandler : EventHandler {

    private val log by logger()

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ChannelJoined -> handleJoin(client, event)
            is ChannelParted -> handlePart(client, event)
            is ChannelNamesReceived -> handleNamesReceived(client, event)
            is ChannelNamesFinished -> handleNamesFinished(client, event)
            is ChannelUserKicked -> handleKick(client, event)
            is ChannelTopicDiscovered -> handleTopicDiscovered(client, event)
            is ChannelTopicMetadataDiscovered -> handleTopicMetadata(client, event)
            is ChannelTopicChanged -> handleTopicChanged(client, event)
            is ChannelQuit -> handleQuit(client, event)
            is ChannelNickChanged -> handleNickChanged(client, event)
            is ModeChanged -> handleModeChanged(client, event)
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

    private fun handleKick(client: IrcClient, event: ChannelUserKicked) {
        if (client.isLocalUser(event.victim)) {
            log.info { "Kicked from channel: ${event.channel}" }
            client.channelState -= event.channel
        } else {
            client.channelState[event.channel]?.let {
                it.users -= event.victim
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
            if (client.behaviour.requestModesOnJoin && !it.modesDiscovered) {
                client.sendModeRequest(it.name)
            }
        }
    }

    private fun handleTopicDiscovered(client: IrcClient, event: ChannelTopicDiscovered) {
        client.channelState[event.channel]?.let {
            if (!it.topicDiscovered) {
                it.topic = ChannelTopic(event.topic)
                if (event.topic == null) {
                    it.topicDiscovered = true
                }
            }
        }
    }

    private fun handleTopicMetadata(client: IrcClient, event: ChannelTopicMetadataDiscovered) {
        client.channelState[event.channel]?.let {
            if (!it.topicDiscovered) {
                it.topic = ChannelTopic(it.topic.topic, event.user, event.setTime)
                it.topicDiscovered = true
            }
        }
    }

    private fun handleTopicChanged(client: IrcClient, event: ChannelTopicChanged) {
        client.channelState[event.channel]?.let {
            it.topic = ChannelTopic(event.topic, event.user, event.time)
        }
    }

    private fun handleModeChanged(client: IrcClient, event: ModeChanged) {
        val chan = client.channelState[event.target] ?: return
        if (event.discovered) {
            chan.modesDiscovered = true
            chan.modes.clear()
        }

        var adding = true
        var argumentOffset = 0
        for (char in event.modes) {
            when (char) {
                '+' -> adding = true
                '-' -> adding = false
                else -> argumentOffset += if (client.serverState.isChannelUserMode(char)) {
                    adjustUserMode(client, chan, char, adding, event.arguments[argumentOffset])
                } else {
                    adjustMode(client, chan, char, event.arguments, argumentOffset, adding)
                }
            }
        }
    }

    private fun adjustMode(client: IrcClient, chan: ChannelState, mode: Char, arguments: Array<String>, argumentOffset: Int, adding: Boolean): Int {
        val type = client.serverState.channelModeType(mode)
        val takesParam = if (adding) type.needsParameterToSet else type.needsParameterToUnset
        val param = if (takesParam) arguments[argumentOffset] else ""
        if (adding) {
            chan.modes[mode] = param
        } else {
            chan.modes.remove(mode)
        }
        return if (takesParam) 1 else 0
    }

    private fun adjustUserMode(client: IrcClient, chan: ChannelState, mode: Char, adding: Boolean, user: String): Int {
        chan.users[user]?.let { channelUser ->
            // Filter from the master list of mode prefixes so that ordering is consistent
            channelUser.modes = client.serverState.channelModePrefixes.modes.filter {
                if (adding) {
                    it == mode || it in channelUser.modes
                } else {
                    it != mode && it in channelUser.modes
                }
            }
        }
        return 1
    }

    private fun handleQuit(client: IrcClient, event: ChannelQuit) {
        client.channelState[event.channel]?.let {
            it.users -= event.user.nickname
        }
    }

    private fun handleNickChanged(client: IrcClient, event: ChannelNickChanged) {
        client.channelState[event.channel]?.let {
            it.users[event.user.nickname]?.let { chanUser ->
                chanUser.nickname = event.newNick
            }
        }
    }

}
