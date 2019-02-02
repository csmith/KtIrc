package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.messages.capabilityEndMessage
import com.dmdirc.ktirc.messages.capabilityRequestMessage
import com.dmdirc.ktirc.model.CapabilitiesNegotiationState
import com.dmdirc.ktirc.model.CapabilitiesState
import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.util.logger

internal class CapabilitiesHandler : EventHandler {

    private val log by logger()

    override fun processEvent(client: IrcClient, event: IrcEvent) {
        when (event) {
            is ServerCapabilitiesReceived -> handleCapabilitiesReceived(client.serverState.capabilities, event.capabilities)
            is ServerCapabilitiesFinished -> handleCapabilitiesFinished(client)
            is ServerCapabilitiesAcknowledged -> handleCapabilitiesAcknowledged(client, event.capabilities)
        }
    }

    private fun handleCapabilitiesReceived(state: CapabilitiesState, capabilities: Map<Capability, String>) {
        state.advertisedCapabilities.putAll(capabilities)
    }

    private fun handleCapabilitiesFinished(client: IrcClient) {
        // TODO: We probably need to split the outgoing REQ lines if there are lots of caps
        // TODO: For caps with values we'll need to decide which value to use/whether to enable them/etc
        with (client.serverState.capabilities) {
            if (advertisedCapabilities.keys.isEmpty()) {
                negotiationState = CapabilitiesNegotiationState.FINISHED
                client.send(capabilityEndMessage())
            } else {
                negotiationState = CapabilitiesNegotiationState.AWAITING_ACK
                advertisedCapabilities.keys.map { it.name }.let {
                    log.info { "Requesting capabilities: ${it.toList()}" }
                    client.send(capabilityRequestMessage(it))
                }
            }
        }
    }

    private fun handleCapabilitiesAcknowledged(client: IrcClient, capabilities: Map<Capability, String>) {
        // TODO: Check if everything we wanted is enabled
        with (client.serverState.capabilities) {
            log.info { "Acknowledged capabilities: ${capabilities.keys.map { it.name }.toList()}" }
            negotiationState = CapabilitiesNegotiationState.FINISHED
            enabledCapabilities.putAll(capabilities)
            client.send(capabilityEndMessage())
        }
    }

}
