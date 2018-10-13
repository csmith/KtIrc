package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.state.ServerState

class WelcomeProcessor(private val serverState: ServerState) : MessageProcessor {

    override val commands = arrayOf("001")

    override fun process(message: IrcMessage) {
        serverState.localNickname = String(message.params[0])
    }

}