package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.MotdFinished
import com.dmdirc.ktirc.events.MotdLineReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MotdProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises MotdFinished when not found numeric received`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "422", params("MOTD missing")))
        assertEquals(1, events.size)

        val event = events[0] as MotdFinished
        assertEquals(TestConstants.time, events[0].metadata.time)
        assertTrue(event.missing)
    }

    @Test
    fun `raises MotdFinished when MOTD finishes normally`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "376", params("acidBurn", "End of /MOTD command.")))
        assertEquals(1, events.size)

        val event = events[0] as MotdFinished
        assertEquals(TestConstants.time, events[0].metadata.time)
        assertFalse(event.missing)
    }

    @Test
    fun `raises MotdLineReceived when start of MOTD received`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "375", params("acidBurn", "- the.gibson MOTD -")))
        assertEquals(1, events.size)

        val event = events[0] as MotdLineReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("- the.gibson MOTD -", event.line)
        assertTrue(event.first)
    }

    @Test
    fun `raises MotdLineReceived when line of MOTD received`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "372", params("acidBurn", "Hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as MotdLineReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("Hack the planet!", event.line)
        assertFalse(event.first)
    }

    @Test
    fun `raises nothing for unsupported events`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "NOTICE", params("acidBurn", "Hi")))
        assertEquals(0, events.size)
    }

}
