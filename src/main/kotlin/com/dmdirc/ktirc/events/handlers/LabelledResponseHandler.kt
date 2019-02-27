package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class LabelledResponseHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        client.serverState.asyncResponseState.pendingResponses.values
                .filter { it.second(event) }
                .forEach {
                    GlobalScope.launch {
                        it.first.send(event)
                    }
                }
    }

}
