package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.AuthenticationMessage
import com.dmdirc.ktirc.events.SaslFinished
import com.dmdirc.ktirc.events.SaslMechanismNotAvailableError
import com.dmdirc.ktirc.model.IrcMessage

internal class AuthenticationProcessor : MessageProcessor {

    override val commands = arrayOf("AUTHENTICATE", RPL_SASLSUCCESS, ERR_SASLFAIL, RPL_SASLMECHS)

    override fun process(message: IrcMessage) = when(message.command) {
        "AUTHENTICATE" -> listOf(AuthenticationMessage(message.time, message.authenticateArgument))
        RPL_SASLSUCCESS -> listOf(SaslFinished(message.time, true))
        ERR_SASLFAIL -> listOf(SaslFinished(message.time, false))
        RPL_SASLMECHS -> listOf(SaslMechanismNotAvailableError(message.time, message.mechanisms))
        else -> emptyList()
    }

    private val IrcMessage.authenticateArgument: String?
        get() = if (params.isEmpty() || params[0].size == 1 && String(params[0]) == "+") null else String(params[0])

    private val IrcMessage.mechanisms: List<String>
        get() = if (params.size < 2) emptyList() else String(params[1]).split(',')

}
