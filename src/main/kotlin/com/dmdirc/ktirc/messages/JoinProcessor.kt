package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.model.IrcMessage

internal class JoinProcessor : MessageProcessor {

    override val commands = arrayOf("JOIN")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        listOf(ChannelJoined(message.time, user, String(message.params[0])))
    } ?: emptyList()

}