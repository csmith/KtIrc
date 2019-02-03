package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JoinProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
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

    @Test
    fun `JoinProcessor adds real name and account from extended join`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "JOIN", listOf("#crashandburn", "acidBurn", "Libby").map { it.toByteArray() }))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost", account = "acidBurn", realName = "Libby"), events[0].user)
        assertEquals("#crashandburn", events[0].channel)
    }

    @Test
    fun `JoinProcessor ignores account if the user is not authed`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "JOIN", listOf("#crashandburn", "*", "Libby").map { it.toByteArray() }))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost", realName = "Libby"), events[0].user)
        assertEquals("#crashandburn", events[0].channel)
    }

}