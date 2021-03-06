package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.messages.tagMap
import com.dmdirc.ktirc.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventUtilsTest {

    private val fakeServerState = ServerState("", "")
    private val fakeLocalUser = User("")
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
        every { caseMapping } returns CaseMapping.Ascii
        every { localUser } returns fakeLocalUser
    }

    @BeforeEach
    fun setUp() {
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
    }

    @Test
    fun `ChannelNamesReceived#toModesAndUsers parses nicknames and mode prefixes`() {
        val event = ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf(
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
        val event = ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf(
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
        fakeLocalUser.nickname = "zeroCool"
        val message = MessageReceived(EventMetadata(TestConstants.time), User("acidBurn"), "Zerocool", "Hack the planet!")

        ircClient.reply(message, "OK")
        verify {
            ircClient.send(tagMap(), "PRIVMSG", "acidBurn", "OK")
        }
    }

    @Test
    fun `reply sends unprefixed response to user when message is in a channel`() {
        val message = MessageReceived(EventMetadata(TestConstants.time), User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.reply(message, "OK")
        verify {
            ircClient.send(tagMap(), "PRIVMSG", "#TheGibson", "OK")
        }
    }

    @Test
    fun `reply sends prefixed response to user when message is in a channel`() {
        val message = MessageReceived(EventMetadata(TestConstants.time), User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.reply(message, "OK", prefixWithNickname = true)
        verify {
            ircClient.send(tagMap(), "PRIVMSG", "#TheGibson", "acidBurn: OK")
        }
    }

    @Test
    fun `reply sends response with message ID to user when message is private`() {
        fakeLocalUser.nickname = "zeroCool"
        val message = MessageReceived(EventMetadata(TestConstants.time, messageId = "abc123"), User("acidBurn"), "Zerocool", "Hack the planet!")

        ircClient.reply(message, "OK")
        verify {
            ircClient.send(tagMap(MessageTag.Reply to "abc123"), "PRIVMSG", "acidBurn", "OK")
        }
    }

    @Test
    fun `reply sends unprefixed response with message ID to user when message is in a channel`() {
        val message = MessageReceived(EventMetadata(TestConstants.time, messageId = "abc123"), User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.reply(message, "OK")
        verify {
            ircClient.send(tagMap(MessageTag.Reply to "abc123"), "PRIVMSG", "#TheGibson", "OK")
        }
    }

    @Test
    fun `reply sends prefixed response with message ID to user when message is in a channel`() {
        val message = MessageReceived(EventMetadata(TestConstants.time, messageId = "abc123"), User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.reply(message, "OK", prefixWithNickname = true)
        verify {
            ircClient.send(tagMap(MessageTag.Reply to "abc123"), "PRIVMSG", "#TheGibson", "acidBurn: OK")
        }
    }


    @Test
    fun `react sends response to user when message is private`() {
        fakeLocalUser.nickname = "zeroCool"
        val message = MessageReceived(EventMetadata(TestConstants.time, messageId = "msgId"), User("acidBurn"), "Zerocool", "Hack the planet!")

        ircClient.react(message, ":P")
        verify {
            ircClient.send(tagMap(MessageTag.React to ":P", MessageTag.Reply to "msgId"), "TAGMSG", "acidBurn")
        }
    }

    @Test
    fun `react sends unprefixed response to user when message is in a channel`() {
        val message = MessageReceived(EventMetadata(TestConstants.time, messageId = "msgId"), User("acidBurn"), "#TheGibson", "Hack the planet!")

        ircClient.react(message, ":P")
        verify {
            ircClient.send(tagMap(MessageTag.React to ":P", MessageTag.Reply to "msgId"), "TAGMSG", "#TheGibson")
        }
    }

}
