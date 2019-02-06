package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.sasl.SaslMechanism

internal class SaslState(private val mechanisms: Collection<SaslMechanism>) {

    var saslBuffer: String = ""
    var currentMechanism: SaslMechanism? = null
        set(value) {
            mechanismState = null
            field = value
        }

    var mechanismState: Any? = null

    fun getPreferredSaslMechanism(serverMechanisms: String?): SaslMechanism? {
        serverMechanisms ?: return null
        val serverSupported = serverMechanisms.split(',')
        return mechanisms
                .filter { it.priority < currentMechanism?.priority ?: Int.MAX_VALUE }
                .filter { serverMechanisms.isEmpty() || it.ircName in serverSupported }
                .maxBy { it.priority }
    }

}
