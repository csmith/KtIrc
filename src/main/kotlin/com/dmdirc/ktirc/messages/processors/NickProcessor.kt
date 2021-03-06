package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.UserNickChanged
import com.dmdirc.ktirc.model.IrcMessage

internal class NickProcessor : MessageProcessor {

    override val commands = arrayOf("NICK")

    override fun process(message: IrcMessage) =
            message.sourceUser?.let { listOf(UserNickChanged(message.metadata, it, String(message.params[0]))) }
                    ?: emptyList()

}
