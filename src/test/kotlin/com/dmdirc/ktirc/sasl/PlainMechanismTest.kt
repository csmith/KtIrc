package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.model.ServerState
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PlainMechanismTest {

    private val fakeServerState = ServerState("", "")
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }

    private val mechanism = PlainMechanism(SaslConfig().apply {
        username = "acidB"
        password = "HackThePlan3t!"
    })

    @Test
    fun `sends encoded username and password when first message received`() {
        val slot = slot<String>()
        every { ircClient.send(eq("AUTHENTICATE"), capture(slot)) } just Runs
        mechanism.handleAuthenticationEvent(ircClient, null)

        val data = String(slot.captured.fromBase64()).split('\u0000')
        assertEquals("acidB", data[0])
        assertEquals("acidB", data[1])
        assertEquals("HackThePlan3t!", data[2])
    }

}