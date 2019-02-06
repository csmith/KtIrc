package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CapabilitiesStateTest {

    @Test
    fun `defaults negotiation state to awaiting list`() = with(CapabilitiesState()) {
        assertEquals(CapabilitiesNegotiationState.AWAITING_LIST, negotiationState)
    }

    @Test
    fun `reset clears all state`() = with(CapabilitiesState()) {
        advertisedCapabilities[Capability.SaslAuthentication] = "foo"
        enabledCapabilities[Capability.SaslAuthentication] = "foo"
        negotiationState = CapabilitiesNegotiationState.FINISHED

        reset()

        assertTrue(advertisedCapabilities.isEmpty())
        assertTrue(enabledCapabilities.isEmpty())
        assertEquals(CapabilitiesNegotiationState.AWAITING_LIST, negotiationState)
    }

}
