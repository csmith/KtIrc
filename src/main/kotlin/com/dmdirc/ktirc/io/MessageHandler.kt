package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.handlers.EventHandler
import com.dmdirc.ktirc.events.mutators.EventMutator
import com.dmdirc.ktirc.messages.MessageProcessor
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.channels.ReceiveChannel

internal interface MessageEmitter {

    fun handleEvent(ircClient: IrcClient, ircEvent: IrcEvent, processOnly: Boolean = false) = handleEvents(ircClient, listOf(ircEvent), processOnly)

    fun handleEvents(ircClient: IrcClient, ircEvents: List<IrcEvent>, processOnly: Boolean = false)

}

internal class MessageHandler(
        processors: List<MessageProcessor>,
        private val mutators: List<EventMutator>,
        private val handlers: List<EventHandler>) : MessageEmitter {

    private val log by logger()

    private val emitters = mutableListOf<(IrcEvent) -> Unit>()
    private val processorMap = processors.flatMap { it.commands.map { c -> c to it } }.toMap()

    suspend fun processMessages(ircClient: IrcClient, messages: ReceiveChannel<IrcMessage>) {
        for (message in messages) {
            handleEvents(ircClient, message.toEvents())
        }
    }

    override fun handleEvents(ircClient: IrcClient, ircEvents: List<IrcEvent>, processOnly: Boolean) {
        val events = if (processOnly) ircEvents else ircEvents.mutate(ircClient)
        events.forEach { event ->
            event.process(ircClient)
            if (!processOnly) {
                log.fine { "Dispatching event of type ${event::class}" }
                emitters.forEach { it(event) }
            }
        }
    }

    fun addEmitter(emitter: (IrcEvent) -> Unit) {
        emitters.add(emitter)
    }

    private fun IrcEvent.process(ircClient: IrcClient) = handlers.forEach { it.processEvent(ircClient, this) }

    private fun List<IrcEvent>.mutate(ircClient: IrcClient) = mutators.fold(this) { events, mutator ->
        events.flatMap { mutator.mutateEvent(ircClient, this@MessageHandler, it) }
    }

    private fun IrcMessage.toEvents() = this.getProcessor()?.process(this) ?: emptyList()
    private fun IrcMessage.getProcessor() = processorMap[command] ?: run {
        log.warning { "No processor found for $command" }
        null
    }

}
