package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.io.IrcMessage

class WelcomeProcessor : MessageProcessor {

    override val commands = arrayOf("001")

    override fun process(message: IrcMessage) = listOf(ServerWelcome(String(message.params[0])))

}