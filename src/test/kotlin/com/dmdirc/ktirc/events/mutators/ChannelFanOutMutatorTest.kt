package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.BehaviourConfig
import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChannelFanOutMutatorTest {

    private val mutator = ChannelFanOutMutator()
    private val fakeServerState = ServerState("", "")
    private val fakeChannelStateMap = ChannelStateMap { CaseMapping.Rfc }
    private val fakeBehaviour = BehaviourConfig()
    private val messageEmitter = mockk<MessageEmitter>()
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
        every { channelState } returns fakeChannelStateMap
        every { behaviour } returns fakeBehaviour
        every { isLocalUser(any<User>()) } answers { arg<User>(0) == User("acidburn", "libby", "root.localhost") }
        every { isLocalUser(any<String>()) } answers { arg<String>(0) == "acidburn" }
    }

    @Test
    fun `raises ChannelQuit event for each channel a user quits from`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelStateMap += this
        }

        with (ChannelState("#dumpsterdiving") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelStateMap += this
        }

        with (ChannelState("#chat") { CaseMapping.Rfc }) {
            users += ChannelUser("AcidBurn")
            fakeChannelStateMap += this
        }

        val quitEvent = UserQuit(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "Hack the planet!")
        val events = mutator.mutateEvent(ircClient, messageEmitter, quitEvent)

        val names = mutableListOf<String>()
        assertEquals(3, events.size)
        assertSame(quitEvent, events[0])
        events.subList(1, events.size).forEach { event ->
            (event as ChannelQuit).let {
                assertEquals(TestConstants.time, it.metadata.time)
                assertEquals("zerocool", it.user.nickname)
                assertEquals("Hack the planet!", it.reason)
                names.add(it.target)
            }
        }

        assertTrue("#thegibson" in names)
        assertTrue("#dumpsterdiving" in names)
    }

    @Test
    fun `raises ChannelAway event for each channel a user shares when away`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelStateMap += this
        }

        with (ChannelState("#dumpsterdiving") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelStateMap += this
        }

        with (ChannelState("#chat") { CaseMapping.Rfc }) {
            users += ChannelUser("AcidBurn")
            fakeChannelStateMap += this
        }

        val quitEvent = UserAway(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "Hack the planet!")
        val events = mutator.mutateEvent(ircClient, messageEmitter, quitEvent)

        val names = mutableListOf<String>()
        assertEquals(3, events.size)
        assertSame(quitEvent, events[0])
        events.subList(1, events.size).forEach { event ->
            (event as ChannelAway).let {
                assertEquals(TestConstants.time, it.metadata.time)
                assertEquals("zerocool", it.user.nickname)
                assertEquals("Hack the planet!", it.message)
                names.add(it.target)
            }
        }

        assertTrue("#thegibson" in names)
        assertTrue("#dumpsterdiving" in names)
    }

    @Test
    fun `raises ChannelNickChanged event for each channel a user changes nicks in`() {
        with (ChannelState("#thegibson") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelStateMap += this
        }

        with (ChannelState("#dumpsterdiving") { CaseMapping.Rfc }) {
            users += ChannelUser("ZeroCool")
            fakeChannelStateMap += this
        }

        with (ChannelState("#chat") { CaseMapping.Rfc }) {
            users += ChannelUser("AcidBurn")
            fakeChannelStateMap += this
        }

        val nickEvent = UserNickChanged(EventMetadata(TestConstants.time), User("zerocool", "dade", "root.localhost"), "zer0c00l")
        val events = mutator.mutateEvent(ircClient, messageEmitter, nickEvent)

        val names = mutableListOf<String>()
        assertEquals(3, events.size)
        assertSame(nickEvent, events[0])
        events.subList(1, events.size).forEach { event ->
            (event as ChannelNickChanged).let {
                Assertions.assertEquals(TestConstants.time, it.metadata.time)
                Assertions.assertEquals("zerocool", it.user.nickname)
                Assertions.assertEquals("zer0c00l", it.newNick)
                names.add(it.target)
            }
        }

        assertTrue("#thegibson" in names)
        assertTrue("#dumpsterdiving" in names)
    }
}