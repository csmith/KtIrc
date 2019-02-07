package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.sendAuthenticationMessage
import com.dmdirc.ktirc.messages.sendCapabilityEnd
import com.dmdirc.ktirc.messages.sendCapabilityRequest
import com.dmdirc.ktirc.model.CapabilitiesNegotiationState
import com.dmdirc.ktirc.model.CapabilitiesState
import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.sasl.fromBase64
import com.dmdirc.ktirc.util.logger

internal class CapabilitiesHandler : EventHandler {

    private val log by logger()

    override fun processEvent(client: IrcClient, event: IrcEvent): List<IrcEvent> {
        when (event) {
            is ServerCapabilitiesReceived -> handleCapabilitiesReceived(client.serverState.capabilities, event.capabilities)
            is ServerCapabilitiesFinished -> handleCapabilitiesFinished(client)
            is ServerCapabilitiesAcknowledged -> handleCapabilitiesAcknowledged(client, event.capabilities)
            is AuthenticationMessage -> handleAuthenticationMessage(client, event.argument)
            is SaslFinished -> handleSaslFinished(client)
        }
        return emptyList()
    }

    private fun handleCapabilitiesReceived(state: CapabilitiesState, capabilities: Map<Capability, String>) {
        state.advertisedCapabilities.putAll(capabilities)
    }

    private fun handleCapabilitiesFinished(client: IrcClient) {
        // TODO: We probably need to split the outgoing REQ lines if there are lots of caps
        // TODO: For caps with values we may need to decide which value to use/whether to enable them/etc
        with (client.serverState.capabilities) {
            if (advertisedCapabilities.keys.isEmpty()) {
                negotiationState = CapabilitiesNegotiationState.FINISHED
                client.sendCapabilityEnd()
            } else {
                negotiationState = CapabilitiesNegotiationState.AWAITING_ACK
                advertisedCapabilities.keys.map { it.name }.let {
                    log.info { "Requesting capabilities: ${it.toList()}" }
                    client.sendCapabilityRequest(it)
                }
            }
        }
    }

    private fun handleCapabilitiesAcknowledged(client: IrcClient, capabilities: Map<Capability, String>) {
        // TODO: Check if everything we wanted is enabled
        with (client.serverState.capabilities) {
            log.info { "Acknowledged capabilities: ${capabilities.keys.map { it.name }.toList()}" }
            enabledCapabilities.putAll(capabilities)

            if (client.serverState.sasl.mechanisms.isNotEmpty()) {
                client.serverState.sasl.getPreferredSaslMechanism(enabledCapabilities[Capability.SaslAuthentication])?.let { mechanism ->
                    log.info { "Attempting SASL authentication using ${mechanism.ircName}" }
                    client.serverState.sasl.currentMechanism = mechanism
                    negotiationState = CapabilitiesNegotiationState.AUTHENTICATING
                    client.sendAuthenticationMessage(mechanism.ircName)
                    return
                }
                log.warning { "SASL is enabled but we couldn't negotiate a SASL mechanism with the server" }
            }

            client.endNegotiation()
        }
    }

    private fun handleAuthenticationMessage(client: IrcClient, argument: String?) {
        if (argument?.length == 400) {
            client.serverState.sasl.saslBuffer += argument
            return
        }

        client.serverState.sasl.currentMechanism?.let {
            it.handleAuthenticationEvent(client, client.getStoredSaslBuffer(argument)?.fromBase64())
        } ?: run {
            client.sendAuthenticationMessage("*")
        }
    }

    private fun handleSaslFinished(client: IrcClient) = with (client) {
        with (serverState.sasl) {
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
