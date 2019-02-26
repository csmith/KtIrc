package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.messages.RPL_WELCOME
import com.dmdirc.ktirc.model.IrcMessage

internal class WelcomeProcessor : MessageProcessor {

    override val commands = arrayOf(RPL_WELCOME)

    override fun process(message: IrcMessage) = listOf(ServerWelcome(
            message.metadata,
            message.serverName(),
            message.localNick()))

    private fun IrcMessage.serverName() = prefix?.let { String(it) } ?: ""
    private fun IrcMessage.localNick() = if (params.isEmpty()) "" else String(params[0])

}
