package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.events.PingReceived
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class PingHandlerTest {

    private val ircClient = mockk<IrcClient>()

    private val handler = PingHandler()

    @Test
    fun `responses to pings with a pong`() {
        handler.processEvent(ircClient, PingReceived(EventMetadata(TestConstants.time), "the_plague".toByteArray()))
        verify {
            ircClient.send("PONG", "the_plague")
        }
    }

}
