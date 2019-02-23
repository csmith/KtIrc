package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.Batch
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BatchMutatorTest {

    private val mutator = BatchMutator()

    private val messageEmitter = mockk<MessageEmitter>()
    private val fakeServerState = ServerState("", "")
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }

    @Test
    fun `returns non-batched events unmodified`() {
        val event = ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn")
        val events = mutator.mutateEvent(ircClient, messageEmitter, event)

        assertEquals(1, events.size)
        assertSame(event, events[0])
    }

    @Test
    fun `starts a batch when BatchStarted event is received`() {
        val event = BatchStarted(EventMetadata(TestConstants.time), "abcdef", "netsplit", arrayOf("foo", "bar"))
        val events = mutator.mutateEvent(ircClient, messageEmitter, event)

        assertTrue(events.isEmpty())
        assertNotNull(fakeServerState.batches["abcdef"])
        fakeServerState.batches["abcdef"]?.let {
            assertEquals(listOf("foo", "bar"), it.arguments)
            assertEquals("netsplit", it.type)
            assertTrue(it.events.isEmpty())
            assertNull(it.metadata.batchId)
        }
    }

    @Test
    fun `adds to batch when event has a batch ID`() {
        fakeServerState.batches["abcdef"] = Batch("netsplit", emptyList(), EventMetadata(TestConstants.time))

        val event = UserNickChanged(EventMetadata(TestConstants.time, "abcdef"), User("zeroCool"), "crashOverride")
        mutator.mutateEvent(ircClient, messageEmitter, event)

        assertEquals(listOf(event), fakeServerState.batches["abcdef"]!!.events)
    }

    @Test
    fun `suppresses event when it has a batch ID`() {
        fakeServerState.batches["abcdef"] = Batch("netsplit", emptyList(), EventMetadata(TestConstants.time))

        val event = UserNickChanged(EventMetadata(TestConstants.time, "abcdef"), User("zeroCool"), "crashOverride")
        val events = mutator.mutateEvent(ircClient, messageEmitter, event)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `passes event for processing only when it has a batch ID`() {
        fakeServerState.batches["abcdef"] = Batch("netsplit", emptyList(), EventMetadata(TestConstants.time))

        val event = UserNickChanged(EventMetadata(TestConstants.time, "abcdef"), User("zeroCool"), "crashOverride")
        mutator.mutateEvent(ircClient, messageEmitter, event)

        verify {
            messageEmitter.handleEvent(any(), refEq(event), eq(true))
        }
    }

    @Test
    fun `sends a batch when it finishes and the parent is null`() {
        fakeServerState.batches["abcdef"] = Batch("netsplit", listOf("p1", "p2"), EventMetadata(TestConstants.time), events = mutableListOf(ServerConnected(EventMetadata(TestConstants.time, "abcdef"))))

        val events = mutator.mutateEvent(ircClient, messageEmitter, BatchFinished(EventMetadata(TestConstants.time), "abcdef"))

        assertEquals(1, events.size)
        assertTrue(events[0] is BatchReceived)
        val event = events[0] as BatchReceived
        assertEquals("netsplit", event.type)
        assertArrayEquals(arrayOf("p1", "p2"), event.params)
        assertEquals(1, event.events.size)
        assertTrue(event.events[0] is ServerConnected)
    }

    @Test
    fun `sends finished batch with correct metadata`() {
        val metadata = EventMetadata(TestConstants.time, label = "1234")
        fakeServerState.batches["abcdef"] = Batch("netsplit", listOf("p1", "p2"), metadata, events = mutableListOf(ServerConnected(EventMetadata(TestConstants.time, "abcdef"))))

        val events = mutator.mutateEvent(ircClient, messageEmitter, BatchFinished(EventMetadata(TestConstants.time), "abcdef"))

        assertEquals(1, events.size)
        assertTrue(events[0] is BatchReceived)
        val event = events[0] as BatchReceived
        assertSame(metadata, event.metadata)
    }

    @Test
    fun `adds a batch to its parent when it finishes`() {
        fakeServerState.batches["12345"] = Batch("history", emptyList(), EventMetadata(TestConstants.time))
        fakeServerState.batches["abcdef"] = Batch("netsplit", emptyList(), EventMetadata(TestConstants.time, batchId = "12345"), mutableListOf(ServerConnected(EventMetadata(TestConstants.time, "abcdef"))))

        val events = mutator.mutateEvent(ircClient, messageEmitter, BatchFinished(EventMetadata(TestConstants.time), "abcdef"))

        assertEquals(0, events.size)

        val parent = fakeServerState.batches["12345"]?.events
        assertEquals(1, parent?.size)

        val event = parent?.get(0) as BatchReceived
        assertEquals(1, event.events.size)
        assertTrue(event.events[0] is ServerConnected)
    }

    @Test
    fun `deletes batch when it finishes`() {
        fakeServerState.batches["abcdef"] = Batch("netsplit", emptyList(), EventMetadata(TestConstants.time), events = mutableListOf(ServerConnected(EventMetadata(TestConstants.time, "abcdef"))))

        mutator.mutateEvent(ircClient, messageEmitter, BatchFinished(EventMetadata(TestConstants.time), "abcdef"))

        assertNull(fakeServerState.batches["abcdef"])
    }

}
