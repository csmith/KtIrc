package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.BehaviourConfig
import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.ChannelNickChanged
import com.dmdirc.ktirc.events.ChannelQuit
import com.dmdirc.ktirc.events.UserNickChanged
import com.dmdirc.ktirc.events.UserQuit
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ChannelFanOutMutatorTest {

    private val mutator = ChannelFanOutMutator()
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

        val quitEvent = UserQuit(TestConstants.time, User("zerocool", "dade", "root.localhost"), "Hack the planet!")
        val events = mutator.mutateEvent(ircClient, quitEvent)

        val names = mutableListOf<String>()
        Assertions.assertEquals(3, events.size)
        Assertions.assertSame(quitEvent, events[0])
        events.subList(1, events.size).forEach { event ->
            (event as ChannelQuit).let {
                Assertions.assertEquals(TestConstants.time, it.time)
                Assertions.assertEquals("zerocool", it.user.nickname)
                Assertions.assertEquals("Hack the planet!", it.reason)
                names.add(it.channel)
            }
        }

        Assertions.assertTrue("#thegibson" in names)
        Assertions.assertTrue("#dumpsterdiving" in names)
    }

    @Test
    fun `raises ChannelNickChanged event for each channel a user changes nicks in`() {
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

        val nickEvent = UserNickChanged(TestConstants.time, User("zerocool", "dade", "root.localhost"), "zer0c00l")
        val events = mutator.mutateEvent(ircClient, nickEvent)

        val names = mutableListOf<String>()
        Assertions.assertEquals(3, events.size)
        Assertions.assertSame(nickEvent, events[0])
        events.subList(1, events.size).forEach { event ->
            (event as ChannelNickChanged).let {
                Assertions.assertEquals(TestConstants.time, it.time)
                Assertions.assertEquals("zerocool", it.user.nickname)
                Assertions.assertEquals("zer0c00l", it.newNick)
                names.add(it.channel)
            }
        }

        Assertions.assertTrue("#thegibson" in names)
        Assertions.assertTrue("#dumpsterdiving" in names)
    }
}