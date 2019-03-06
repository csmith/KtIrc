package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.BehaviourConfig
import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChannelStateHandlerTest {

    private val handler = ChannelStateHandler()
    private val fakeChannelState = ChannelStateMap { CaseMapping.Rfc }
    private val fakeServerState = ServerState("", "")
    private val behaviourConfig = BehaviourConfig()
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
        every { channelState } returns fakeChannelState
        every { behaviour } returns behaviourConfig
        every { isLocalUser(any<User>()) } answers { arg<User>(0) == User("acidburn", "libby", "root.localhost") }
        every { isLocalUser(any<String>()) } answers { arg<String>(0) == "acidburn" }
    }

    @Test
    fun `creates new state object for local joins`() {
        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson"))
        assertTrue("#thegibson" in fakeChannelState)
    }

    @Test
    fun `does not create new state object for remote joins`() {
        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))
        assertFalse("#thegibson" in fakeChannelState)
    }

    @Test
    fun `adds joiners to channel state`() {
        fakeChannelState += ChannelState("#thegibson") { CaseMapping.Rfc }

        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertTrue("zerocool" in fakeChannelState["#thegibson"]?.users!!)
    }

    @Test
    fun `ignores duplicate joiners`() {
        fakeChannelState += ChannelState("#thegibson") { CaseMapping.Rfc }

        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))
        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertTrue("zerocool" in fakeChannelState["#thegibson"]?.users!!)
        assertEquals(1, fakeChannelState["#thegibson"]?.users?.count())
    }

    @Test
    fun `clears existing users when getting a new list`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("acidBurn")
        channel.users += ChannelUser("thePlague")
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("zeroCool")))

        assertEquals(1, channel.users.count())
        assertNotNull(channel.users["zeroCool"])
    }

    @Test
    fun `adds users from multiple name received events`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel

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
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("acidBurn")))
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("thePlague")))

        assertEquals(2, channel.users.count())
        assertNotNull(channel.users["acidBurn"])
        assertNotNull(channel.users["thePlague"])
    }

    @Test
    fun `updates replacedUsers property of names finished event`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel

        val event = ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson")
        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("zeroCool")))
        handler.processEvent(ircClient, event)

        assertEquals(1, event.replacedUsers?.size)
        assertEquals("zeroCool", event.replacedUsers?.get(0))
    }

    @Test
    fun `adds users with mode prefixes`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

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
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@zeroCool!dade@root.localhost", "+acidBurn!libby@root.localhost")))
        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        assertEquals(2, channel.users.count())
        assertEquals("o", channel.users["zeroCool"]?.modes)
        assertEquals("v", channel.users["acidBurn"]?.modes)
    }

    @Test
    fun `updates receiving user list state`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@zeroCool!dade@root.localhost", "+acidBurn!libby@root.localhost")))

        assertTrue(channel.receivingUserList)

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        assertFalse(channel.receivingUserList)
    }

    @Test
    fun `requests modes on end of names if configured and undiscovered`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
        behaviourConfig.requestModesOnJoin = true

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        verify {
            ircClient.send("MODE", "#thegibson")
        }
    }

    @Test
    fun `does not request modes on end of names if already discovered`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
        behaviourConfig.requestModesOnJoin = true
        channel.modesDiscovered = true

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        verify(inverse = true) {
            ircClient.send("MODE", "#thegibson")
        }
    }

    @Test
    fun `does not request modes on end of names if not configured`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
        behaviourConfig.requestModesOnJoin = false

        handler.processEvent(ircClient, ChannelNamesFinished(EventMetadata(TestConstants.time), "#thegibson"))

        verify(inverse = true) {
            ircClient.send("MODE", "#thegibson")
        }
    }

    @Test
    fun `removes state object for local parts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson"))

        assertFalse("#thegibson" in fakeChannelState)
    }

    @Test
    fun `removes user from channel member list for remote parts`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `removes state object for local kicks`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson", "acidburn", "Bye!"))

        assertFalse("#thegibson" in fakeChannelState)
    }

    @Test
    fun `removes user from channel member list for remote kicks`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("ZeroCool")
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson", "zerocool", "Bye!"))

        assertFalse("zerocool" in channel.users)
    }

    @Test
    fun `removes user from channel member lists for quits`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelState += this
        }

        with (ChannelState("#dumpsterdiving") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelState += this
        }

        with (ChannelState("#chat") { CaseMapping.Rfc }) {
            users += ChannelUser("AcidBurn")
            fakeChannelState += this
        }

        handler.processEvent(ircClient, ChannelQuit(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#thegibson"))
        handler.processEvent(ircClient, ChannelQuit(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertFalse("zerocool" in fakeChannelState["#thegibson"]!!.users)
        assertFalse("zerocool" in fakeChannelState["#dumpsterdiving"]!!.users)
        assertFalse("zerocool" in fakeChannelState["#chat"]!!.users)
        assertTrue("acidburn" in fakeChannelState["#chat"]!!.users)
    }

    @Test
    fun `renames user in channel member list for nick changes`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        channel.users += ChannelUser("acidBurn")
        fakeChannelState += channel

        handler.processEvent(ircClient, ChannelNickChanged(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#thegibson", "acidB"))
        handler.processEvent(ircClient, ChannelNickChanged(EventMetadata(TestConstants.time), User("acidburn", "libby", "root.localhost"), "#dumpsterdiving", "acidB"))

        assertFalse("acidBurn" in channel.users)
        assertTrue("acidB" in channel.users)
        assertEquals("acidB", channel.users["acidB"]?.nickname)
    }

    @Test
    fun `sets mode discovered flag when discovered mode event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "+", emptyArray(), true))

        assertTrue(channel.modesDiscovered)
    }

    @Test
    fun `adds modes when discovered mode event received`() {
        val channel = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "+ceg", arrayOf("CCC", "EEE"), true))

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
        fakeChannelState += channel
        fakeServerState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "-c+d-eh+fg", arrayOf("CCC", "DDD", "FFF"), true))

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
            fakeChannelState += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "+o", arrayOf("zeroCool")))

        assertEquals("o", fakeChannelState["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles privileged user gaining lesser mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "o")
            fakeChannelState += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "+v", arrayOf("zeroCool")))

        assertEquals("ov", fakeChannelState["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles privileged user gaining greater mode`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "v")
            fakeChannelState += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "+o", arrayOf("zeroCool")))

        assertEquals("ov", fakeChannelState["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles user gaining multiple modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelState += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "+vo", arrayOf("zeroCool", "zeroCool")))

        assertEquals("ov", fakeChannelState["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles user losing multiple modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "ov")
            fakeChannelState += this
        }

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "-vo", arrayOf("zeroCool", "zeroCool")))

        assertEquals("", fakeChannelState["#thegibson"]?.users?.get("zeroCool")?.modes)
    }

    @Test
    fun `handles mixture of user modes and normal modes`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool", "v")
            fakeChannelState += this
        }
        fakeServerState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")

        handler.processEvent(ircClient, ModeChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "oa-v+b", arrayOf("zeroCool", "aaa", "zeroCool", "bbb")))

        assertEquals("o", fakeChannelState["#thegibson"]?.users?.get("zeroCool")?.modes)
        assertEquals("aaa", fakeChannelState["#thegibson"]?.modes?.get('a'))
        assertEquals("bbb", fakeChannelState["#thegibson"]?.modes?.get('b'))
    }

    @Test
    fun `updates topic state when it's discovered for the first time`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += state

        handler.processEvent(ircClient, ChannelTopicDiscovered(EventMetadata(TestConstants.time), "#thegibson", "Hack the planet!"))
        handler.processEvent(ircClient, ChannelTopicMetadataDiscovered(EventMetadata(TestConstants.time), "#thegibson", User("acidBurn"), TestConstants.otherTime))

        assertTrue(state.topicDiscovered)
        assertEquals(ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.otherTime), state.topic)
    }

    @Test
    fun `updates topic state when no topic is discovered for the first time`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += state

        handler.processEvent(ircClient, ChannelTopicDiscovered(EventMetadata(TestConstants.time), "#thegibson", null))

        assertTrue(state.topicDiscovered)
        assertEquals(ChannelTopic(), state.topic)
    }

    @Test
    fun `leaves topic state when it's discovered for a second time`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        state.topic = ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.otherTime)
        state.topicDiscovered = true
        fakeChannelState += state

        handler.processEvent(ircClient, ChannelTopicDiscovered(EventMetadata(TestConstants.time), "#thegibson", "Hack the planet"))
        handler.processEvent(ircClient, ChannelTopicMetadataDiscovered(EventMetadata(TestConstants.time), "#thegibson", User("zeroCool"), TestConstants.time))

        assertTrue(state.topicDiscovered)
        assertEquals(ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.otherTime), state.topic)
    }

    @Test
    fun `updates topic state when the topic is changed`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += state

        handler.processEvent(ircClient, ChannelTopicChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "Hack the planet!"))

        assertEquals(ChannelTopic("Hack the planet!", User("acidBurn"), TestConstants.time), state.topic)
    }

    @Test
    fun `updates topic state when the topic is unset`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += state

        handler.processEvent(ircClient, ChannelTopicChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", null))

        assertEquals(ChannelTopic(null, User("acidBurn"), TestConstants.time), state.topic)
    }

    @Test
    fun `ignores topic change when channel doesn't exist`() {
        val state = ChannelState("#thegibson") { CaseMapping.Rfc }
        fakeChannelState += state

        handler.processEvent(ircClient, ChannelTopicChanged(EventMetadata(TestConstants.time), User("acidBurn"), "#dumpsterdiving", "Hack the planet!"))

        assertEquals(ChannelTopic(), state.topic)
    }

}
