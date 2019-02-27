package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendAuthenticationMessage
import com.dmdirc.ktirc.messages.sendCapabilityEnd
import com.dmdirc.ktirc.messages.sendCapabilityRequest
import com.dmdirc.ktirc.model.CapabilitiesNegotiationState
import com.dmdirc.ktirc.model.CapabilitiesState
import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.capabilities
import com.dmdirc.ktirc.sasl.fromBase64
import com.dmdirc.ktirc.util.logger

internal class CapabilitiesHandler : EventHandler {

    private val log by logger()

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ServerCapabilitiesReceived -> handleCapabilitiesReceived(client.serverState.capabilities, event.capabilities)
            is ServerCapabilitiesFinished -> handleCapabilitiesFinished(client)
            is ServerCapabilitiesAcknowledged -> handleCapabilitiesAcknowledged(client, event.capabilities)
            is AuthenticationMessage -> handleAuthenticationMessage(client, event.argument)
            is SaslMechanismNotAvailableError -> handleSaslMechanismChange(client, event.mechanisms)
            is SaslFinished -> handleSaslFinished(client)
        }
    }

    private fun handleCapabilitiesReceived(state: CapabilitiesState, capabilities: Map<String, String>) {
        state.advertisedCapabilities.putAll(capabilities)
    }

    private fun handleCapabilitiesFinished(client: IrcClient) {
        // TODO: We probably need to split the outgoing REQ lines if there are lots of caps
        // TODO: For caps with values we may need to decide which value to use/whether to enable them/etc
        with(client.serverState.capabilities) {
            if (advertisedCapabilities.keys.isEmpty()) {
                negotiationState = CapabilitiesNegotiationState.FINISHED
                client.sendCapabilityEnd()
            } else {
                negotiationState = CapabilitiesNegotiationState.AWAITING_ACK
                advertisedCapabilities.keys.let {
                    log.info { "Requesting capabilities: ${it.toList()}" }
                    client.sendCapabilityRequest(it)
                }
            }
        }
    }

    private fun handleCapabilitiesAcknowledged(client: IrcClient, ackedCapabilities: Map<String, String>) {
        // TODO: Check if everything we wanted is enabled
        with(client.serverState.capabilities) {
            log.info { "Acknowledged capabilities: ${ackedCapabilities.keys.toList()}" }
            ackedCapabilities.forEach { n, v ->
                capabilities[n]?.let {
                    enabledCapabilities[it] = v
                }
            }

            if (client.serverState.sasl.mechanisms.isNotEmpty()) {
                // TODO: Icky. What if SASL had multiple names?
                advertisedCapabilities[Capability.SaslAuthentication.names[0]]?.let { serverCaps ->
                    if (startSaslAuth(client, if (serverCaps.isEmpty()) emptyList() else serverCaps.split(','))) {
                        return
                    }
                }
                log.warning { "SASL is enabled but we couldn't negotiate a SASL mechanism with the server" }
            }

            client.endNegotiation()
        }
    }

    private fun handleSaslMechanismChange(client: IrcClient, mechanisms: Collection<String>) {
        if (!startSaslAuth(client, mechanisms)) {
            log.warning { "SASL is enabled but we couldn't negotiate a SASL mechanism with the server" }
            client.endNegotiation()
        }
    }

    private fun startSaslAuth(client: IrcClient, serverMechanisms: Collection<String>) =
            with(client.serverState) {
                sasl.getPreferredSaslMechanism(serverMechanisms)?.let { mechanism ->
                    log.info { "Attempting SASL authentication using ${mechanism.ircName}" }
                    sasl.currentMechanism = mechanism
                    capabilities.negotiationState = CapabilitiesNegotiationState.AUTHENTICATING
                    client.sendAuthenticationMessage(mechanism.ircName)
                    true
                } ?: false
            }

    private fun handleAuthenticationMessage(client: IrcClient, argument: String?) {
        if (argument?.length == 400) {
            client.serverState.sasl.saslBuffer += argument
            return
        }

        client.serverState.sasl.currentMechanism
                ?.handleAuthenticationEvent(client, client.getStoredSaslBuffer(argument)?.fromBase64())
                ?: run { client.sendAuthenticationMessage("*") }
    }

    private fun handleSaslFinished(client: IrcClient) = with(client) {
        with(serverState.sasl) {
            saslBuffer = ""
            mechanismState = null
            currentMechanism = null
        }
        endNegotiation()
    }

    private fun IrcClient.endNegotiation() {
        serverState.capabilities.negotiationState = CapabilitiesNegotiationState.FINISHED
        sendCapabilityEnd()
    }

    private fun IrcClient.getStoredSaslBuffer(argument: String?): String? {
        val data = serverState.sasl.saslBuffer + (argument ?: "")
        serverState.sasl.saslBuffer = ""
        return if (data.isEmpty()) null else data
    }

}
