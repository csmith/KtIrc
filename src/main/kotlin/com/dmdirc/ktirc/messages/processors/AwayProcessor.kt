package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.UserAway
import com.dmdirc.ktirc.messages.RPL_NOWAWAY
import com.dmdirc.ktirc.messages.RPL_UNAWAY
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.model.asUser

internal class AwayProcessor : MessageProcessor {

    override val commands = arrayOf("AWAY", RPL_UNAWAY, RPL_NOWAWAY)

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            "AWAY" -> message.prefix?.let { yield(UserAway(message.metadata, it.asUser(), message.awayMessage)) }
            RPL_NOWAWAY -> yield(UserAway(message.metadata, User(String(message.params[0])), ""))
            RPL_UNAWAY -> yield(UserAway(message.metadata, User(String(message.params[0])), null))
        }
    }.toList()

    private val IrcMessage.awayMessage: String?
        get() = if (params.isEmpty()) null else String(params[0])

}

