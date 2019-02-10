package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.handlers.EventHandler
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.messages.MessageProcessor
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.channels.ReceiveChannel

internal class MessageHandler(private val processors: List<MessageProcessor>, val handlers: MutableList<EventHandler>) {

    private val log by logger()

    suspend fun processMessages(ircClient: IrcClient, messages: ReceiveChannel<IrcMessage>) {
        for (message in messages) {
            message.toEvents().forEach { event -> emitEvent(ircClient, event) }
        }
    }

    fun emitEvent(ircClient: IrcClient, ircEvent: IrcEvent) {
        log.fine { "Dispatching event of type ${ircEvent::class}" }
        handlers.forEach { handler ->
            handler.processEvent(ircClient, ircEvent).forEach {
                emitEvent(ircClient, it)
            }
        }
    }

    private fun IrcMessage.toEvents() = this.getProcessor()?.process(this) ?: emptyList()
    private fun IrcMessage.getProcessor() = processors.firstOrNull { it.commands.contains(command) } ?: run {
        log.warning { "No processor found for $command" }
        null
    }

}
