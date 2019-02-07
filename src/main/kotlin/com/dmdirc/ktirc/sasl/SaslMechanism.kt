package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig

internal interface SaslMechanism {

    val ircName: String
    val priority: Int

    fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?)

}

internal fun SaslConfig.createSaslMechanism(): List<SaslMechanism> = mechanisms.mapNotNull {
    when (it.toUpperCase()) {
        "EXTERNAL" -> ExternalMechanism()
        "PLAIN" -> PlainMechanism(this)
        else -> null
    }
}
