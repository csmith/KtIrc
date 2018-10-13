package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.state.ServerFeature
import com.dmdirc.ktirc.state.ServerState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ISupportProcessorTest {

    private val state = mock<ServerState>()
    private val processor = ISupportProcessor(state)

    @Test
    fun `ISupportProcessor can handle 005s`() {
        assertTrue(processor.commands.contains("005")) { "ISupportProcessor should handle 005 messages" }
    }

    @Test
    fun `ISupportProcessor handles multiple numeric arguments`() {
        processor.process(IrcMessage(null, "server.com".toByteArray(), "005",
                listOf("nickname", "CHANLIMIT=123", "CHANNELLEN=456", "are supported blah blah").map { it.toByteArray() }))

        verify(state).setFeature(ServerFeature.MaximumChannels, 123)
        verify(state).setFeature(ServerFeature.MaximumChannelNameLength, 456)
    }

    @Test
    fun `ISupportProcessor handles string arguments`() {
        processor.process(IrcMessage(null, "server.com".toByteArray(), "005",
                listOf("nickname", "CHANMODES=abcd", "are supported blah blah").map { it.toByteArray() }))

        verify(state).setFeature(ServerFeature.ChannelModes, "abcd")
    }

    @Test
    fun `ISupportProcessor handles resetting arguments`() {
        processor.process(IrcMessage(null, "server.com".toByteArray(), "005",
                listOf("nickname", "-CHANMODES", "are supported blah blah").map { it.toByteArray() }))

        verify(state).resetFeature(ServerFeature.ChannelModes)
    }

    @Test
    fun `ISupportProcessor handles case mapping arguments`() {
        processor.process(IrcMessage(null, "server.com".toByteArray(), "005",
                listOf("nickname", "CASEMAPPING=rfc1459-strict", "are supported blah blah").map { it.toByteArray() }))

        verify(state).setFeature(ServerFeature.ServerCaseMapping, CaseMapping.RfcStrict)
    }

    @Test
    fun `ISupportProcessor handles boolean features with no arguments`() {
        processor.process(IrcMessage(null, "server.com".toByteArray(), "005",
                listOf("nickname", "WHOX", "are supported blah blah").map { it.toByteArray() }))

        verify(state).setFeature(ServerFeature.WhoxSupport, true)
    }

}