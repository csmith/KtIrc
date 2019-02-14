package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus

internal class ServerStateHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ServerConnecting -> client.serverState.status = ServerStatus.Connecting
            is ServerConnected -> client.serverState.status = ServerStatus.Negotiating
            is ServerDisconnected -> client.serverState.status = ServerStatus.Disconnected
            is ServerReady -> client.serverState.status = ServerStatus.Ready
            is ServerWelcome -> handleWelcome(client.serverState, event.server, event.localNick)
            is ServerFeaturesUpdated -> client.serverState.features.setAll(event.serverFeatures)
        }
    }

    private fun handleWelcome(serverState: ServerState, server: String, localNick: String) {
        serverState.receivedWelcome = true
        serverState.serverName = server
        serverState.localNickname = localNick
    }

}
