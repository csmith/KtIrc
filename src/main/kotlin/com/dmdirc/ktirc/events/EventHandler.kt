package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient

@FunctionalInterface
interface EventHandler {

    suspend fun processEvent(client: IrcClient, event: IrcEvent)

}

val eventHandlers = listOf(
        CapabilitiesHandler(),
        ChannelStateHandler(),
        PingHandler(),
        ServerStateHandler(),
        UserStateHandler()
)