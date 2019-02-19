package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.BehaviourConfig
import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChannelStateHandlerTest {

    private val handler = ChannelStateHandler()
    private val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
    private val serverState = ServerState("", "")
    private val behaviour = BehaviourConfig()
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
        on { channelState } doReturn channelStateMap
        on { behaviour } doReturn behaviour
        on { isLocalUser(User("acidburn", "libby", "root.localhost")) } doReturn true
        on { isLocalUser("acidburn") } doReturn  true
    }

    @Test
    fun `creates new state object for local joins`() {
        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson"))
        assertTrue("#thegibson" in channelStateMap)
    }

    @Test
    fun `does not create new state object for remote joins`() {
        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))
        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `adds joiners to channel state`() {
        channelStateMap += ChannelState("#thegibson") { CaseMapping.Rfc }

        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertTrue("zerocool" in channelStateMap["#thegibson"]?.users!!)
    }

    @Test
    fun `clears existing users when getting a new list`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("acidBurn")
        channel.users += ChannelUser("thePlague")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("zeroCool")))

        assertEquals(1, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
    }

    @Test
    fun `adds users from multiple name received events`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("thePlague")))

        assertEquals(3, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `clears and readds users on additional names received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("thePlague")))

        assertEquals(2, channel.users.count())
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `adds users with mode prefixes`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@zeroCool", "@+acidBurn", "+thePlague", "cerealKiller")))
        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

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

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@zeroCool!dade@root.localhost", "+acidBurn!libby@root.localhost")))
        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        assertEquals(2, channel.users.count())
        assertEquals("o", channel.users["zeroCool"]?.modes)
        assertEquals("v", channel.users["acidBurn"]?.modes)
    }

    @Test
    fun `updates receiving user list state`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@zeroCool!dade@root.localhost", "+acidBurn!libby@root.localhost")))

        assertTrue(channel.receivingUserList)

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        assertFalse(channel.receivingUserList)
    }

    @Test
    fun `requests modes on end of names if configured and undiscovered`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
        behaviour.requestModesOnJoin = true

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        verify(ircClient).send("MODE", "#thegibson")
    }

    @Test
    fun `does not request modes on end of names if already discovered`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
        behaviour.requestModesOnJoin = true
        channel.modesDiscovered = true

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        verify(ircClient, never()).send("MODE", "#thegibson")
    }

    @Test
    fun `does not request modes on end of names if not configured`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
        behaviour.requestModesOnJoin = false

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        verify(ircClient, never()).send("MODE", "#thegibson")
    }

    @Test
    fun `removes state object for local parts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson"))

        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `removes user from channel member list for remote parts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `removes state object for local kicks`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson", "acidburn", "Bye!"))

        assertFalse("#thegibson" in channelStateMap)
    }

    @Test
    fun `removes user from channel member list for remote kicks`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson", "zerocool", "Bye!"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `removes user from channel member lists for quits`() {
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

        handler.processEvent(ircClient, ChannelQuit(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))
        handler.processEvent(ircClient, ChannelQuit(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertFalse("zerocool" in channelStateMap["#thegibson"]!!.users)
        assertFalse("zerocool" in channelStateMap["#dumpsterdiving"]!!.users)
        assertFalse("zerocool" in channelStateMap["#chat"]!!.users)
        assertTrue("acidburn" in channelStateMap["#chat"]!!.users)
    }

    @Test
    fun `renames user in channel member list for nick changes`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("acidBurn")
        channelStateMap += channel

        handler.processEvent(ircClient, ChannelNickChanged(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson", "acidB"))
        handler.processEvent(ircClient, ChannelNickChanged(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#dumpsterdiving", "acidB"))

        assertFalse("acidBurn" in channel.users)
        assertTrue("acidB" in channel.users)
        assertEquals("acidB", channel.users["acidB"]?.nickname)
    }

    @Test
    fun `sets mode discovered flag when discovered mode event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "+", emptyArray(), true))

        assertTrue(channel.modesDiscovered)
    }

    @Test
    fun `adds modes when discovered mode event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += channel
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "+ceg", arrayOf("CCC", "EEE"), true))

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

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "-c+d-eh+fg", arrayOf("CCC", "DDD", "FFF"), true))

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

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "+o", arrayOf("zeroCool")))

        assertEquals("o", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles privileged user gaining lesser mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "o")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "+v", arrayOf("zeroCool")))

        assertEquals("ov", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles privileged user gaining greater mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "v")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "+o", arrayOf("zeroCool")))

        assertEquals("ov", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles user gaining multiple modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "+vo", arrayOf("zeroCool", "zeroCool")))

        assertEquals("ov", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles user losing multiple modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "ov")
            channelStateMap += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "-vo", arrayOf("zeroCool", "zeroCool")))

        assertEquals("", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles mixture of user modes and normal modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "v")
            channelStateMap += this
        }
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), "#thegibson", "oa-v+b", arrayOf("zeroCool", "aaa", "zeroCool", "bbb")))

        assertEquals("o", channelStateMap["#thegibson"]?.users?.get("zeroCool")?.modes)
        assertEquals("aaa", channelStateMap["#thegibson"]?.modes?.get('a'))
        assertEquals("bbb", channelStateMap["#thegibson"]?.modes?.get('b'))
    }

    @Test
    fun `updates topic state when it's discovered for the first time`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += state

        handler.processEvent(ircClient, ChannelTopicDiscovered(EventMetadata(TestConstants.time), "#thegibson", "Hack the planet!"))
        handler.processEvent(ircClient, ChannelTopicMetadataDiscovered(EventMetadata(TestConstants.time), "#thegibson", User("acidBurn"), TestConstants.otherTime))

        assertTrue(state.topicDiscovered)
        assertEquals(ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.otherTime), state.topic)
    }

    @Test
    fun `updates topic state when no topic is discovered for the first time`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += state

        handler.processEvent(ircClient, ChannelTopicDiscovered(EventMetadata(TestConstants.time), "#thegibson", null))

        assertTrue(state.topicDiscovered)
        assertEquals(ChannelTopic(), state.topic)
    }

    @Test
    fun `leaves topic state when it's discovered for a second time`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        state.topic = ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.otherTime)
        state.topicDiscovered = true
        channelStateMap += state

        handler.processEvent(ircClient, ChannelTopicDiscovered(EventMetadata(TestConstants.time), "#thegibson", "Hack the planet"))
        handler.processEvent(ircClient, ChannelTopicMetadataDiscovered(EventMetadata(TestConstants.time), "#thegibson", User("zeroCool"), TestConstants.time))

        assertTrue(state.topicDiscovered)
        assertEquals(ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.otherTime), state.topic)
    }

    @Test
    fun `updates topic state when the topic is changed`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += state

        handler.processEvent(ircClient, ChannelTopicChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "Hack the planet!"))

        assertEquals(ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.time), state.topic)
    }

    @Test
    fun `updates topic state when the topic is unset`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += state

        handler.processEvent(ircClient, ChannelTopicChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", null))

        assertEquals(ChannelTopic(null, User("acidBurn"), TestConstants.time), state.topic)
    }

    @Test
    fun `ignores topic change when channel doesn't exist`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        channelStateMap += state

        handler.processEvent(ircClient, ChannelTopicChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#dumpsterdiving", "Hack the planet!"))

        assertEquals(ChannelTopic(), state.topic)
    }

}
