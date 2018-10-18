package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.model.IrcMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class WelcomeProcessorTest {

    private val processor = WelcomeProcessor()

    @Test
    fun `WelcomeProcessor can handle 001s`() {
        assertTrue(processor.commands.contains("001")) { "WelcomeProcessor should handle 001 messages" }
    }

    @Test
    fun `WelcomeProcessor returns server welcome event`() {
        val events = processor.process(IrcMessage(emptyMap(), ":thegibson.com".toByteArray(), "001", listOf(
                "acidBurn".toByteArray(),
                "Welcome to the Internet Relay Network, acidBurn!burn@hacktheplanet.com".toByteArray())))
        assertEquals(listOf<IrcEvent>(ServerWelcome("acidBurn")), events)
    }

}