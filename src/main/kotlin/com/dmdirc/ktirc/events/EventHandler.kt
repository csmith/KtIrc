package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient

interface EventHandler {

    suspend fun processEvent(client: IrcClient, event: IrcEvent)

}

val eventHandlers = setOf(
        ChannelStateHandler(),
        PingHandler(),
        ServerStateHandler()
)