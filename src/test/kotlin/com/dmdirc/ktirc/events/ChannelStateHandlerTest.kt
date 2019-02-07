package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChannelStateHandlerTest {

    private val handler = ChannelStateHandler()
    private val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
    private val serverState = ServerState("", "", emptyList())
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
        on { channelState } doReturn channelStateMap
        on { isLocalUser(User("acidburn", "libby", "root.localhost")) } doReturn true
        on { isLocalUser("acidburn") } doReturn  true
    }

    @Test
    fun `creates new state object for local joins`() {
        handler.processEvent(ircClient, ChannelJoined(TestConstants.time, User("acidburn", "libby", "root.localhost"), "#thegibson"))
        assertTrue("#thegibson" in channelStateMap)
    }

    @Test
    fun `does not create new state object for remote joins`() {
        handler.processEvent(ircClient, ChannelJoined(TestConstants.time, User("zerocool", "dade", "root.localhost"), "#thegibson"))
        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `adds joiners to channel state`() {
        channelStateMap += ChannelState("#thegibson") { CaseMapping.Rfc }

        handler.processEvent(ircClient, ChannelJoined(TestConstants.time, User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertTrue("zerocool" in channelStateMap["#thegibson"]?.users!!)
    }

    @Test
    fun `clears existing users when getting a new list`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("acidBurn")
        channel.users += ChannelUser("thePlague")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("zeroCool")))

        assertEquals(1, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
    }

    @Test
    fun `adds users from multiple name received events`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("thePlague")))

        assertEquals(3, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `clears and readds users on additional names received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesFinished(TestConstants.time, "#thegibson"))
        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("thePlague")))

        assertEquals(2, channel.users.count())
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `adds users with mode prefixes`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("@zeroCool", "@+acidBurn", "+thePlague", "cerealKiller")))
        handler.processEvent(ircClient, ChannelNamesFinished(TestConstants.time, "#thegibson"))

        assertEquals(4, channel.users.count())
        assertEquals("o", channel.users["zeroCool"]?.modes)
        assertEquals("ov", channel.users["acidBurn"]?.modes)
        assertEquals("v", channel.users["thePlague"]?.modes)
        assertEquals("", channel.users["cerealKiller"]?.modes)
    }

    @Test
    fun `adds users with full hosts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("@zeroCool!dade@root.localhost", "+acidBurn!libby@root.localhost")))
        handler.processEvent(ircClient, ChannelNamesFinished(TestConstants.time, "#thegibson"))

        assertEquals(2, channel.users.count())
        assertEquals("o", channel.users["zeroCool"]?.modes)
        assertEquals("v", channel.users["acidBurn"]?.modes)
    }

    @Test
    fun `removes state object for local parts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("acidburn", "libby", "root.localhost"), "#thegibson"))

        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `removes user from channel member list for remote parts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `removes state object for local kicks`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("zerocool", "dade", "root.localhost"), "#thegibson", "acidburn", "Bye!"))

        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `removes user from channel member list for remote kicks`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("acidburn", "libby", "root.localhost"), "#thegibson", "zerocool", "Bye!"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `removes user from all channel member lists for quits`() {
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

        handler.processEvent(ircClient, UserQuit(TestConstants.time, User("zerocool", "dade", "root.localhost")))

        assertFalse("zerocool" in channelStateMap["#thegibson"]!!.users)
        assertFalse("zerocool" in channelStateMap["#dumpsterdiving"]!!.users)
        assertFalse("zerocool" in channelStateMap["#chat"]!!.users)
        assertTrue("acidburn" in channelStateMap["#chat"]!!.users)
    }


    @Test
    fun `raises ChannelQuit event for each channel a user quits from`() {
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

        val events = handler.processEvent(ircClient, UserQuit(TestConstants.time, User("zerocool", "dade", "root.localhost"), "Hack the planet!"))

        val names = mutableListOf<String>()
        assertEquals(2, events.size)
        events.forEach { event ->
            (event as ChannelQuit).let {
                assertEquals(TestConstants.time, it.time)
                assertEquals("zerocool", it.user.nickname)
                assertEquals("Hack the planet!", it.reason)
                names.add(it.channel)
            }
        }

        assertTrue("#thegibson" in names)
        assertTrue("#dumpsterdiving" in names)
    }

    @Test
    fun `sets mode discovered flag when discovered mode event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "+", emptyArray(), true))

        assertTrue(channel.modesDiscovered)
    }

    @Test
    fun `adds modes when discovered mode event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "+ceg", arrayOf("CCC", "EEE"), true))

        assertEquals("CCC", channel.modes['c'])
        assertEquals("EEE", channel.modes['e'])
        assertEquals("", channel.modes['g'])
    }

    @Test
    fun `adjusts complex modes when mode change event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.modes['c'] = "CCC"
        channel.modes['e'] = "EEE"
        channel.modes['h'] = ""
        channelStateMap += channel
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "-c+d-eh+fg", arrayOf("CCC", "DDD", "FFF"), true))

        assertNull(channel.modes['c'])
        assertEquals("DDD", channel.modes['d'])
        assertNull(channel.modes['e'])
        assertEquals("FFF", channel.modes['f'])
        assertEquals("", channel.modes['g'])
        assertNull(channel.modes['h'])
    }

    @Test
    fun `handles unprivileged user gaining new mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "+o", arrayOf("zeroCool")))

        assertEquals("o", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles privileged user gaining lesser mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "o")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "+v", arrayOf("zeroCool")))

        assertEquals("ov", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles privileged user gaining greater mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "v")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "+o", arrayOf("zeroCool")))

        assertEquals("ov", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles user gaining multiple modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "+vo", arrayOf("zeroCool", "zeroCool")))

        assertEquals("ov", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles user losing multiple modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "ov")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "-vo", arrayOf("zeroCool", "zeroCool")))

        assertEquals("", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles mixture of user modes and normal modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "v")
            channelStateMap += this
        }
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(TestConstants.time, "#thegibson", "oa-v+b", arrayOf("zeroCool", "aaa", "zeroCool", "bbb")))

        assertEquals("o", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
        assertEquals("aaa", channelStateMap["#thegibson"]?.modes?.get('a'))
        assertEquals("bbb", channelStateMap["#thegibson"]?.modes?.get('b'))
    }

}
