package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.MotdFinished
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
    fun `MotdProcessor raises motdFinished when not found numeric received`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "422", params("MOTD missing")))
        assertEquals(1, events.size)

        val event = events[0] as MotdFinished
        assertEquals(TestConstants.time, events[0].time)
        assertTrue(event.missing)
    }

    @Test
    fun `MotdProcessor raises motdFinished when MOTD finishes normally`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "376", params("acidBurn", "End of /MOTD command.")))
        assertEquals(1, events.size)

        val event = events[0] as MotdFinished
        assertEquals(TestConstants.time, events[0].time)
        assertFalse(event.missing)
    }

}