package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.UserHostChanged
import com.dmdirc.ktirc.model.IrcMessage

internal class ChangeHostProcessor : MessageProcessor {

    override fun process(message: IrcMessage) = message.sourceUser?.let {
        listOf(UserHostChanged(message.metadata, it, String(message.params[0]), String(message.params[1])))
    } ?: emptyList()

    override val commands = arrayOf("CHGHOST")

}
