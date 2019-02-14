package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus
import com.dmdirc.ktirc.model.User
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerReadyMutatorTest {

    private val serverState = ServerState("", "")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    private val mutator = ServerReadyMutator()


    @Test
    fun `emits event on receiving post-005 line`() {
        ircClient.serverState.receivedWelcome = true
        ircClient.serverState.status = ServerStatus.Negotiating

        listOf(
                ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"),
                PingReceived(TestConstants.time, "1234".toByteArray()),
                ServerCapabilitiesReceived(TestConstants.time, emptyMap()),
                ServerCapabilitiesAcknowledged(TestConstants.time, emptyMap()),
                ServerCapabilitiesFinished(TestConstants.time)
        ).forEach {
            assertEquals(1, mutator.mutateEvent(ircClient, it).size)
        }

        val event = MessageReceived(TestConstants.time, User("zeroCool"), "acidBurn", "Welcome!")
        val events = mutator.mutateEvent(ircClient, event)
        assertEquals(2, events.size)
        assertSame(event, events[1])
        assertTrue(events[0] is ServerReady)
    }

}
