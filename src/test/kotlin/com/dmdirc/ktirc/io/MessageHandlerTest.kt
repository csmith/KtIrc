package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerReady
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.events.handlers.EventHandler
import com.dmdirc.ktirc.events.mutators.EventMutator
import com.dmdirc.ktirc.messages.MessageProcessor
import com.dmdirc.ktirc.model.IrcMessage
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class MessageHandlerTest {

    private val ircClient = mock<IrcClient>()

    private val nickProcessor = mock<MessageProcessor> {
        on { commands } doReturn arrayOf("FOO", "NICK")
    }

    private val joinProcessor = mock<MessageProcessor> {
        on { commands } doReturn arrayOf("BAR", "JOIN")
    }

    @Test
    fun `passes message on to correct processor`() = runBlocking<Unit> {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), emptyList(), mutableListOf())
        val message = IrcMessage(emptyMap(), null, "JOIN", emptyList())

        with(Channel<IrcMessage>(1)) {
            send(message)
            close()
            handler.processMessages(ircClient, this)
        }

        verify(joinProcessor).process(message)
    }

    @Test
    fun `reads multiple messages`() = runBlocking<Unit> {
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

        with(inOrder(joinProcessor, nickProcessor)) {
            verify(joinProcessor).process(joinMessage)
            verify(nickProcessor).process(nickMessage)
        }
    }

    @Test
    fun `invokes all event handler with all returned events`() = runBlocking {
        val eventHandler1 = mock<EventHandler>()
        val eventHandler2 = mock<EventHandler>()
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), emptyList(), mutableListOf(eventHandler1, eventHandler2))
        val joinMessage = IrcMessage(emptyMap(), null, "JOIN", emptyList())
        whenever(joinProcessor.process(any())).thenReturn(listOf(ServerConnected(TestConstants.time), ServerWelcome(TestConstants.time, "the.gibson", "acidBurn")))

        with(Channel<IrcMessage>(1)) {
            send(joinMessage)
            close()
            handler.processMessages(ircClient, this)
        }

        verify(eventHandler1).processEvent(same(ircClient), isA<ServerConnected>())
        verify(eventHandler1).processEvent(same(ircClient), isA<ServerWelcome>())
        verify(eventHandler2).processEvent(same(ircClient), isA<ServerConnected>())
        verify(eventHandler2).processEvent(same(ircClient), isA<ServerWelcome>())
    }

    @Test
    fun `emits custom events to all handlers`() {
        val eventHandler1 = mock<EventHandler>()
        val eventHandler2 = mock<EventHandler>()
        val handler = MessageHandler(emptyList(), emptyList(), mutableListOf(eventHandler1, eventHandler2))
        handler.emitEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))

        verify(eventHandler1).processEvent(same(ircClient), isA<ServerWelcome>())
        verify(eventHandler2).processEvent(same(ircClient), isA<ServerWelcome>())
    }

    @Test
    fun `mutates events in order`() {
        val eventMutator1 = mock<EventMutator> {
            on { mutateEvent(any(), isA<ServerWelcome>()) } doReturn listOf(ServerReady(TestConstants.time))
        }
        val eventMutator2 = mock<EventMutator> {
            on { mutateEvent(any(), isA<ServerReady>()) } doReturn listOf(ServerConnected(TestConstants.time))
        }
        val eventHandler = mock<EventHandler>()

        val handler = MessageHandler(emptyList(), listOf(eventMutator1, eventMutator2), mutableListOf(eventHandler))
        handler.emitEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))

        verify(eventMutator1).mutateEvent(same(ircClient), isA<ServerWelcome>())
        verify(eventMutator2).mutateEvent(same(ircClient), isA<ServerReady>())
        verify(eventHandler).processEvent(same(ircClient), isA<ServerConnected>())
        verifyNoMoreInteractions(eventHandler)
    }

    @Test
    fun `allows mutators to fan out events`() {
        val eventMutator1 = mock<EventMutator> {
            on { mutateEvent(any(), isA<ServerWelcome>()) } doReturn listOf(
                    ServerReady(TestConstants.time),
                    ServerConnected(TestConstants.time)
            )
        }
        val eventMutator2 = mock<EventMutator> {
            on { mutateEvent(any(), isA<ServerReady>()) } doReturn listOf(ServerReady(TestConstants.time))
            on { mutateEvent(any(), isA<ServerConnected>()) } doReturn listOf(ServerConnected(TestConstants.time))
        }
        val eventHandler = mock<EventHandler>()

        val handler = MessageHandler(emptyList(), listOf(eventMutator1, eventMutator2), mutableListOf(eventHandler))
        handler.emitEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))

        with (inOrder(eventMutator2, eventHandler)) {
            verify(eventMutator2).mutateEvent(same(ircClient), isA<ServerReady>())
            verify(eventMutator2).mutateEvent(same(ircClient), isA<ServerConnected>())
            verify(eventHandler).processEvent(same(ircClient), isA<ServerReady>())
            verify(eventHandler).processEvent(same(ircClient), isA<ServerConnected>())
        }
    }

    @Test
    fun `allows mutators to suppress events`() {
        val eventMutator1 = mock<EventMutator> {
            on { mutateEvent(any(), isA<ServerWelcome>()) } doReturn emptyList()
        }
        val eventMutator2 = mock<EventMutator>()
        val eventHandler = mock<EventHandler>()

        val handler = MessageHandler(emptyList(), listOf(eventMutator1, eventMutator2), mutableListOf(eventHandler))
        handler.emitEvent(ircClient, ServerWelcome(TestConstants.time, "the.gibson", "acidBurn"))

        verify(eventMutator2, never()).mutateEvent(any(), any())
        verify(eventHandler, never()).processEvent(any(), any())
    }

}
