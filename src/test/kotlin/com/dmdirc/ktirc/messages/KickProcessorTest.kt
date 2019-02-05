package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KickProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises kick event without message`() {
        val events = KickProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "KICK", params("#crashandburn", "zeroCool")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#crashandburn", events[0].channel)
        assertEquals("zeroCool", events[0].victim)
        assertEquals("", events[0].reason)
    }

    @Test
    fun `raises kick event with message`() {
        val events = KickProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "KICK", params("#crashandburn", "zeroCool", "Hack the planet!")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#crashandburn", events[0].channel)
        assertEquals("zeroCool", events[0].victim)
        assertEquals("Hack the planet!", events[0].reason)
    }

    @Test
    fun `does nothing if prefix missing`() {
        val events = KickProcessor().process(
                IrcMessage(emptyMap(), null, "KICK", params("#crashandburn", "zeroCool")))
        assertEquals(0, events.size)
    }

}
