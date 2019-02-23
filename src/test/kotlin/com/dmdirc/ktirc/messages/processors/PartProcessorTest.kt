package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PartProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `PartProcessor raises part event without message`() {
        val events = PartProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PART", params("#crashandburn")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#crashandburn", events[0].target)
        assertEquals("", events[0].reason)
    }

    @Test
    fun `PartProcessor raises part event with message`() {
        val events = PartProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PART", params("#crashandburn", "Hack the planet!")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#crashandburn", events[0].target)
        assertEquals("Hack the planet!", events[0].reason)
    }

    @Test
    fun `PartProcessor does nothing if prefix missing`() {
        val events = PartProcessor().process(
                IrcMessage(emptyMap(), null, "PART", params("#crashandburn")))
        assertEquals(0, events.size)
    }

}