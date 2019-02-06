package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.sendAuthenticationMessage

internal class PlainMechanism : SaslMechanism {

    override val ircName = "PLAIN"
    override val priority = 0

    override fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?) {
        with (client.profile) {
            client.sendAuthenticationMessage("$authUsername\u0000$authUsername\u0000$authPassword".toByteArray().toBase64())
        }
    }

}
