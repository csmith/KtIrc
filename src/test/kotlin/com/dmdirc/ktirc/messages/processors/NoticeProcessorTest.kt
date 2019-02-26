package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.CtcpReplyReceived
import com.dmdirc.ktirc.events.NoticeReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class NoticeProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises notice received event`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "NOTICE", params("#crashandburn", "hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as NoticeReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hack the planet!", event.message)
    }

    @Test
    fun `raises notice received event with no prefix`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), null, "NOTICE", params("#crashandburn", "hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as NoticeReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("*"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("hack the planet!", event.message)
    }

    @Test
    fun `raises CTCP reply received event with content`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "NOTICE", params("#crashandburn", "\u0001PING 12345\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as CtcpReplyReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("PING", event.type)
        assertEquals("12345", event.content)
    }

    @Test
    fun `raises CTCP reply received event with content when containing unicode chars`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "NOTICE", params("#crashandburn", "\u0001PING 👩‍💻\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as CtcpReplyReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("PING", event.type)
        assertEquals("👩‍💻", event.content)
    }

    @Test
    fun `raises CTCP reply received event without content`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "NOTICE", params("#crashandburn", "\u0001PING\u0001")))
        assertEquals(1, events.size)

        val event = events[0] as CtcpReplyReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
        assertEquals("PING", event.type)
        assertEquals("", event.content)
    }

    @Test
    fun `does nothing if prefix missing for CTCP replies`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), null, "NOTICE", params("#crashandburn", "\u0001BORK\u0001")))
        assertEquals(0, events.size)
    }

    @Test
    fun `does nothing if target missing for CTCP replies`() {
        val events = NoticeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "NOTICE", params("\u0001BORK\u0001")))
        assertEquals(0, events.size)
    }
}
