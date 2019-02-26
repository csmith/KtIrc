package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ServerCapabilitiesAcknowledged
import com.dmdirc.ktirc.events.ServerCapabilitiesFinished
import com.dmdirc.ktirc.events.ServerCapabilitiesReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CapabilityProcessorTest {

    private val processor = CapabilityProcessor()

    @Test
    fun `does nothing for unknown subcommand`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "FOO")))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `does nothing for missing subcommand`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*")))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `raises ServerCapabilitiesReceived event given no capabilities`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "LS", "")))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(0, receivedEvent.capabilities.size)
    }


    @Test
    fun `raises ServerCapabilitiesReceived event with known capabilities`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "LS", "chghost extended-join invalid")))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertTrue("chghost" in receivedEvent.capabilities)
        assertTrue("extended-join" in receivedEvent.capabilities)
    }

    @Test
    fun `raises ServerCapabilitiesReceived event with values for capabilities`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "LS", "chghost=test123 extended-join=abc=def invalid")))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertEquals("test123", receivedEvent.capabilities["chghost"])
        assertEquals("abc=def", receivedEvent.capabilities["extended-join"])
    }

    @Test
    fun `overwrites earlier values with later ones for identical capabilities`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "LS", "chghost=test123 chghost chghost=456")))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(1, receivedEvent.capabilities.size)
        assertEquals("456", receivedEvent.capabilities["chghost"])
    }


    @Test
    fun `raises ServerCapabilitiesReceived event with known capabilities for multi-line responses`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "LS", "*", "chghost extended-join invalid")))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertTrue("chghost" in receivedEvent.capabilities)
        assertTrue("extended-join" in receivedEvent.capabilities)
    }

    @Test
    fun `raises ServerCapabilitiesFinished event for final LS responses`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "LS", "chghost extended-join invalid")))

        assertEquals(2, events.size)
        assertTrue(events[1] is ServerCapabilitiesFinished)
    }

    @Test
    fun `raises ServerCapabilitiesAcknowledged event`() {
        val events = processor.process(IrcMessage(emptyMap(), "the.gibson".toByteArray(), "CAP", params("*", "ACK", "chghost=test123 extended-join=abc=def invalid")))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesAcknowledged>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertEquals("test123", receivedEvent.capabilities["chghost"])
        assertEquals("abc=def", receivedEvent.capabilities["extended-join"])
    }

}