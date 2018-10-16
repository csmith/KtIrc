package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.ChannelStateMap
import com.dmdirc.ktirc.model.User
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ChannelStateHandlerTest {

    private val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
    private val ircClient = mock<IrcClient> {
        on { channelState } doReturn channelStateMap
        on { isLocalUser(User("acidburn", "libby", "root.localhost")) } doReturn true
    }

    @Test
    fun `ChannelStateHandler creates new state object for local joins`() = runBlocking {
        val handler = ChannelStateHandler()
        handler.processEvent(ircClient, ChannelJoined(User("acidburn", "libby", "root.localhost"), "#newchannel"))
        assertTrue("#newchannel" in channelStateMap)
    }

    @Test
    fun `ChannelStateHandler does not create new state object for remote joins`() = runBlocking {
        val handler = ChannelStateHandler()
        handler.processEvent(ircClient, ChannelJoined(User("zerocool", "dade", "root.localhost"), "#newchannel"))
        assertFalse("#newchannel" in channelStateMap)
    }

}