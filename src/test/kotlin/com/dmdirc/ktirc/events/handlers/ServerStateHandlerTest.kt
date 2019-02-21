package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerStateHandlerTest {

    private val fakeServerState = ServerState("", "")
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }

    private val handler = ServerStateHandler()

    @Test
    fun `sets local nickname on welcome event`() {
        handler.processEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))
        assertEquals("acidBurn", fakeServerState.localNickname)
    }

    @Test
    fun `sets server name on welcome event`() {
        handler.processEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))
        assertEquals("the.gibson", fakeServerState.serverName)
    }

    @Test
    fun `sets receivedWelcome on welcome event`() {
        handler.processEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))
        assertTrue(fakeServerState.receivedWelcome)
    }

    @Test
    fun `sets state to connecting on event`() {
        handler.processEvent(ircClient, ServerConnecting(EventMetadata(TestConstants.time)))
        assertEquals(ServerStatus.Connecting, fakeServerState.status)
    }

    @Test
    fun `sets state to disconnected on event`() {
        fakeServerState.status = ServerStatus.Ready
        handler.processEvent(ircClient, ServerDisconnected(EventMetadata(TestConstants.time)))
        assertEquals(ServerStatus.Disconnected, fakeServerState.status)
    }

    @Test
    fun `sets state to negotiating on connected`() {
        handler.processEvent(ircClient, ServerConnected(EventMetadata(TestConstants.time)))
        assertEquals(ServerStatus.Negotiating, fakeServerState.status)
    }

    @Test
    fun `sets state to ready on ServerReady`() {
        handler.processEvent(ircClient, ServerReady(EventMetadata(TestConstants.time)))
        assertEquals(ServerStatus.Ready, fakeServerState.status)
    }

    @Test
    fun `updates features on features event`() {
        val features = ServerFeatureMap()
        features[ServerFeature.ChannelModes] = arrayOf("abc", "def")
        features[ServerFeature.WhoxSupport] = true

        handler.processEvent(ircClient, ServerFeaturesUpdated(EventMetadata(TestConstants.time), features))

        assertArrayEquals(arrayOf("abc", "def"), fakeServerState.features[ServerFeature.ChannelModes])
        assertEquals(true, fakeServerState.features[ServerFeature.WhoxSupport])
    }

}