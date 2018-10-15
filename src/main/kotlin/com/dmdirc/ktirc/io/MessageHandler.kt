package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.EventHandler
import com.dmdirc.ktirc.messages.MessageProcessor
import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach

class MessageHandler(private val processors: Collection<MessageProcessor>, private val handlers: Collection<EventHandler>) {

    private val log by logger()

    suspend fun processMessages(ircClient: IrcClient, messages: ReceiveChannel<IrcMessage>) {
        messages.consumeEach {
            it.process().forEach { event ->
                handlers.forEach { handler ->
                    handler.processEvent(ircClient, event)
                }
            }
        }
    }

    private fun IrcMessage.process() = this.getProcessor()?.process(this) ?: emptyList()
    private fun IrcMessage.getProcessor() = processors.firstOrNull { it.commands.contains(command) } ?: run {
        log.warning { "No processor found for $command" }
        null
    }

}