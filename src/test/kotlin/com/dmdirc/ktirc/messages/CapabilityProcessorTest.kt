package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ServerCapabilitiesAcknowledged
import com.dmdirc.ktirc.events.ServerCapabilitiesFinished
import com.dmdirc.ktirc.events.ServerCapabilitiesReceived
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.model.Capability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CapabilityProcessorTest {

    private val processor = CapabilityProcessor()

    @Test
    fun `CapabilityProcessor does nothing for unknown subcommand`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*".toByteArray(), "FOO".toByteArray())))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `CapabilityProcessor raises ServerCapabilitiesReceived event with known capabilities`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*", "LS", "chghost extended-join invalid").map { it.toByteArray() }))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertTrue(Capability.HostChangeMessages in receivedEvent.capabilities)
        assertTrue(Capability.AccountAndRealNameInJoinMessages in receivedEvent.capabilities)
    }

    @Test
    fun `CapabilityProcessor raises ServerCapabilitiesReceived event with values for capabilities`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*", "LS", "chghost=test123 extended-join=abc=def invalid").map { it.toByteArray() }))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertEquals("test123", receivedEvent.capabilities[Capability.HostChangeMessages])
        assertEquals("abc=def", receivedEvent.capabilities[Capability.AccountAndRealNameInJoinMessages])
    }

    @Test
    fun `CapabilityProcessor overwrites earlier values with later ones for identical capabilities`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*", "LS", "chghost=test123 chghost chghost=456").map { it.toByteArray() }))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(1, receivedEvent.capabilities.size)
        assertEquals("456", receivedEvent.capabilities[Capability.HostChangeMessages])
    }


    @Test
    fun `CapabilityProcessor raises ServerCapabilitiesReceived event with known capabilities for multi-line responses`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*", "LS", "*", "chghost extended-join invalid").map { it.toByteArray() }))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesReceived>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertTrue(Capability.HostChangeMessages in receivedEvent.capabilities)
        assertTrue(Capability.AccountAndRealNameInJoinMessages in receivedEvent.capabilities)
    }

    @Test
    fun `CapabilityProcessor raises ServerCapabilitiesFinished event for final LS responses`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*", "LS", "chghost extended-join invalid").map { it.toByteArray() }))

        assertEquals(2, events.size)
        assertTrue(events[1] is ServerCapabilitiesFinished)
    }

    @Test
    fun `CapabilityProcessor raises ServerCapabilitiesAcknowledged event`() {
        val events = processor.process(IrcMessage(null, "the.gibson".toByteArray(), "CAP", listOf("*", "ACK", "chghost=test123 extended-join=abc=def invalid").map { it.toByteArray() }))

        val receivedEvent = events.filterIsInstance<ServerCapabilitiesAcknowledged>()[0]
        assertEquals(2, receivedEvent.capabilities.size)
        assertEquals("test123", receivedEvent.capabilities[Capability.HostChangeMessages])
        assertEquals("abc=def", receivedEvent.capabilities[Capability.AccountAndRealNameInJoinMessages])
    }

}