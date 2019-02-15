package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.UserState

internal class UserStateHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ChannelJoined -> handleJoin(client.userState, event)
            is ChannelParted -> handlePart(client, event)
            is ChannelUserKicked -> handleKick(client, event)
            is ChannelNamesReceived -> handleNamesReceived(client, event)
            is UserAccountChanged -> handleAccountChanged(client, event)
            is UserNickChanged -> handleNickChanged(client, event)
            is UserHostChanged -> handleHostChanged(client, event)
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

    private fun handleKick(client: IrcClient, event: ChannelUserKicked) {
        if (client.isLocalUser(event.victim)) {
            // Remove channel from all users
            client.userState.forEach { it.channels -= event.channel }
            client.userState.removeIf { it.channels.isEmpty() && !client.isLocalUser(it.details) }
        } else {
            client.userState[event.victim]?.channels?.let {
                it -= event.channel
                if (it.isEmpty()) {
                    client.userState -= event.victim
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

    private fun handleAccountChanged(client: IrcClient, event: UserAccountChanged) {
        client.userState[event.user]?.details?.account = event.newAccount
    }

    private fun handleNickChanged(client: IrcClient, event: UserNickChanged) {
        client.userState[event.user]?.details?.nickname = event.newNick
        if (client.isLocalUser(event.user)) {
            client.serverState.localNickname = event.newNick
        }
    }

    private fun handleHostChanged(client: IrcClient, event: UserHostChanged) {
        client.userState[event.user]?.details?.let {
            it.ident = event.newIdent
            it.hostname = event.newHost
        }
    }

    private fun handleQuit(state: UserState, event: UserQuit) {
        state -= event.user
    }

}
