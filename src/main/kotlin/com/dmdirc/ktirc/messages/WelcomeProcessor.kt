package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.model.IrcMessage

internal class WelcomeProcessor : MessageProcessor {

    override val commands = arrayOf(RPL_WELCOME)

    override fun process(message: IrcMessage) = listOf(ServerWelcome(message.metadata, message.serverName(), String(message.params[0])))

    private fun IrcMessage.serverName() = prefix?.let { String(it) } ?: ""

}
