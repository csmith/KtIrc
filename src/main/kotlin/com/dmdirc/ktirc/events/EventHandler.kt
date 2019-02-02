package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient

@FunctionalInterface
interface EventHandler {

    fun processEvent(client: IrcClient, event: IrcEvent)

}

val eventHandlers = listOf(
        CapabilitiesHandler(),
        ChannelStateHandler(),
        PingHandler(),
        ServerStateHandler(),
        UserStateHandler()
)