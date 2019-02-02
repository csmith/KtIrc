package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.UserQuit
import com.dmdirc.ktirc.model.IrcMessage

internal class QuitProcessor : MessageProcessor {

    override val commands = arrayOf("QUIT")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        listOf(UserQuit(message.time, user, message.reason))
    } ?: emptyList()

    private val IrcMessage.reason
        get() = if (params.isNotEmpty()) String(params[0]) else ""

}
