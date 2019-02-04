package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.ModePrefixMapping
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.User
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventUtilsTest {

    private val serverState = ServerState("", "")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
        on { caseMapping } doReturn CaseMapping.Ascii
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

    @Test
    fun `reply sends response to user when message is private`() {
        serverState.localNickname = "zeroCool"
        val message = MessageReceived(TestConstants.time, User("acidBurn"), "Zerocool", "Hack the planet!")

        ircClient.reply(message, "OK")
        verify(ircClient).send("PRIVMSG acidBurn :OK")
    }

    @Test
    fun `reply sends unprefixed response to user when message is in a channel`() {
        val message = MessageReceived(TestConstants.time, User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.reply(message, "OK")
        verify(ircClient).send("PRIVMSG #TheGibson :OK")
    }

    @Test
    fun `reply sends prefixed response to user when message is in a channel`() {
        val message = MessageReceived(TestConstants.time, User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.reply(message, "OK", prefixWithNickname = true)
        verify(ircClient).send("PRIVMSG #TheGibson :acidBurn: OK")
    }


}