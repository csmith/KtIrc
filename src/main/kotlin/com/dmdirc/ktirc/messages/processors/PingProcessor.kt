package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.PingReceived
import com.dmdirc.ktirc.messages.processors.MessageProcessor
import com.dmdirc.ktirc.model.IrcMessage

internal class PingProcessor : MessageProcessor {

    override val commands = arrayOf("PING")

    override fun process(message: IrcMessage) = listOf(PingReceived(message.metadata, message.params[0]))

}
