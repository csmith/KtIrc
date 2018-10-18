package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.asUser

internal class PrivmsgProcessor : MessageProcessor {

    override val commands = arrayOf("PRIVMSG")

    override fun process(message: IrcMessage) = message.prefix?.let {
        listOf(MessageReceived(it.asUser(), String(message.params[0]), String(message.params[1])))
    } ?: emptyList()

}