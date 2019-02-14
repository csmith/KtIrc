package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent

@FunctionalInterface
internal interface EventMutator {

    fun mutateEvent(client: IrcClient, event: IrcEvent): List<IrcEvent>

}

internal val eventMutators = listOf(
        ServerReadyMutator(),
        ChannelFanOutMutator()
)
