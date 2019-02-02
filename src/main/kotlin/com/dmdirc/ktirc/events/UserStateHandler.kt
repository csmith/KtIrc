package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.model.UserState

class UserStateHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ChannelJoined -> handleJoin(client.userState, event)
            is ChannelParted -> handlePart(client, event)
            is ChannelNamesReceived  -> handleNamesReceived(client, event)
            is UserQuit -> handleQuit(client.userState, event)
        }
    }

    private fun handleJoin(state: UserState, event: ChannelJoined) {
        state.addToChannel(event.user, event.channel)
        state.update(event.user)
    }

    private fun handlePart(client: IrcClient, event: ChannelParted) {
        if (client.isLocalUser(event.user)) {
            // Remove channel from all users
            client.userState.forEach { it.channels -= event.channel }
            client.userState.removeIf { it.channels.isEmpty() && !client.isLocalUser(it.details) }
        } else {
            client.userState[event.user]?.channels?.let {
                it -= event.channel
                if (it.isEmpty()) {
                    client.userState -= event.user
                }
            }
        }
    }

    private fun handleNamesReceived(client: IrcClient, event: ChannelNamesReceived) {
        event.toModesAndUsers(client).forEach { (_, user) ->
            client.userState.addToChannel(user, event.channel)
            client.userState.update(user)
        }
    }

    private fun handleQuit(state: UserState, event: UserQuit) {
        state -= event.user
    }

}