package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.messages.MessageProcessor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class MessageHandlerTest {

    private val nickProcessor = mock<MessageProcessor> {
        on { commands } doReturn arrayOf("FOO", "NICK")
    }

    private val joinProcessor = mock<MessageProcessor> {
        on { commands } doReturn arrayOf("BAR", "JOIN")
    }

    @Test
    fun `MessageHandler passes message on to correct processor`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor))
        val message = IrcMessage(null, null, "JOIN", emptyList())

        with(Channel<IrcMessage>(1)) {
            send(message)
            close()
            handler.processMessages(this)
        }

        verify(joinProcessor).process(message)
    }

    @Test
    fun `MessageHandler reads multiple messages`() = runBlocking {
        val handler = MessageHandler(listOf(joinProcessor, nickProcessor))
        val joinMessage = IrcMessage(null, null, "JOIN", emptyList())
        val nickMessage = IrcMessage(null, null, "NICK", emptyList())
        val otherMessage = IrcMessage(null, null, "OTHER", emptyList())

        with(Channel<IrcMessage>(3)) {
            send(joinMessage)
            send(nickMessage)
            send(otherMessage)
            close()
            handler.processMessages(this)
        }

        with(inOrder(joinProcessor, nickProcessor)) {
            verify(joinProcessor).process(joinMessage)
            verify(nickProcessor).process(nickMessage)
        }
    }

}