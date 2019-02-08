package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.messages.sendAuthenticationMessage

internal interface SaslMechanism {

    val ircName: String
    val priority: Int

    fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?)

}

internal fun SaslConfig.createSaslMechanism(): List<SaslMechanism> = mechanisms.mapNotNull {
    when (it.toUpperCase()) {
        "SCRAM-SHA-1" -> ScramMechanism("SHA-1", 10, this)
        "SCRAM-SHA-256" -> ScramMechanism("SHA-256", 20, this)
        "EXTERNAL" -> ExternalMechanism()
        "PLAIN" -> PlainMechanism(this)
        else -> null
    }
}

internal fun IrcClient.sendAuthenticationData(data: String) {
    val lines = data.toByteArray().toBase64().chunked(400)
    lines.forEach(this::sendAuthenticationMessage)
    if (lines.last().length == 400) {
        sendAuthenticationMessage("+")
    }
}
