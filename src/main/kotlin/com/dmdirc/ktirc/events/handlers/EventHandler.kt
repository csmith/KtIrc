package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent

@FunctionalInterface
internal interface EventHandler {

    fun processEvent(client: IrcClient, event: IrcEvent)

}

internal val eventHandlers = listOf(
        CapabilitiesHandler(),
        ChannelStateHandler(),
        PingHandler(),
        ServerStateHandler(),
        UserStateHandler(),
        AsyncResponseHandler()
)
