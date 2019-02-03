package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
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
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", listOf("#crashandburn".toByteArray(), "hack the planet!".toByteArray())))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#crashandburn", events[0].target)
        assertEquals("hack the planet!", events[0].message)
    }

    @Test
    fun `PrivsgProcessor does nothing if prefix missing`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), null, "PRIVMSG", listOf("#crashandburn".toByteArray(), "hack the planet!".toByteArray())))
        assertEquals(0, events.size)
    }
}