package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ChannelNamesFinished
import com.dmdirc.ktirc.events.ChannelNamesReceived
import com.dmdirc.ktirc.model.IrcMessage

internal class NamesProcessor : MessageProcessor {

    override val commands = arrayOf("353", "366")

    override fun process(message: IrcMessage) = when (message.command) {
        "353" -> listOf(ChannelNamesReceived(message.metadata, String(message.params[2]), String(message.params[3]).split(' ')))
        "366" -> listOf(ChannelNamesFinished(message.metadata, String(message.params[1])))
        else -> emptyList()
    }

}
