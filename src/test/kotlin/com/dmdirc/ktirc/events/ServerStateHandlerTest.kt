package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ServerStateHandlerTest {

    private val serverState = ServerState("")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    private val handler = ServerStateHandler()

    @Test
    fun `ServerStateHandler sets local nickname on welcome event`() = runBlocking {
        handler.processEvent(ircClient, ServerWelcome(TestConstants.time, "acidBurn"))
        assertEquals("acidBurn", serverState.localNickname)
    }

    @Test
    fun `ServerStateHandler sets receivedWelcome on welcome event`() = runBlocking {
        handler.processEvent(ircClient, ServerWelcome(TestConstants.time, "acidBurn"))
        assertTrue(serverState.receivedWelcome)
    }

    @Test
    fun `ServerStateHandler sets state to negotiating on connected`() = runBlocking {
        handler.processEvent(ircClient, ServerConnected(TestConstants.time))
        assertEquals(ServerStatus.Negotiating, serverState.status)
    }

    @Test
    fun `ServerStateHandler sets server state to ready on receiving post-005 line`() = runBlocking {
        ircClient.serverState.status = ServerStatus.Negotiating

        listOf(
                ServerWelcome(TestConstants.time, "acidBurn"),
                PingReceived(TestConstants.time, "1234".toByteArray()),
                ServerCapabilitiesReceived(TestConstants.time, emptyMap()),
                ServerCapabilitiesAcknowledged(TestConstants.time, emptyMap()),
                ServerCapabilitiesFinished(TestConstants.time),
                MessageReceived(TestConstants.time, User("zeroCool"), "acidBurn", "Welcome!")
        ).forEach {
            assertEquals(ServerStatus.Negotiating, serverState.status)
            handler.processEvent(ircClient, it)
        }

        assertEquals(ServerStatus.Ready, serverState.status)
    }

    @Test
    fun `ServerStateHandler emits event on receiving post-005 line`() = runBlocking {
        ircClient.serverState.status = ServerStatus.Negotiating

        listOf(
                ServerWelcome(TestConstants.time, "acidBurn"),
                PingReceived(TestConstants.time, "1234".toByteArray()),
                ServerCapabilitiesReceived(TestConstants.time, emptyMap()),
                ServerCapabilitiesAcknowledged(TestConstants.time, emptyMap()),
                ServerCapabilitiesFinished(TestConstants.time)
        ).forEach {
            assertTrue(handler.processEvent(ircClient, it).isEmpty())
        }

        val events = handler.processEvent(ircClient, MessageReceived(TestConstants.time, User("zeroCool"), "acidBurn", "Welcome!"))
        assertEquals(1, events.size)
        assertTrue(events[0] is ServerReady)
    }

    @Test
    fun `ServerStateHandler updates features on features event`() = runBlocking {
        val features = ServerFeatureMap()
        features[ServerFeature.ChannelModes] = "abc"
        features[ServerFeature.WhoxSupport] = true

        handler.processEvent(ircClient, ServerFeaturesUpdated(TestConstants.time, features))

        assertEquals("abc", serverState.features[ServerFeature.ChannelModes])
        assertEquals(true, serverState.features[ServerFeature.WhoxSupport])
    }

}