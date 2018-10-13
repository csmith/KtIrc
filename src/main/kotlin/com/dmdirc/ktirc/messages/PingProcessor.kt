package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.PingReceived
import com.dmdirc.ktirc.io.IrcMessage

class PingProcessor : MessageProcessor {

    override val commands = arrayOf("PING")

    override fun process(message: IrcMessage) = listOf(PingReceived(message.params[0]))

}