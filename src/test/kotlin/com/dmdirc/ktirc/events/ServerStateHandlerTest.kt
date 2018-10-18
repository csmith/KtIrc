package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.ServerState
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `ServerStateHandler updates features on features event`() = runBlocking {
        val features = ServerFeatureMap()
        features[ServerFeature.ChannelModes] = "abc"
        features[ServerFeature.WhoxSupport] = true

        handler.processEvent(ircClient, ServerFeaturesUpdated(TestConstants.time, features))

        assertEquals("abc", serverState.features[ServerFeature.ChannelModes])
        assertEquals(true, serverState.features[ServerFeature.WhoxSupport])
    }

}