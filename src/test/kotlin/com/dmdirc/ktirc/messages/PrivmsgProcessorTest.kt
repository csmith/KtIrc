package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.ActionReceived
import com.dmdirc.ktirc.events.CtcpReceived
import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.MessageTag
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PrivmsgProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `PrivsgProcessor raises message received event`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as MessageReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hack the planet!", event.message)
        assertNull(event.messageId)
    }

    @Test
    fun `PrivsgProcessor raises message received event with message ID`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(mapOf(MessageTag.MessageId to "abc123"), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as MessageReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hack the planet!", event.message)
        assertEquals("abc123", event.messageId)
    }

    @Test
    fun `PrivsgProcessor raises action received event with content`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "\u0001ACTION hacks the planet\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as ActionReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hacks the planet", event.action)
        assertNull(event.messageId)
    }

    @Test
    fun `PrivsgProcessor raises action received event with message ID`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(mapOf(MessageTag.MessageId to "abc123"), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "\u0001ACTION hacks the planet\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as ActionReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hacks the planet", event.action)
        assertEquals("abc123", event.messageId)
    }

    @Test
    fun `PrivsgProcessor raises action received event without content`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "\u0001ACTION\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as ActionReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("", event.action)
        assertNull(event.messageId)
    }

    @Test
    fun `PrivsgProcessor raises action received event with lowercase type`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "\u0001action hacks the planet\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as ActionReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hacks the planet", event.action)
        assertNull(event.messageId)
    }

    @Test
    fun `PrivsgProcessor raises CTCP received event with content`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "\u0001PING 12345\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as CtcpReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("PING", event.type)
        assertEquals("12345", event.content)
    }

    @Test
    fun `PrivsgProcessor raises CTCP received event without content`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", params("#crashandburn", "\u0001PING\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as CtcpReceived
        assertEquals(TestConstants.time, event.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("PING", event.type)
        assertEquals("", event.content)
    }

    @Test
    fun `PrivsgProcessor does nothing if prefix missing`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), null, "PRIVMSG", params("#crashandburn", "hack the planet!")))
        assertEquals(0, events.size)
    }
}