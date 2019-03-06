package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ModeProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises event for user mode discovery with no params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "221", params("acidBurn", "+hax")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("acidBurn", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(0, events[0].arguments.size)
        assertTrue(events[0].discovered)
    }

    @Test
    fun `raises event for user mode discovery with params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "221", params("acidBurn", "+hax", "123", "467")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("acidBurn", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(2, events[0].arguments.size)
        assertEquals("123", events[0].arguments[0])
        assertEquals("467", events[0].arguments[1])
        assertTrue(events[0].discovered)
    }

    @Test
    fun `raises event for user mode change with no params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "MODE", params("acidBurn", "+hax")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("acidBurn", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(0, events[0].arguments.size)
        assertFalse(events[0].discovered)
    }

    @Test
    fun `raises event for user mode change with params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "MODE", params("acidBurn", "+hax", "123", "467")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("acidBurn", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(2, events[0].arguments.size)
        assertEquals("123", events[0].arguments[0])
        assertEquals("467", events[0].arguments[1])
        assertFalse(events[0].discovered)
    }

    @Test
    fun `raises event for channel mode discovery with no params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "324", params("acidBurn", "#thegibson", "+hax")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#thegibson", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(0, events[0].arguments.size)
        assertTrue(events[0].discovered)
    }

    @Test
    fun `raises event for channel mode discovery with no params and no prefix`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), null, "324", params("acidBurn", "#thegibson", "+hax")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("*"), events[0].user)
        assertEquals("#thegibson", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(0, events[0].arguments.size)
        assertTrue(events[0].discovered)
    }

    @Test
    fun `raises event for channel mode discovery with params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "324", params("acidBurn", "#thegibson", "+hax", "123", "467")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#thegibson", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(2, events[0].arguments.size)
        assertEquals("123", events[0].arguments[0])
        assertEquals("467", events[0].arguments[1])
        assertTrue(events[0].discovered)
    }

    @Test
    fun `raises event for channel mode change with no params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "MODE", params("#thegibson", "+hax")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#thegibson", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(0, events[0].arguments.size)
        assertFalse(events[0].discovered)
    }

    @Test
    fun `raises event for channel mode change with params`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "MODE", params("#thegibson", "+hax", "123", "467")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals(User("acidburn", "libby", "root.localhost"), events[0].user)
        assertEquals("#thegibson", events[0].target)
        assertEquals("+hax", events[0].modes)
        assertEquals(2, events[0].arguments.size)
        assertEquals("123", events[0].arguments[0])
        assertEquals("467", events[0].arguments[1])
        assertFalse(events[0].discovered)
    }

    @Test
    fun `ignores mode changes with a missing target`() {
        val events = ModeProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "MODE", params("+hax")))
        assertEquals(0, events.size)
    }

}
