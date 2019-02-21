package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus
import com.dmdirc.ktirc.model.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerReadyMutatorTest {

    private val messageEmitter = mockk<MessageEmitter>()
    private val fakeServerState = ServerState("", "")
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }

    private val mutator = ServerReadyMutator()


    @Test
    fun `emits event on receiving post-005 line`() {
        ircClient.serverState.receivedWelcome = true
        ircClient.serverState.status = ServerStatus.Negotiating

        listOf(
                ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"),
                PingReceived(EventMetadata(TestConstants.time), "1234".toByteArray()),
                ServerCapabilitiesReceived(EventMetadata(TestConstants.time), emptyMap()),
                ServerCapabilitiesAcknowledged(EventMetadata(TestConstants.time), emptyMap()),
                ServerCapabilitiesFinished(EventMetadata(TestConstants.time))
        ).forEach {
            assertEquals(1, mutator.mutateEvent(ircClient, messageEmitter, it).size)
        }

        val event = MessageReceived(EventMetadata(TestConstants.time), User("zeroCool"), "acidBurn", "Welcome!")
        val events = mutator.mutateEvent(ircClient, messageEmitter, event)
        assertEquals(2, events.size)
        assertSame(event, events[1])
        assertTrue(events[0] is ServerReady)
    }

}
