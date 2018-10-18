package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelParted
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.asUser

internal class PartProcessor : MessageProcessor {

    override val commands = arrayOf("PART")

    override fun process(message: IrcMessage) = message.prefix?.let {
        listOf(ChannelParted(message.time, it.asUser(), message.channel, message.reason))
    } ?: emptyList()

    private val IrcMessage.channel
        get() = String(params[0])

    private val IrcMessage.reason
        get() = if (params.size > 1) String(params[1]) else ""

}