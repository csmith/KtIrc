package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.model.IrcMessage

internal class PrivmsgProcessor : MessageProcessor {

    override val commands = arrayOf("PRIVMSG")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        listOf(MessageReceived(message.time, user, String(message.params[0]), String(message.params[1])))
    } ?: emptyList()

}
