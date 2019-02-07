package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.CapabilitiesNegotiationState
import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.sasl.SaslMechanism
import com.dmdirc.ktirc.sasl.fromBase64
import com.dmdirc.ktirc.sasl.toBase64
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CapabilitiesHandlerTest {

    private val saslMech1 = mock<SaslMechanism> {
        on { priority } doReturn 1
        on { ircName } doReturn "mech1"
    }

    private val saslMech2 = mock<SaslMechanism> {
        on { priority } doReturn 2
        on { ircName } doReturn "mech2"
    }

    private val saslMech3 = mock<SaslMechanism> {
        on { priority } doReturn 3
        on { ircName } doReturn "mech3"
    }

    private val handler = CapabilitiesHandler()
    private val serverState = ServerState("", "", null)
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    @Test
    fun `adds new capabilities to the state`() {
        handler.processEvent(ircClient, ServerCapabilitiesReceived(TestConstants.time, hashMapOf(
                Capability.EchoMessages to "",
                Capability.HostsInNamesReply to "123"
        )))

        assertEquals(2, serverState.capabilities.advertisedCapabilities.size)
        assertEquals("", serverState.capabilities.advertisedCapabilities[Capability.EchoMessages])
        assertEquals("123", serverState.capabilities.advertisedCapabilities[Capability.HostsInNamesReply])
    }

    @Test
    fun `updates negotiation state when capabilities finished`() {
        serverState.capabilities.advertisedCapabilities[Capability.EchoMessages] = ""

        handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

        assertEquals(CapabilitiesNegotiationState.AWAITING_ACK, serverState.capabilities.negotiationState)
    }

    @Test
    fun `sends REQ when capabilities received`() {
        serverState.capabilities.advertisedCapabilities[Capability.EchoMessages] = ""
        serverState.capabilities.advertisedCapabilities[Capability.AccountChangeMessages] = ""

        handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

        verify(ircClient).send(argThat { equals("CAP REQ :echo-message account-notify") || equals("CAP REQ :account-notify echo-message") })
    }

    @Test
    fun `sends END when blank capabilities received`() {
        handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

        verify(ircClient).send("CAP END")
    }

    @Test
    fun `updates negotiation when blank capabilities received`() {
        handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

        assertEquals(CapabilitiesNegotiationState.FINISHED, serverState.capabilities.negotiationState)
    }

    @Test
    fun `sends END when capabilities acknowledged and no profile`() {
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.EchoMessages to "",
                Capability.HostsInNamesReply to "123"
        )))

        verify(ircClient).send("CAP END")
    }

    @Test
    fun `sends END when capabilities acknowledged and no sasl state`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.EchoMessages to "",
                Capability.HostsInNamesReply to "123"
        )))

        verify(ircClient).send("CAP END")
    }

    @Test
    fun `sends END when capabilities acknowledged and no shared mechanism`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.SaslAuthentication to "fake1,fake2",
                Capability.HostsInNamesReply to "123"
        )))

        verify(ircClient).send("CAP END")
    }

    @Test
    fun `sends AUTHENTICATE when capabilities acknowledged with shared mechanism`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.SaslAuthentication to "mech1,fake2",
                Capability.HostsInNamesReply to "123"
        )))

        verify(ircClient).send("AUTHENTICATE mech1")
    }

    @Test
    fun `sets current SASL mechanism when capabilities acknowledged with shared mechanism`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.SaslAuthentication to "mech1,fake2",
                Capability.HostsInNamesReply to "123"
        )))

        assertSame(saslMech1, serverState.sasl.currentMechanism)
    }


    @Test
    fun `sends authenticate when capabilities acknowledged with shared mechanism`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.SaslAuthentication to "mech1,fake2",
                Capability.HostsInNamesReply to "123"
        )))

        verify(ircClient).send("AUTHENTICATE mech1")
    }

    @Test
    fun `updates negotiation state when capabilities acknowledged with shared mechanism`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.SaslAuthentication to "mech1,fake2",
                Capability.HostsInNamesReply to "123"
        )))

        assertEquals(CapabilitiesNegotiationState.AUTHENTICATING, serverState.capabilities.negotiationState)
    }

    @Test
    fun `updates negotiation state when capabilities acknowledged`() {
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.EchoMessages to "",
                Capability.HostsInNamesReply to "123"
        )))

        assertEquals(CapabilitiesNegotiationState.FINISHED, serverState.capabilities.negotiationState)
    }

    @Test
    fun `stores enabled caps when capabilities acknowledged`() {
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                Capability.EchoMessages to "",
                Capability.HostsInNamesReply to "123"
        )))

        assertEquals(2, serverState.capabilities.enabledCapabilities.size)
        assertEquals("", serverState.capabilities.enabledCapabilities[Capability.EchoMessages])
        assertEquals("123", serverState.capabilities.enabledCapabilities[Capability.HostsInNamesReply])
    }

    @Test
    fun `aborts authentication attempt if not expecting one`() {
        serverState.sasl.currentMechanism = null
        handler.processEvent(ircClient, AuthenticationMessage(TestConstants.time, "+"))

        verify(ircClient).send("AUTHENTICATE *")
    }

    @Test
    fun `passes authentication message to mechanism if in auth process`() {
        serverState.sasl.currentMechanism = saslMech1

        val argument = "ABC"
        handler.processEvent(ircClient, AuthenticationMessage(TestConstants.time, argument))

        verify(saslMech1).handleAuthenticationEvent(ircClient, argument.fromBase64())
    }

    @Test
    fun `stores partial authentication message if it's 400 bytes long`() {
        serverState.sasl.currentMechanism = saslMech1

        val argument = "A".repeat(400)
        handler.processEvent(ircClient, AuthenticationMessage(TestConstants.time, argument))

        assertEquals(argument, serverState.sasl.saslBuffer)
        verify(saslMech1, never()).handleAuthenticationEvent(any(), any())
    }

    @Test
    fun `appends authentication messages if it's 400 bytes long and data already exists`() {
        serverState.sasl.currentMechanism = saslMech1

        serverState.sasl.saslBuffer = "A".repeat(400)
        handler.processEvent(ircClient, AuthenticationMessage(TestConstants.time, "B".repeat(400)))

        assertEquals("A".repeat(400) + "B".repeat(400), serverState.sasl.saslBuffer)
        verify(saslMech1, never()).handleAuthenticationEvent(any(), any())
    }

    @Test
    fun `reconstructs partial authentication message to mechanism if data stored and partial received`() {
        serverState.sasl.currentMechanism = saslMech1

        serverState.sasl.saslBuffer = "A".repeat(400)

        val argument = "ABCD"
        handler.processEvent(ircClient, AuthenticationMessage(TestConstants.time, argument))

        val captor = argumentCaptor<ByteArray>()
        verify(saslMech1).handleAuthenticationEvent(same(ircClient), captor.capture())
        assertEquals("A".repeat(400) + "ABCD", captor.firstValue.toBase64())
    }

    @Test
    fun `reconstructs partial authentication message to mechanism if data stored and null received`() {
        serverState.sasl.currentMechanism = saslMech1

        serverState.sasl.saslBuffer = "A".repeat(400)

        handler.processEvent(ircClient, AuthenticationMessage(TestConstants.time, null))

        val captor = argumentCaptor<ByteArray>()
        verify(saslMech1).handleAuthenticationEvent(same(ircClient), captor.capture())
        assertEquals("A".repeat(400), captor.firstValue.toBase64())
    }

    @Test
    fun `sends END when SASL auth finished`() {
        handler.processEvent(ircClient, SaslFinished(TestConstants.time, true))

        verify(ircClient).send("CAP END")
    }

    @Test
    fun `sets negotiation state when SASL auth finished`() {
        handler.processEvent(ircClient, SaslFinished(TestConstants.time, true))

        assertEquals(CapabilitiesNegotiationState.FINISHED, serverState.capabilities.negotiationState)
    }

    @Test
    fun `resets SASL state when SASL auth finished`() {
        with (serverState.sasl) {
            currentMechanism = saslMech1
            saslBuffer = "HackThePlanet"
            mechanismState = "root@thegibson"
        }

        handler.processEvent(ircClient, SaslFinished(TestConstants.time, true))

        with (serverState.sasl) {
            assertNull(currentMechanism)
            assertEquals("", saslBuffer)
            assertNull(mechanismState)
        }
    }

    @Test
    fun `sends a new authenticate request when sasl mechanism rejected and new one is acceptable`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, SaslMechanismNotAvailableError(TestConstants.time, listOf("mech1", "fake2")))

        verify(ircClient).send("AUTHENTICATE mech1")
    }

    @Test
    fun `sends cap end when sasl mechanism rejected and no new one is acceptable`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, SaslMechanismNotAvailableError(TestConstants.time, listOf("fake1", "fake2")))

        verify(ircClient).send("CAP END")
    }

    @Test
    fun `sets negotiation state when sasl mechanism rejected and no new one is acceptable`() {
        serverState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, SaslMechanismNotAvailableError(TestConstants.time, listOf("fake1", "fake2")))

        assertEquals(CapabilitiesNegotiationState.FINISHED, serverState.capabilities.negotiationState)
    }

}
