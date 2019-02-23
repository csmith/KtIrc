package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ChannelUserKicked
import com.dmdirc.ktirc.model.IrcMessage

internal class KickProcessor : MessageProcessor {

    override val commands = arrayOf("KICK")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        listOf(ChannelUserKicked(message.metadata, user, message.channel, message.victim, message.reason))
    } ?: emptyList()

    private val IrcMessage.channel
        get() = String(params[0])

    private val IrcMessage.victim
        get() = String(params[1])

    private val IrcMessage.reason
        get() = if (params.size > 2) String(params[2]) else ""

}
