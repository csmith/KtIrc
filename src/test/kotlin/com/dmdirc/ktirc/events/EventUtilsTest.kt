package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.ModePrefixMapping
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.ServerState
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventUtilsTest {

    private val serverState = ServerState("")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    @BeforeEach
    fun setUp() {
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
    }

    @Test
    fun `ChannelNamesReceived#toModesAndUsers parses nicknames and mode prefixes`() {
        val event = ChannelNamesReceived(TestConstants.time, "#thegibson", listOf(
                "@+acidBurn", "@zeroCool", "thePlague"
        ))

        val result = event.toModesAndUsers(ircClient)
        assertEquals(3, result.size)

        assertEquals("ov", result[0].first)
        assertEquals("acidBurn", result[0].second.nickname)

        assertEquals("o", result[1].first)
        assertEquals("zeroCool", result[1].second.nickname)

        assertEquals("", result[2].first)
        assertEquals("thePlague", result[2].second.nickname)
    }

    @Test
    fun `ChannelNamesReceived#toModesAndUsers parses extended joins with prefixes`() {
        val event = ChannelNamesReceived(TestConstants.time, "#thegibson", listOf(
                "@+acidBurn!libby@root.localhost", "zeroCool!dade@root.localhost"
        ))

        val result = event.toModesAndUsers(ircClient)
        assertEquals(2, result.size)

        assertEquals("ov", result[0].first)
        assertEquals("acidBurn", result[0].second.nickname)
        assertEquals("libby", result[0].second.ident)
        assertEquals("root.localhost", result[0].second.hostname)

        assertEquals("", result[1].first)
        assertEquals("zeroCool", result[1].second.nickname)
        assertEquals("dade", result[1].second.ident)
        assertEquals("root.localhost", result[1].second.hostname)
    }


}