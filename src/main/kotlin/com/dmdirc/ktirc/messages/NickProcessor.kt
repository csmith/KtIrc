package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.UserNickChanged
import com.dmdirc.ktirc.model.IrcMessage

internal class NickProcessor : MessageProcessor {

    override val commands = arrayOf("NICK")

    override fun process(message: IrcMessage) =
            message.sourceUser?.let { listOf(UserNickChanged(message.time, it, String(message.params[0]))) }
                    ?: emptyList()

}
