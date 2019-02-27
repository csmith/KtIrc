package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class AsyncResponseHandler(private val scope: CoroutineScope = GlobalScope) : EventHandler {

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        client.serverState.asyncResponseState.pendingResponses.values
                .filter { it.second(event) }
                .forEach {
                    scope.launch {
                        it.first.send(event)
                    }
                }
    }

}
