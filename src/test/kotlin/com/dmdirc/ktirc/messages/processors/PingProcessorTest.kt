package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.PingReceived
import com.dmdirc.ktirc.events.PongReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PingProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises ping event with nonce`() {
        val events = PingProcessor().process(
                IrcMessage(emptyMap(), null, "PING", params("12345")))
        assertEquals(1, events.size)

        val event = events[0] as PingReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("12345", String(event.nonce))
    }

    @Test
    fun `raises pong event with nonce`() {
        val events = PingProcessor().process(
                IrcMessage(emptyMap(), null, "PONG", params("12345")))
        assertEquals(1, events.size)

        val event = events[0] as PongReceived
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("12345", String(event.nonce))
    }

}