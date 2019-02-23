package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.events.handlers.EventHandler
import com.dmdirc.ktirc.events.mutators.EventMutator
import com.dmdirc.ktirc.messages.processors.MessageProcessor
import com.dmdirc.ktirc.model.IrcMessage
import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class MessageHandlerTest {

    private val ircClient = mockk<IrcClient>()

    private val nickProcessor = mockk<MessageProcessor> {
        every { commands } returns arrayOf("FOO", "NICK")
        every { process(any()) } returns emptyList()
    }

    private val joinProcessor = mockk<MessageProcessor> {
        every { commands } returns arrayOf("BAR", "JOIN")
        every { process(any()) } returns emptyList()
    }

    private val eventHandler1 = mockk<EventHandler>()
    private val eventHandler2 = mockk<EventHandler>()

    @Test
    fun `passes message on to correct processor`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), emptyList(), mutableListOf())
        val message = IrcMessage(emptyMap(), null, "JOIN", emptyList())

        with(Channel<IrcMessage>(1)) {
            send(message)
            close()
            handler.processMessages(ircClient, this)
        }

        verify {
            joinProcessor.process(message)
        }
    }

    @Test
    fun `reads multiple messages`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), emptyList(), mutableListOf())
        val joinMessage = IrcMessage(emptyMap(), null, "JOIN", emptyList())
        val nickMessage = IrcMessage(emptyMap(), null, "NICK", emptyList())
        val otherMessage = IrcMessage(emptyMap(), null, "OTHER", emptyList())

        with(Channel<IrcMessage>(3)) {
            send(joinMessage)
            send(nickMessage)
            send(otherMessage)
            close()
            handler.processMessages(ircClient, this)
        }

        verifyOrder {
            joinProcessor.process(joinMessage)
            nickProcessor.process(nickMessage)
        }
    }

    @Test
    fun `invokes all event handler with all returned events`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), emptyList(), mutableListOf(eventHandler1, eventHandler2))
        val joinMessage = IrcMessage(emptyMap(), null, "JOIN", emptyList())

        every { joinProcessor.process(any()) } returns listOf(ServerConnected(EventMetadata(TestConstants.time)), ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))

        with(Channel<IrcMessage>(1)) {
            send(joinMessage)
            close()
            handler.processMessages(ircClient, this)
        }

        verify {
            eventHandler1.processEvent(refEq(ircClient), ofType<ServerConnected>())
            eventHandler1.processEvent(refEq(ircClient), ofType<ServerWelcome>())
            eventHandler2.processEvent(refEq(ircClient), ofType<ServerConnected>())
            eventHandler2.processEvent(refEq(ircClient), ofType<ServerWelcome>())
        }
    }

    @Test
    fun `sends custom events to all handlers`() {
        val handler = MessageHandler(emptyList(), emptyList(), mutableListOf(eventHandler1, eventHandler2))
        handler.handleEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))

        verify {
            eventHandler1.processEvent(refEq(ircClient), ofType<ServerWelcome>())
            eventHandler2.processEvent(refEq(ircClient), ofType<ServerWelcome>())
        }
    }

    @Test
    fun `sends custom events to all emitters`() {
        val handler = MessageHandler(emptyList(), emptyList(), emptyList())
        val emitter1 = mockk<(IrcEvent) -> Unit> {
            every { this@mockk(any<IrcEvent>()) } just Runs
        }
        val emitter2 = mockk<(IrcEvent) -> Unit>  {
            every { this@mockk(any<IrcEvent>()) } just Runs
        }
        handler.addEmitter(emitter1)
        handler.addEmitter(emitter2)
        handler.handleEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))

        verify {
            emitter1(ofType<ServerWelcome>())
            emitter2(ofType<ServerWelcome>())
        }
    }

    @Test
    fun `sends events to handlers but not mutators or emitters if process only is true`() {
        val mutator = mockk<EventMutator>()
        val handler = MessageHandler(emptyList(), listOf(mutator), listOf(eventHandler1, eventHandler2))
        val emitter = mockk<(IrcEvent) -> Unit>()
        handler.addEmitter(emitter)

        handler.handleEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"), true)

        verify {
            eventHandler1.processEvent(refEq(ircClient), ofType<ServerWelcome>())
            eventHandler2.processEvent(refEq(ircClient), ofType<ServerWelcome>())
        }
        verify(inverse = true) {
            emitter.invoke(any())
            mutator.mutateEvent(any(), any(), any())
        }
    }

    @Test
    fun `mutates events in order`() {
        val eventMutator1 = mockk<EventMutator> {
            every { mutateEvent(any(), any(), ofType<ServerWelcome>()) } returns listOf(ServerReady(EventMetadata(TestConstants.time)))
        }
        val eventMutator2 = mockk<EventMutator> {
            every { mutateEvent(any(), any(), ofType<ServerReady>()) } returns  listOf(ServerConnected(EventMetadata(TestConstants.time)))
        }
        val eventHandler = mockk<EventHandler>()

        val handler = MessageHandler(emptyList(), listOf(eventMutator1, eventMutator2), mutableListOf(eventHandler))
        handler.handleEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))

        verifyAll {
            eventMutator1.mutateEvent(refEq(ircClient), refEq(handler), ofType<ServerWelcome>())
            eventMutator2.mutateEvent(refEq(ircClient), refEq(handler), ofType<ServerReady>())
            eventHandler.processEvent(refEq(ircClient), ofType<ServerConnected>())
        }
    }

    @Test
    fun `allows mutators to fan out events`() {
        val eventMutator1 = mockk<EventMutator> {
            every { mutateEvent(any(), any(), ofType<ServerWelcome>()) } returns listOf(
                    ServerReady(EventMetadata(TestConstants.time)),
                    ServerConnected(EventMetadata(TestConstants.time))
            )
        }
        val eventMutator2 = mockk<EventMutator> {
            every { mutateEvent(any(), any(), ofType<ServerReady>()) } returns listOf(ServerReady(EventMetadata(TestConstants.time)))
            every { mutateEvent(any(), any(), ofType<ServerConnected>()) } returns listOf(ServerConnected(EventMetadata(TestConstants.time)))
        }
        val eventHandler = mockk<EventHandler>()

        val handler = MessageHandler(emptyList(), listOf(eventMutator1, eventMutator2), mutableListOf(eventHandler))
        handler.handleEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))

        verifyOrder {
            eventMutator2.mutateEvent(refEq(ircClient), refEq(handler), ofType<ServerReady>())
            eventMutator2.mutateEvent(refEq(ircClient), refEq(handler), ofType<ServerConnected>())
            eventHandler.processEvent(refEq(ircClient), ofType<ServerReady>())
            eventHandler.processEvent(refEq(ircClient), ofType<ServerConnected>())
        }
    }

    @Test
    fun `allows mutators to suppress events`() {
        val eventMutator1 = mockk<EventMutator> {
            every { mutateEvent(any(), any(), ofType<ServerWelcome>()) } returns emptyList()
        }
        val eventMutator2 = mockk<EventMutator>()
        val eventHandler = mockk<EventHandler>()

        val handler = MessageHandler(emptyList(), listOf(eventMutator1, eventMutator2), mutableListOf(eventHandler))
        handler.handleEvent(ircClient, ServerWelcome(EventMetadata(TestConstants.time), "the.gibson", "acidBurn"))

        verify(inverse = true) {
            eventMutator2.mutateEvent(any(), refEq(handler), any())
            eventHandler.processEvent(any(), any())
        }
    }

}
