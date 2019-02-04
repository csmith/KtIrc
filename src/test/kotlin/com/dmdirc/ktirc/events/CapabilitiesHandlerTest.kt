package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.CapabilitiesNegotiationState
import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.ServerState
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CapabilitiesHandlerTest {

    private val handler = CapabilitiesHandler()
    private val serverState = ServerState("", "")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    @Test
    fun `CapabilitiesHandler adds new capabilities to the state`() {
        runBlocking {
            handler.processEvent(ircClient, ServerCapabilitiesReceived(TestConstants.time, hashMapOf(
                    Capability.EchoMessages to "",
                    Capability.HostsInNamesReply to "123"
            )))

            assertEquals(2, serverState.capabilities.advertisedCapabilities.size)
            assertEquals("", serverState.capabilities.advertisedCapabilities[Capability.EchoMessages])
            assertEquals("123", serverState.capabilities.advertisedCapabilities[Capability.HostsInNamesReply])
        }
    }

    @Test
    fun `CapabilitiesHandler updates negotiation state when capabilities finished`() {
        runBlocking {
            serverState.capabilities.advertisedCapabilities[Capability.EchoMessages] = ""

            handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

            assertEquals(CapabilitiesNegotiationState.AWAITING_ACK, serverState.capabilities.negotiationState)
        }
    }

    @Test
    fun `CapabilitiesHandler sends REQ when capabilities received`() {
        runBlocking {
            serverState.capabilities.advertisedCapabilities[Capability.EchoMessages] = ""
            serverState.capabilities.advertisedCapabilities[Capability.AccountChangeMessages] = ""

            handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

            verify(ircClient).send(argThat { equals("CAP REQ :echo-message account-notify") || equals("CAP REQ :account-notify echo-message") })
        }
    }

    @Test
    fun `CapabilitiesHandler sends END when blank capabilities received`() {
        runBlocking {
            handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

            verify(ircClient).send("CAP END")
        }
    }

    @Test
    fun `CapabilitiesHandler updates negotiation when blank capabilities received`() {
        runBlocking {
            handler.processEvent(ircClient, ServerCapabilitiesFinished(TestConstants.time))

            assertEquals(CapabilitiesNegotiationState.FINISHED, serverState.capabilities.negotiationState)
        }
    }

    @Test
    fun `CapabilitiesHandler sends END when capabilities acknowledged`() {
        runBlocking {
            handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                    Capability.EchoMessages to "",
                    Capability.HostsInNamesReply to "123"
            )))

            verify(ircClient).send("CAP END")
        }
    }

    @Test
    fun `CapabilitiesHandler updates negotiation state when capabilities acknowledged`() {
        runBlocking {
            handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                    Capability.EchoMessages to "",
                    Capability.HostsInNamesReply to "123"
            )))

            assertEquals(CapabilitiesNegotiationState.FINISHED, serverState.capabilities.negotiationState)
        }
    }

    @Test
    fun `CapabilitiesHandler stores enabled caps when capabilities acknowledged`() {
        runBlocking {
            handler.processEvent(ircClient, ServerCapabilitiesAcknowledged(TestConstants.time, hashMapOf(
                    Capability.EchoMessages to "",
                    Capability.HostsInNamesReply to "123"
            )))

            assertEquals(2, serverState.capabilities.enabledCapabilities.size)
            assertEquals("", serverState.capabilities.enabledCapabilities[Capability.EchoMessages])
            assertEquals("123", serverState.capabilities.enabledCapabilities[Capability.HostsInNamesReply])
        }
    }

}