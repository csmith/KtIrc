package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.pongMessage

class PingHandler : EventHandler {

    override suspend fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is PingReceived -> client.send(pongMessage(event.nonce))
        }
    }

}