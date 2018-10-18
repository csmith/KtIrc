package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.model.IrcMessage

internal class WelcomeProcessor : MessageProcessor {

    override val commands = arrayOf("001")

    override fun process(message: IrcMessage) = listOf(ServerWelcome(message.time, String(message.params[0])))

}