package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.CapabilitiesNegotiationState
import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.sasl.SaslMechanism
import com.dmdirc.ktirc.sasl.fromBase64
import com.dmdirc.ktirc.sasl.toBase64
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CapabilitiesHandlerTest {

    private val saslMech1 = mockk<SaslMechanism> {
        every { priority } returns 1
        every { ircName } returns "mech1"
    }

    private val saslMech2 = mockk<SaslMechanism> {
        every { priority } returns 2
        every { ircName } returns "mech2"
    }

    private val saslMech3 = mockk<SaslMechanism> {
        every { priority } returns 3
        every { ircName } returns "mech3"
    }

    private val handler = CapabilitiesHandler()
    private val fakeServerState = ServerState("", "", null)
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }

    @Test
    fun `adds new capabilities to the state`() {
        handler.processEvent(ircClient, ServerCapabilitiesReceived(EventMetadata(TestConstants.time), hashMapOf(
                Capability.EchoMessages.names[0] to "",
                Capability.HostsInNamesReply.names[0] to "123"
        )))

        assertEquals(2, fakeServerState.capabilities.advertisedCapabilities.size)
        assertEquals("", fakeServerState.capabilities.advertisedCapabilities[Capability.EchoMessages.names[0]])
        assertEquals("123", fakeServerState.capabilities.advertisedCapabilities[Capability.HostsInNamesReply.names[0]])
    }

    @Test
    fun `updates negotiation state when capabilities finished`() {
        fakeServerState.capabilities.advertisedCapabilities[Capability.EchoMessages.names[0]] = ""

        handler.processEvent(ircClient, ServerCapabilitiesFinished(EventMetadata(TestConstants.time)))

        assertEquals(CapabilitiesNegotiationState.AWAITING_ACK, fakeServerState.capabilities.negotiationState)
    }

    @Test
    fun `sends REQ when capabilities received`() {
        fakeServerState.capabilities.advertisedCapabilities[Capability.EchoMessages.names[0]] = ""
        fakeServerState.capabilities.advertisedCapabilities[Capability.AccountChangeMessages.names[0]] = ""

        handler.processEvent(ircClient, ServerCapabilitiesFinished(EventMetadata(TestConstants.time)))

        verify {
            ircClient.send(eq("CAP"), eq("REQ"), or(eq("echo-message account-notify"), eq("account-notify echo-message")))
        }
    }

    @Test
    fun `sends END when blank capabilities received`() {
        handler.processEvent(ircClient, ServerCapabilitiesFinished(EventMetadata(TestConstants.time)))

        verify {
            ircClient.send("CAP", "END")
        }
    }

    @Test
    fun `updates negotiation when blank capabilities received`() {
        handler.processEvent(ircClient, ServerCapabilitiesFinished(EventMetadata(TestConstants.time)))

        assertEquals(CapabilitiesNegotiationState.FINISHED, fakeServerState.capabilities.negotiationState)
    }

    @Test
    fun `sends END when capabilities acknowledged and no enabled mechanisms`() {
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.EchoMessages.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        verify {
            ircClient.send("CAP", "END")
        }
    }

    @Test
    fun `sends END when capabilities acknowledged and no sasl state`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.EchoMessages.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        verify {
            ircClient.send("CAP", "END")
        }
    }

    @Test
    fun `sends END when capabilities acknowledged and no shared mechanism`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        fakeServerState.capabilities.advertisedCapabilities[Capability.SaslAuthentication.names[0]] = "fake1,fake2"
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.SaslAuthentication.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        verify {
            ircClient.send("CAP", "END")
        }
    }

    @Test
    fun `sets current SASL mechanism when capabilities acknowledged with shared mechanism`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        fakeServerState.capabilities.advertisedCapabilities[Capability.SaslAuthentication.names[0]] = "mech1,fake2"
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.SaslAuthentication.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        assertSame(saslMech1, fakeServerState.sasl.currentMechanism)
    }

    @Test
    fun `sets current SASL mechanism when capabilities acknowledged with no declared mechanisms`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        fakeServerState.capabilities.advertisedCapabilities[Capability.SaslAuthentication.names[0]] = ""
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.SaslAuthentication.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        assertSame(saslMech3, fakeServerState.sasl.currentMechanism)
    }

    @Test
    fun `sends authenticate when capabilities acknowledged with shared mechanism`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        fakeServerState.capabilities.advertisedCapabilities[Capability.SaslAuthentication.names[0]] = "mech1,fake2"
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.SaslAuthentication.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        verify {
            ircClient.send("AUTHENTICATE", "mech1")
        }
    }

    @Test
    fun `sends authenticate when capabilities acknowledged with no declared mechanisms`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        fakeServerState.capabilities.advertisedCapabilities[Capability.SaslAuthentication.names[0]] = ""
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.SaslAuthentication.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        verify {
            ircClient.send("AUTHENTICATE", "mech3")
        }
    }

    @Test
    fun `updates negotiation state when capabilities acknowledged with shared mechanism`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        fakeServerState.capabilities.advertisedCapabilities[Capability.SaslAuthentication.names[0]] = "mech1,fake2"
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.SaslAuthentication.names[0] to "",
                Capability.HostsInNamesReply.names[0] to ""
        )))

        assertEquals(CapabilitiesNegotiationState.AUTHENTICATING, fakeServerState.capabilities.negotiationState)
    }

    @Test
    fun `updates negotiation state when capabilities acknowledged`() {
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.EchoMessages.names[0] to "",
                Capability.HostsInNamesReply.names[0] to "123"
        )))

        assertEquals(CapabilitiesNegotiationState.FINISHED, fakeServerState.capabilities.negotiationState)
    }

    @Test
    fun `stores enabled caps when capabilities acknowledged`() {
        handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), hashMapOf(
                Capability.EchoMessages.names[0] to "",
                Capability.HostsInNamesReply.names[0] to "123"
        )))

        assertEquals(2, fakeServerState.capabilities.enabledCapabilities.size)
        assertEquals("", fakeServerState.capabilities.enabledCapabilities[Capability.EchoMessages])
        assertEquals("123", fakeServerState.capabilities.enabledCapabilities[Capability.HostsInNamesReply])
    }

    @Test
    fun `aborts authentication attempt if not expecting one`() {
        fakeServerState.sasl.currentMechanism = null
        handler.processEvent(ircClient, AuthenticationMessage(EventMetadata(TestConstants.time), "+"))

        verify {
            ircClient.send("AUTHENTICATE", "*")
        }
    }

    @Test
    fun `passes authentication message to mechanism if in auth process`() {
        fakeServerState.sasl.currentMechanism = saslMech1

        val argument = "ABC"
        handler.processEvent(ircClient, AuthenticationMessage(EventMetadata(TestConstants.time), argument))

        verify {
            saslMech1.handleAuthenticationEvent(ircClient, argument.fromBase64())
        }
    }

    @Test
    fun `stores partial authentication message if it's 400 bytes long`() {
        fakeServerState.sasl.currentMechanism = saslMech1

        val argument = "A".repeat(400)
        handler.processEvent(ircClient, AuthenticationMessage(EventMetadata(TestConstants.time), argument))

        assertEquals(argument, fakeServerState.sasl.saslBuffer)
        verify(inverse = true) {
            saslMech1.handleAuthenticationEvent(any(), any())
        }
    }

    @Test
    fun `appends authentication messages if it's 400 bytes long and data already exists`() {
        fakeServerState.sasl.currentMechanism = saslMech1

        fakeServerState.sasl.saslBuffer = "A".repeat(400)
        handler.processEvent(ircClient, AuthenticationMessage(EventMetadata(TestConstants.time), "B".repeat(400)))

        assertEquals("A".repeat(400) + "B".repeat(400), fakeServerState.sasl.saslBuffer)
        verify(inverse = true) {
            saslMech1.handleAuthenticationEvent(any(), any())
        }
    }

    @Test
    fun `reconstructs partial authentication message to mechanism if data stored and partial received`() {
        fakeServerState.sasl.currentMechanism = saslMech1
        fakeServerState.sasl.saslBuffer = "A".repeat(400)

        val slot = slot<ByteArray>()
        every { saslMech1.handleAuthenticationEvent(eq(ircClient), capture(slot)) } just Runs

        val argument = "ABCD"
        handler.processEvent(ircClient, AuthenticationMessage(EventMetadata(TestConstants.time), argument))

        assertEquals("A".repeat(400) + "ABCD", slot.captured.toBase64())
    }

    @Test
    fun `reconstructs partial authentication message to mechanism if data stored and null received`() {
        fakeServerState.sasl.currentMechanism = saslMech1
        fakeServerState.sasl.saslBuffer = "A".repeat(400)

        val slot = slot<ByteArray>()
        every { saslMech1.handleAuthenticationEvent(eq(ircClient), capture(slot)) } just Runs

        handler.processEvent(ircClient, AuthenticationMessage(EventMetadata(TestConstants.time), null))

        assertEquals("A".repeat(400), slot.captured.toBase64())
    }

    @Test
    fun `sends END when SASL auth finished`() {
        handler.processEvent(ircClient, SaslFinished(EventMetadata(TestConstants.time), true))

        verify {
            ircClient.send("CAP", "END")
        }
    }

    @Test
    fun `sets negotiation state when SASL auth finished`() {
        handler.processEvent(ircClient, SaslFinished(EventMetadata(TestConstants.time), true))

        assertEquals(CapabilitiesNegotiationState.FINISHED, fakeServerState.capabilities.negotiationState)
    }

    @Test
    fun `resets SASL state when SASL auth finished`() {
        with (fakeServerState.sasl) {
            currentMechanism = saslMech1
            saslBuffer = "HackThePlanet"
            mechanismState = "root@thegibson"
        }

        handler.processEvent(ircClient, SaslFinished(EventMetadata(TestConstants.time), true))

        with (fakeServerState.sasl) {
            assertNull(currentMechanism)
            assertEquals("", saslBuffer)
            assertNull(mechanismState)
        }
    }

    @Test
    fun `sends a new authenticate request when sasl mechanism rejected and new one is acceptable`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, SaslMechanismNotAvailableError(EventMetadata(TestConstants.time), listOf("mech1", "fake2")))

        verify {
            ircClient.send("AUTHENTICATE", "mech1")
        }
    }

    @Test
    fun `sends cap end when sasl mechanism rejected and no new one is acceptable`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, SaslMechanismNotAvailableError(EventMetadata(TestConstants.time), listOf("fake1", "fake2")))

        verify {
            ircClient.send("CAP", "END")
        }
    }

    @Test
    fun `sets negotiation state when sasl mechanism rejected and no new one is acceptable`() {
        fakeServerState.sasl.mechanisms.addAll(listOf(saslMech1, saslMech2, saslMech3))
        handler.processEvent(ircClient, SaslMechanismNotAvailableError(EventMetadata(TestConstants.time), listOf("fake1", "fake2")))

        assertEquals(CapabilitiesNegotiationState.FINISHED, fakeServerState.capabilities.negotiationState)
    }

}
