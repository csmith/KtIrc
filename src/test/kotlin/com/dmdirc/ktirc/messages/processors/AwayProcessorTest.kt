package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AwayProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises away changed event with reason`() {
        val events = AwayProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "AWAY", params("Hacking the planet")))
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("Hacking the planet", event.message)
    }

    @Test
    fun `raises away changed event with no reason`() {
        val events = AwayProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "AWAY", emptyList()))
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertNull(event.message)
    }

    @Test
    fun `does nothing on away if prefix missing`() {
        val events = AwayProcessor().process(IrcMessage(emptyMap(), null, "AWAY", params("*")))
        Assertions.assertEquals(0, events.size)
    }

}