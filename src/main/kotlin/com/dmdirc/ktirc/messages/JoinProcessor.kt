package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.model.asUser

internal class JoinProcessor : MessageProcessor {

    override val commands = arrayOf("JOIN")

    override fun process(message: IrcMessage) = message.prefix?.let { listOf(ChannelJoined(it.asUser(), String(message.params[0]))) } ?: emptyList()

}