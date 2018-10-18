package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChannelStateHandlerTest {

    private val handler = ChannelStateHandler()
    private val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
    private val serverState = ServerState("")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
        on { channelState } doReturn channelStateMap
        on { isLocalUser(User("acidburn", "libby", "root.localhost")) } doReturn true
    }

    @Test
    fun `ChannelStateHandler creates new state object for local joins`() = runBlocking {
        handler.processEvent(ircClient, ChannelJoined(User("acidburn", "libby", "root.localhost"), "#thegibson"))
        assertTrue("#thegibson" in channelStateMap)
    }

    @Test
    fun `ChannelStateHandler does not create new state object for remote joins`() = runBlocking {
        handler.processEvent(ircClient, ChannelJoined(User("zerocool", "dade", "root.localhost"), "#thegibson"))
        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `ChannelStateHandler adds joiners to channel state`() = runBlocking {
        channelStateMap += ChannelState("#thegibson") { CaseMapping.Rfc }

        handler.processEvent(ircClient, ChannelJoined(User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertTrue("zerocool" in channelStateMap["#thegibson"]?.users!!)
    }

    @Test
    fun `ChannelStateHandler clears existing users when getting a new list`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("acidBurn")
        channel.users += ChannelUser("thePlague")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("zeroCool")))

        assertEquals(1, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
    }

    @Test
    fun `ChannelStateHandler adds users from multiple name received events`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("thePlague")))

        assertEquals(3, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `ChannelStateHandler clears and readds users on additional names received`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesFinished("#thegibson"))
        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("thePlague")))

        assertEquals(2, channel.users.count())
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `ChannelStateHandler adds users with mode prefixes`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("@zeroCool", "@+acidBurn", "+thePlague", "cerealKiller")))
        handler.processEvent(ircClient, ChannelNamesFinished("#thegibson"))

        assertEquals(4, channel.users.count())
        assertEquals("o", channel.users["zeroCool"]?.modes)
        assertEquals("ov", channel.users["acidBurn"]?.modes)
        assertEquals("v", channel.users["thePlague"]?.modes)
        assertEquals("", channel.users["cerealKiller"]?.modes)
    }

    @Test
    fun `ChannelStateHandler adds users with full hosts`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived("#thegibson", listOf("@zeroCool!dade@root.localhost", "+acidBurn!libby@root.localhost")))
        handler.processEvent(ircClient, ChannelNamesFinished("#thegibson"))

        assertEquals(2, channel.users.count())
        assertEquals("o", channel.users["zeroCool"]?.modes)
        assertEquals("v", channel.users["acidBurn"]?.modes)
    }

    @Test
    fun `ChannelStateHandler removes state object for local parts`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelParted(User("acidburn", "libby", "root.localhost"), "#thegibson"))

        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `ChannelStateHandler removes user from channel member list for remote parts`() = runBlocking {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelParted(User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `ChannelStateHandler removes user from all channel member lists for quits`() = runBlocking {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            channelStateMap += this
        }

        with (ChannelState("#dumpsterdiving") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            channelStateMap += this
        }

        with (ChannelState("#chat") { CaseMapping.Rfc }) {
            users += ChannelUser("AcidBurn")
            channelStateMap += this
        }

        handler.processEvent(ircClient, UserQuit(User("zerocool", "dade", "root.localhost")))

        assertFalse("zerocool" in channelStateMap["#thegibson"]!!.users)
        assertFalse("zerocool" in channelStateMap["#dumpsterdiving"]!!.users)
        assertFalse("zerocool" in channelStateMap["#chat"]!!.users)
        assertTrue("acidburn" in channelStateMap["#chat"]!!.users)
    }

}