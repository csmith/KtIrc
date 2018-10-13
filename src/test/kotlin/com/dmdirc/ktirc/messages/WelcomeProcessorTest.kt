package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.state.ServerState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class WelcomeProcessorTest {

    private val state = mock<ServerState>()
    private val processor = WelcomeProcessor(state)

    @Test
    fun `WelcomeProcessor can handle 001s`() {
        assertTrue(processor.commands.contains("001")) { "WelcomeProcessor should handle 001 messages" }
    }

    @Test
    fun `WelcomeProcessor parses local nickname`() {
        processor.process(IrcMessage(null, ":thegibson.com".toByteArray(), "001", listOf(
                "acidBurn".toByteArray(),
                "Welcome to the Internet Relay Network, acidBurn!burn@hacktheplanet.com".toByteArray())))
        verify(state).localNickname = "acidBurn"
    }

    @Test
    fun `WelcomeProcessor returns server connected event`() {
        val events = processor.process(IrcMessage(null, ":thegibson.com".toByteArray(), "001", listOf(
                "acidBurn".toByteArray(),
                "Welcome to the Internet Relay Network, acidBurn!burn@hacktheplanet.com".toByteArray())))
        assertEquals(listOf<IrcEvent>(ServerConnected), events)
    }

}