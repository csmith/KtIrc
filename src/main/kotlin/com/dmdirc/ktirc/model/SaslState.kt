package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.sasl.SaslMechanism
import com.dmdirc.ktirc.sasl.createSaslMechanism

internal class SaslState(config: SaslConfig?) {

    val mechanisms = (config?.createSaslMechanism() ?: emptyList()).toMutableList()

    var saslBuffer: String = ""

    var currentMechanism: SaslMechanism? = null
        set(value) {
            mechanismState = null
            field = value
        }

    var mechanismState: Any? = null

    fun getPreferredSaslMechanism(serverMechanisms: Collection<String>): SaslMechanism? {
        return mechanisms
                .filter { it.priority < currentMechanism?.priority ?: Int.MAX_VALUE }
                .filter { serverMechanisms.isEmpty() || it.ircName in serverMechanisms }
                .maxBy { it.priority }
    }

    fun reset() {
        saslBuffer = ""
        currentMechanism = null
    }

}
