package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.handlers.EventHandler
import com.dmdirc.ktirc.events.mutators.EventMutator
import com.dmdirc.ktirc.messages.MessageProcessor
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.channels.ReceiveChannel

internal class MessageHandler(
        private val processors: List<MessageProcessor>,
        private val mutators: List<EventMutator>,
        val handlers: MutableList<EventHandler>) {

    private val log by logger()

    suspend fun processMessages(ircClient: IrcClient, messages: ReceiveChannel<IrcMessage>) {
        for (message in messages) {
            emitEvents(ircClient, message.toEvents())
        }
    }

    fun emitEvent(ircClient: IrcClient, ircEvent: IrcEvent) = emitEvents(ircClient, listOf(ircEvent))

    fun emitEvents(ircClient: IrcClient, ircEvents: List<IrcEvent>) {
        mutators.fold(ircEvents) { events, mutator ->
            events.flatMap { mutator.mutateEvent(ircClient, it) }
        }.forEach { event ->
            log.fine { "Dispatching event of type ${event::class}" }
            handlers.forEach { it.processEvent(ircClient, event) }
        }
    }

    private fun IrcMessage.toEvents() = this.getProcessor()?.process(this) ?: emptyList()
    private fun IrcMessage.getProcessor() = processors.firstOrNull { it.commands.contains(command) } ?: run {
        log.warning { "No processor found for $command" }
        null
    }

}
