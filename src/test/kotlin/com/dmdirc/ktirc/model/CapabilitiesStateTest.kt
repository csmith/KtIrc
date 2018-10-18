package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CapabilitiesStateTest {

    @Test
    fun `CapabilitiesState defaults negotiation state to awaiting list`() {
        val capabilitiesState = CapabilitiesState()

        assertEquals(CapabilitiesNegotiationState.AWAITING_LIST, capabilitiesState.negotiationState)
    }

}