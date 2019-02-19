package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.events.PingReceived
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class PingHandlerTest {

    private val ircClient = mock<IrcClient>()

    private val handler = PingHandler()

    @Test
    fun `PingHandler responses to pings with a pong`() = runBlocking {
        handler.processEvent(ircClient, PingReceived(EventMetadata(TestConstants.time), "the_plague".toByteArray()))
        verify(ircClient).send("PONG", "the_plague")
    }

}
