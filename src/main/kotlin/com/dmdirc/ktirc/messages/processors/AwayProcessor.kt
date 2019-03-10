package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.UserAway
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.asUser

internal class AwayProcessor : MessageProcessor {

    override val commands = arrayOf("AWAY")

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            "AWAY" -> message.prefix?.let { yield(UserAway(message.metadata, it.asUser(), message.awayMessage)) }
        }
    }.toList()

    private val IrcMessage.awayMessage: String?
        get() = if (params.isEmpty()) null else String(params[0])

}

