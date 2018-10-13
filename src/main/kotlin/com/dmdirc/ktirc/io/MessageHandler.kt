package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.messages.MessageProcessor
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach

class MessageHandler(private val processors: Collection<MessageProcessor>) {

    suspend fun processMessages(messages: ReceiveChannel<IrcMessage>) {
        messages.consumeEach { it.process() }
    }

    private fun IrcMessage.process() = this.getProcessor()?.process(this)
    private fun IrcMessage.getProcessor() = processors.firstOrNull { it.commands.contains(this.command) }

}