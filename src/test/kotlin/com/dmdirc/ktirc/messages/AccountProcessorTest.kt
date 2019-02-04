package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AccountProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `AccountProcessor raises account changed event with account name`() {
        val events = AccountProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "ACCOUNT", listOf("acidBurn".toByteArray())))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("acidBurn", events[0].newAccount)
    }

    @Test
    fun `AccountProcessor raises account changed event when account removed`() {
        val events = AccountProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "ACCOUNT", listOf("*".toByteArray())))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals(null, events[0].newAccount)
    }

    @Test
    fun `AccountProcessor does nothing if prefix missing`() {
        val events = AccountProcessor().process(
                IrcMessage(emptyMap(), null, "ACCOUNT", listOf("*".toByteArray())))
        assertEquals(0, events.size)
    }

}