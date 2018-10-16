package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelNamesFinished
import com.dmdirc.ktirc.events.ChannelNamesReceived
import com.dmdirc.ktirc.io.IrcMessage

internal class NamesProcessor : MessageProcessor {

    override val commands = arrayOf("353", "366")

    override fun process(message: IrcMessage) = when (message.command) {
        "353" -> listOf(ChannelNamesReceived(String(message.params[2]), String(message.params[3]).split(' ')))
        "366" -> listOf(ChannelNamesFinished(String(message.params[1])))
        else -> emptyList()
    }

}
