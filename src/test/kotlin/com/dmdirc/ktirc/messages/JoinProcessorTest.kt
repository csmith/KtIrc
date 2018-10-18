package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JoinProcessorTest {

    @BeforeEach
    fun setUp() {
        IrcMessage.currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `JoinProcessor raises join event`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "JOIN", listOf("#crashandburn".toByteArray())))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#crashandburn", events[0].channel)
    }

    @Test
    fun `JoinProcessor does nothing if prefix missing`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), null, "JOIN", listOf("#crashandburn".toByteArray())))
        assertEquals(0, events.size)
    }

}