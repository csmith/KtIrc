package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.UserQuit
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.model.asUser

internal class QuitProcessor : MessageProcessor {

    override val commands = arrayOf("QUIT")

    override fun process(message: IrcMessage) = message.prefix?.let {
        listOf(UserQuit(it.asUser(), message.reason))
    } ?: emptyList()

    private val IrcMessage.reason
        get() = if (params.isNotEmpty()) String(params[0]) else ""

}