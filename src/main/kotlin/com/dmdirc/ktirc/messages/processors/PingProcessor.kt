package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.PingReceived
import com.dmdirc.ktirc.events.PongReceived
import com.dmdirc.ktirc.model.IrcMessage

internal class PingProcessor : MessageProcessor {

    override val commands = arrayOf("PING", "PONG")

    override fun process(message: IrcMessage) = if (message.command == "PING") {
        listOf(PingReceived(message.metadata, message.params[0]))
    } else {
        listOf(PongReceived(message.metadata, message.params[0]))
    }

}
