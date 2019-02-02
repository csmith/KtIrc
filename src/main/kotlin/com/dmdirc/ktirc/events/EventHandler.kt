package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient

@FunctionalInterface
internal interface EventHandler {

    fun processEvent(client: IrcClient, event: IrcEvent)

}

internal val eventHandlers = listOf(
        CapabilitiesHandler(),
        ChannelStateHandler(),
        PingHandler(),
        ServerStateHandler(),
        UserStateHandler()
)
