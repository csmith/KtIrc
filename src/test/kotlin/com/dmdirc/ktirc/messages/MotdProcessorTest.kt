package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.MotdFinished
import com.dmdirc.ktirc.model.IrcMessage
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
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "422", listOf("MOTD missing".toByteArray())))
        assertEquals(1, events.size)

        val event = events[0] as MotdFinished
        assertEquals(TestConstants.time, events[0].time)
        assertTrue(event.missing)
    }

    @Test
    fun `MotdProcessor raises motdFinished when MOTD finishes normally`() {
        val events = MotdProcessor().process(
                IrcMessage(emptyMap(), "the.gibson".toByteArray(), "376", listOf("acidBurn".toByteArray(), "End of /MOTD command.".toByteArray())))
        assertEquals(1, events.size)

        val event = events[0] as MotdFinished
        assertEquals(TestConstants.time, events[0].time)
        assertFalse(event.missing)
    }

}