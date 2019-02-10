package com.dmdirc.ktirc.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus
import java.time.LocalDateTime

internal class ServerStateHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent): List<IrcEvent> {
        when (event) {
            is ServerConnecting -> client.serverState.status = ServerStatus.Connecting
            is ServerConnected -> client.serverState.status = ServerStatus.Negotiating
            is ServerDisconnected -> client.serverState.status = ServerStatus.Disconnected
            is ServerWelcome -> handleWelcome(client.serverState, event.server, event.localNick)
            is ServerFeaturesUpdated -> client.serverState.features.setAll(event.serverFeatures)

            // Events that won't trigger a server ready event
            is PingReceived -> Unit
            is ServerCapabilitiesReceived -> Unit
            is ServerCapabilitiesAcknowledged -> Unit
            is ServerCapabilitiesFinished -> Unit

            else -> return checkReadyState(client, event.time)
        }
        return emptyList()
    }

    private fun handleWelcome(serverState: ServerState, server: String, localNick: String) {
        serverState.receivedWelcome = true
        serverState.serverName = server
        serverState.localNickname = localNick
    }

    private fun checkReadyState(client: IrcClient, time: LocalDateTime): List<IrcEvent> {
        if (client.serverState.receivedWelcome && client.serverState.status == ServerStatus.Negotiating) {
            client.serverState.status = ServerStatus.Ready
            return listOf(ServerReady(time))
        }
        return emptyList()
    }

}
