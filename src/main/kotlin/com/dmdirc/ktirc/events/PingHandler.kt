package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.sendPong

internal class PingHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent): List<IrcEvent> {
        when (event) {
            is PingReceived -> client.sendPong(event.nonce)
        }
        return emptyList()
    }

}
