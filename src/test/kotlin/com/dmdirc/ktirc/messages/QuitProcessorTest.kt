package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class QuitProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `QuitProcessor raises quit event without message`() {
        val events = QuitProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "QUIT", emptyList()))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("", events[0].reason)
    }

    @Test
    fun `QuitProcessor raises quit event with message`() {
        val events = QuitProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "QUIT", params("Hack the planet!")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("Hack the planet!", events[0].reason)
    }

    @Test
    fun `QuitProcessor does nothing if prefix missing`() {
        val events = QuitProcessor().process(
                IrcMessage(emptyMap(), null, "QUIT", params("Hack the planet!")))
        assertEquals(0, events.size)
    }

}