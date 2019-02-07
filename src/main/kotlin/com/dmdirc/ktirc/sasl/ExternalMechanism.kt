package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.sendAuthenticationMessage

internal class ExternalMechanism : SaslMechanism {

    override val ircName = "EXTERNAL"
    override val priority = 100

    override fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?) {
        client.sendAuthenticationMessage("+")
    }

}
