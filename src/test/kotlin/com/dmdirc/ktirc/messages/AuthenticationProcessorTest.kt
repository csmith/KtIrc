package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.AuthenticationMessage
import com.dmdirc.ktirc.events.SaslFinished
import com.dmdirc.ktirc.events.SaslMechanismNotAvailableError
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AuthenticationProcessorTest {

    private var processor = AuthenticationProcessor()

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises authentication message with null argument if no params specified`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "AUTHENTICATE", emptyList()))

        assertEquals(1, events.size)
        val event = events[0] as AuthenticationMessage
        assertEquals(TestConstants.time, event.time)
        assertNull(event.argument)
    }

    @Test
    fun `raises authentication message with null argument if + specified`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "AUTHENTICATE", params("+")))

        assertEquals(1, events.size)
        val event = events[0] as AuthenticationMessage
        assertEquals(TestConstants.time, event.time)
        assertNull(event.argument)
    }

    @Test
    fun `raises authentication message with argument`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "AUTHENTICATE", params("HackThePlanet")))

        assertEquals(1, events.size)
        val event = events[0] as AuthenticationMessage
        assertEquals(TestConstants.time, event.time)
        assertEquals("HackThePlanet", event.argument)
    }

    @Test
    fun `raises sasl finished on success`() {
        val events = processor.process(IrcMessage(emptyMap(), ":the.gibson".toByteArray(), "903", params("*", "SASL authentication successful")))

        assertEquals(1, events.size)
        val event = events[0] as SaslFinished
        assertEquals(TestConstants.time, event.time)
        assertTrue(event.success)
    }

    @Test
    fun `raises sasl finished on generic failure`() {
        val events = processor.process(IrcMessage(emptyMap(), ":the.gibson".toByteArray(), "904", params("*", "SASL authentication failed")))

        assertEquals(1, events.size)
        val event = events[0] as SaslFinished
        assertEquals(TestConstants.time, event.time)
        assertFalse(event.success)
    }

    @Test
    fun `raises not available error on mechs numeric`() {
        val events = processor.process(IrcMessage(emptyMap(), ":the.gibson".toByteArray(), "908", params("*", "PLAIN,EXTERNAL,MAGIC", "are supported by this server")))

        assertEquals(1, events.size)
        val event = events[0] as SaslMechanismNotAvailableError
        assertEquals(TestConstants.time, event.time)
        assertEquals(listOf("PLAIN", "EXTERNAL", "MAGIC"), event.mechanisms)
    }

    @Test
    fun `raises empty not available error on malformed mechs numeric`() {
        val events = processor.process(IrcMessage(emptyMap(), ":the.gibson".toByteArray(), "908", params("*")))

        assertEquals(1, events.size)
        val event = events[0] as SaslMechanismNotAvailableError
        assertEquals(TestConstants.time, event.time)
        assertTrue(event.mechanisms.isEmpty())
    }

}
