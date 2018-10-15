package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient

class ServerStateHandler : EventHandler {

    override suspend fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ServerWelcome -> client.serverState.localNickname = event.localNick
            is ServerFeaturesUpdated -> client.serverState.features.setAll(event.serverFeatures)
        }
    }

}