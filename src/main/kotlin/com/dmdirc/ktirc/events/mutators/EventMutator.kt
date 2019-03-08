package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.MessageEmitter

@FunctionalInterface
internal interface EventMutator {

    fun mutateEvent(client: IrcClient, messageEmitter: MessageEmitter, event: IrcEvent): List<IrcEvent>

}

internal val eventMutators = listOf(
        ServerReadyMutator(),
        ChannelFanOutMutator(),
        NickChangeRequiredMutator(),
        BatchMutator()
)
