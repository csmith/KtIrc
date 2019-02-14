package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerStateHandlerTest {

    private val serverState = ServerState("", "")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    private val handler = ServerStateHandler()

    @Test
    fun `sets local nickname on welcome event`() {
        handler.processEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))
        assertEquals("acidBurn", serverState.localNickname)
    }

    @Test
    fun `sets server name on welcome event`() {
        handler.processEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))
        assertEquals("the.gibson", serverState.serverName)
    }

    @Test
    fun `sets receivedWelcome on welcome event`() {
        handler.processEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))
        assertTrue(serverState.receivedWelcome)
    }

    @Test
    fun `sets state to connecting on event`() {
        handler.processEvent(ircClient, ServerConnecting(TestConstants.time))
        assertEquals(ServerStatus.Connecting, serverState.status)
    }

    @Test
    fun `sets state to disconnected on event`() {
        serverState.status = ServerStatus.Ready
        handler.processEvent(ircClient, ServerDisconnected(TestConstants.time))
        assertEquals(ServerStatus.Disconnected, serverState.status)
    }

    @Test
    fun `sets state to negotiating on connected`() {
        handler.processEvent(ircClient, ServerConnected(TestConstants.time))
        assertEquals(ServerStatus.Negotiating, serverState.status)
    }

    @Test
    fun `sets state to ready on ServerReady`() {
        handler.processEvent(ircClient, ServerReady(TestConstants.time))
        assertEquals(ServerStatus.Ready, serverState.status)
    }

    @Test
    fun `updates features on features event`() {
        val features = ServerFeatureMap()
        features[ServerFeature.ChannelModes] = arrayOf("abc", "def")
        features[ServerFeature.WhoxSupport] = true

        handler.processEvent(ircClient, ServerFeaturesUpdated(TestConstants.time, features))

        assertArrayEquals(arrayOf("abc", "def"), serverState.features[ServerFeature.ChannelModes])
        assertEquals(true, serverState.features[ServerFeature.WhoxSupport])
    }

}