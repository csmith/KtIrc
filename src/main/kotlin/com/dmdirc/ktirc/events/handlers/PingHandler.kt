package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.PingReceived
import com.dmdirc.ktirc.messages.sendPong

internal class PingHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is PingReceived -> client.sendPong(event.nonce)
        }
    }

}
