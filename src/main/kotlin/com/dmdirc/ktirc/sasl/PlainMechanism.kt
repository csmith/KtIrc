package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig

internal class PlainMechanism(private val saslConfig: SaslConfig) : SaslMechanism {

    override val ircName = "PLAIN"
    override val priority = 0

    override fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?) {
        with (saslConfig) {
            client.sendAuthenticationData("$username\u0000$username\u0000$password")
        }
    }

}
