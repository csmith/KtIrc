package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient

internal interface SaslMechanism {

    val ircName: String
    val priority: Int

    fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?)

}
