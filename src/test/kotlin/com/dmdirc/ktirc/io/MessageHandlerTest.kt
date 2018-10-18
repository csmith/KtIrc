package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.EventHandler
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerWelcome
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
    fun `MessageHandler passes message on to correct processor`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), mutableListOf())
        val message = IrcMessage(emptyMap(), null, "JOIN", emptyList())

        with(Channel<IrcMessage>(1)) {
            send(message)
            close()
            handler.processMessages(ircClient, this)
        }

        verify(joinProcessor).process(message)
        Unit
    }

    @Test
    fun `MessageHandler reads multiple messages`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), mutableListOf())
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
        Unit
    }

    @Test
    fun `MessageHandler invokes all event handler with all returned events`() = runBlocking {
        val eventHandler1 = mock<EventHandler>()
        val eventHandler2 = mock<EventHandler>()
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor), mutableListOf(eventHandler1, eventHandler2))
        val joinMessage = IrcMessage(emptyMap(), null, "JOIN", emptyList())
        whenever(joinProcessor.process(any())).thenReturn(listOf(ServerConnected, ServerWelcome("abc")))

        with(Channel<IrcMessage>(1)) {
            send(joinMessage)
            close()
            handler.processMessages(ircClient, this)
        }

        verify(eventHandler1).processEvent(ircClient, ServerConnected)
        verify(eventHandler1).processEvent(ircClient, ServerWelcome("abc"))
        verify(eventHandler2).processEvent(ircClient, ServerConnected)
        verify(eventHandler2).processEvent(ircClient, ServerWelcome("abc"))
    }

}