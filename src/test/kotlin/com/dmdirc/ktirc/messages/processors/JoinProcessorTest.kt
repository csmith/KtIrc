package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.ChannelJoinFailed
import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JoinProcessorTest {

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises join event`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "JOIN", params("#crashandburn")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoined
        assertEquals(User("acidburn", "libby", "root.localhost"), event.user)
        assertEquals("#crashandburn", event.target)
    }

    @Test
    fun `does nothing if prefix missing`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), null, "JOIN", params("#crashandburn")))
        assertEquals(0, events.size)
    }

    @Test
    fun `adds real name and account from extended join`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "JOIN", params("#crashandburn", "acidBurn", "Libby")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoined
        assertEquals(User("acidburn", "libby", "root.localhost", account = "acidBurn", realName = "Libby"), event.user)
        assertEquals("#crashandburn", event.target)
    }

    @Test
    fun `ignores account if the user is not authed`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "JOIN", params("#crashandburn", "*", "Libby")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoined
        assertEquals(User("acidburn", "libby", "root.localhost", realName = "Libby"), event.user)
        assertEquals("#crashandburn", event.target)
    }

    @Test
    fun `raises join failed if too many channels reached`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "405", params("acidBurn", "#thegibson", "You have joined too many channels")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.TooManyChannels, event.reason)
    }

    @Test
    fun `raises join failed if hiding not allowed`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "459", params("acidBurn", "#thegibson", "You are not allowed to hide")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NoHiding, event.reason)
    }

    @Test
    fun `raises join failed if channel is full`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "471", params("acidBurn", "#thegibson", "Channel is full")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.ChannelFull, event.reason)
    }

    @Test
    fun `raises join failed if channel is invite only`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "473", params("acidBurn", "#thegibson", "Channel is invite only")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NeedInvite, event.reason)
    }

    @Test
    fun `raises join failed if banned`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "474", params("acidBurn", "#thegibson", "You are banned")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.Banned, event.reason)
    }

    @Test
    fun `raises join failed if channel is keyed`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "475", params("acidBurn", "#thegibson", "Bad key (+k)")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NeedKey, event.reason)
    }

    @Test
    fun `raises join failed if need a registered nickname`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "477", params("acidBurn", "#thegibson", "Must be registered")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NeedRegisteredNick, event.reason)
    }

    @Test
    fun `raises join failed if bad channel name`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "479", params("acidBurn", "#thegibson", "Bad channel name")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.BadChannelName, event.reason)
    }

    @Test
    fun `raises join failed if throttled`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "480", params("acidBurn", "#thegibson", "You are throttled")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.Throttled, event.reason)
    }

    @Test
    fun `raises join failed if secure only`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "489", params("acidBurn", "#thegibson", "You must use a secure connection")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NeedTls, event.reason)
    }

    @Test
    fun `raises join failed if too many joins`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "500", params("acidBurn", "#thegibson", "Too many joins")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.Throttled, event.reason)
    }

    @Test
    fun `raises join failed if admin onnly`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "519", params("acidBurn", "#thegibson", "Admins only")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NeedAdmin, event.reason)
    }

    @Test
    fun `raises join failed if oper only`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "520", params("acidBurn", "#thegibson", "Opers only")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.NeedOper, event.reason)
    }

    @Test
    fun `raises join failed if unknown numeric`() {
        val events = JoinProcessor().process(IrcMessage(emptyMap(), null, "999", params("acidBurn", "#thegibson", "Weird")))
        assertEquals(1, events.size)

        assertEquals(TestConstants.time, events[0].metadata.time)
        val event = events[0] as ChannelJoinFailed
        assertEquals("#thegibson", event.target)
        assertEquals(ChannelJoinFailed.JoinError.Unknown, event.reason)
    }

}