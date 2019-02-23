package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LabelledResponseHandler : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        event.metadata.label?.let {
            GlobalScope.launch {
                client.serverState.labelChannels[it]?.send(event)
            }
        }
    }

}
