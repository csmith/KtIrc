package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WelcomeProcessorTest {

    private val processor = WelcomeProcessor()

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `WelcomeProcessor can handle 001s`() {
        assertTrue(processor.commands.contains("001")) { "WelcomeProcessor should handle 001 messages" }
    }

    @Test
    fun `WelcomeProcessor returns server welcome event`() {
        val events = processor.process(IrcMessage(emptyMap(), ":thegibson.com".toByteArray(), "001", listOf(
                "acidBurn".toByteArray(),
                "Welcome to the Internet Relay Network, acidBurn!burn@hacktheplanet.com".toByteArray())))
        assertEquals(1, events.size)
        assertEquals(TestConstants.time, events[0].time)
        assertEquals("acidBurn", events[0].localNick)
    }

}