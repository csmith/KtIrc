package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.ServerStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NickChangeRequiredMutatorTest {

    private val mutator = NickChangeRequiredMutator()
    private val fakeServerState = ServerState("acidBurn", "the.gibson")
    private val mockClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }
    private val mockEmitter = mockk<MessageEmitter>()

    @Test
    fun `returns other events before server ready`() {
        fakeServerState.status = ServerStatus.Negotiating
        val event = ServerConnected(EventMetadata(TestConstants.time))
        val events = mutator.mutateEvent(mockClient, mockEmitter, event)
        assertEquals(1, events.size)
        assertEquals(event, events[0])
    }

    @Test
    fun `returns other events after server ready`() {
        fakeServerState.status = ServerStatus.Ready
        val event = ServerConnected(EventMetadata(TestConstants.time))
        val events = mutator.mutateEvent(mockClient, mockEmitter, event)
        assertEquals(1, events.size)
        assertEquals(event, events[0])
    }

    @Test
    fun `returns nick change failed events after server ready`() {
        fakeServerState.status = ServerStatus.Ready
        val event = NicknameChangeFailed(EventMetadata(TestConstants.time), NicknameChangeError.AlreadyInUse)
        val events = mutator.mutateEvent(mockClient, mockEmitter, event)
        assertEquals(1, events.size)
        assertEquals(event, events[0])
    }

    @Test
    fun `returns nick change required event before server ready`() {
        fakeServerState.status = ServerStatus.Negotiating
        val event = NicknameChangeFailed(EventMetadata(TestConstants.time), NicknameChangeError.AlreadyInUse)
        val events = mutator.mutateEvent(mockClient, mockEmitter, event)
        assertEquals(1, events.size)
        val received = events[0] as NicknameChangeRequired
        assertEquals(event.metadata, received.metadata)
        assertEquals(event.cause, received.cause)
    }

}