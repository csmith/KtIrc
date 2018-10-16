package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JoinProcessorTest {

    @Test
    fun `JoinProcessor raises join event`() {
        val events = JoinProcessor().process(
                IrcMessage(null, "acidburn!libby@root.localhost".toByteArray(), "JOIN", listOf("#crashandburn".toByteArray())))
        assertEquals(1, events.size)
        assertEquals(ChannelJoined(User("acidburn", "libby", "root.localhost"), "#crashandburn"), events[0])
    }

    @Test
    fun `JoinProcessor does nothing if prefix missing`() {
        val events = JoinProcessor().process(
                IrcMessage(null, null, "JOIN", listOf("#crashandburn".toByteArray())))
        assertEquals(0, events.size)
    }

}