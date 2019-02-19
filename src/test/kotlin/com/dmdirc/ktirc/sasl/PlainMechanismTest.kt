package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.model.ServerState
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PlainMechanismTest {

    private val serverState = ServerState("", "")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    private val mechanism = PlainMechanism(SaslConfig().apply {
        username = "acidB"
        password = "HackThePlan3t!"
    })

    @Test
    fun `sends encoded username and password when first message received`() {
        mechanism.handleAuthenticationEvent(ircClient, null)

        val captor = argumentCaptor<String>()
        verify(ircClient).send(eq("AUTHENTICATE"), captor.capture())
        val data = String(captor.firstValue.fromBase64()).split('\u0000')
        assertEquals("acidB", data[0])
        assertEquals("acidB", data[1])
        assertEquals("HackThePlan3t!", data[2])
    }

}