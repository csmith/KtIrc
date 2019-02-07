package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.argForWhich
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UserStateHandlerTest {

    private val serverState = ServerState("", "")
    private val userState = UserState { CaseMapping.Rfc }

    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
        on { userState } doReturn userState
        on { isLocalUser(argForWhich<User> { nickname == "zeroCool" }) } doReturn true
        on { isLocalUser("zeroCool") } doReturn true
    }

    private val handler = UserStateHandler()

    @BeforeEach
    fun setUp() {
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
    }

    @Test
    fun `adds channel to user on join`() {
        userState += User("acidBurn")

        handler.processEvent(ircClient, ChannelJoined(TestConstants.time, User("acidBurn", "libby", "root.localhost"), "#thegibson"))

        assertEquals(listOf("#thegibson"), userState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `updates user info on join`() {
        userState += User("acidBurn")

        handler.processEvent(ircClient, ChannelJoined(TestConstants.time, User("acidBurn", "libby", "root.localhost"), "#thegibson"))

        val details = userState["acidBurn"]?.details!!
        assertEquals("libby", details.ident)
        assertEquals("root.localhost", details.hostname)
    }

    @Test
    fun `removes channel from user on part`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#thegibson")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("acidBurn", "libby", "root.localhost"), "#dumpsterdiving"))

        assertEquals(listOf("#thegibson"), userState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `removes user on part from last channel`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("acidBurn", "libby", "root.localhost"), "#dumpsterdiving"))

        assertNull(userState["acidBurn"])
    }

    @Test
    fun `removes channel from all users on local part`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")
        userState.addToChannel(User("acidBurn"), "#thegibson")

        userState += User("zeroCool")
        userState.addToChannel(User("zeroCool"), "#dumpsterdiving")
        userState.addToChannel(User("zeroCool"), "#thegibson")

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("zeroCool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertEquals(listOf("#thegibson"), userState["acidBurn"]?.channels?.toList())
        assertEquals(listOf("#thegibson"), userState["zeroCool"]?.channels?.toList())
    }

    @Test
    fun `removes remote users with no remaining channels on local part`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("zeroCool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertNull(userState["acidBurn"])
    }

    @Test
    fun `keeps local user with no remaining channels after local part`() {
        userState += User("zeroCool")
        userState.addToChannel(User("zeroCool"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(TestConstants.time, User("zeroCool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertNotNull(userState["zeroCool"])
    }


    @Test
    fun `removes channel from user on kick`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#thegibson")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("thePlague"), "#dumpsterdiving", "acidBurn"))

        assertEquals(listOf("#thegibson"), userState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `removes user on kick from last channel`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("thePlague"), "#dumpsterdiving", "acidBurn"))

        assertNull(userState["acidBurn"])
    }

    @Test
    fun `removes channel from all users on local kick`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")
        userState.addToChannel(User("acidBurn"), "#thegibson")

        userState += User("zeroCool")
        userState.addToChannel(User("zeroCool"), "#dumpsterdiving")
        userState.addToChannel(User("zeroCool"), "#thegibson")

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("thePlague"), "#dumpsterdiving", "zeroCool"))

        assertEquals(listOf("#thegibson"), userState["acidBurn"]?.channels?.toList())
        assertEquals(listOf("#thegibson"), userState["zeroCool"]?.channels?.toList())
    }

    @Test
    fun `removes remote users with no remaining channels on local kick`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("thePlague"), "#dumpsterdiving", "zeroCool"))

        assertNull(userState["acidBurn"])
    }

    @Test
    fun `keeps local user with no remaining channels after local kick`() {
        userState += User("zeroCool")
        userState.addToChannel(User("zeroCool"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(TestConstants.time, User("thePlague"), "#dumpsterdiving", "zeroCool"))

        assertNotNull(userState["zeroCool"])
    }

    @Test
    fun `removes user entirely on quit`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, UserQuit(TestConstants.time, User("acidBurn", "libby", "root.localhost")))

        assertNull(userState["acidBurn"])
    }

    @Test
    fun `adds users to channels on names received`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("@acidBurn")))

        assertEquals(listOf("#dumpsterdiving", "#thegibson"), userState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `updates user details on names received`() {
        userState += User("acidBurn")
        userState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelNamesReceived(TestConstants.time, "#thegibson", listOf("@acidBurn!libby@root.localhost")))

        val details = userState["acidBurn"]?.details!!
        assertEquals("libby", details.ident)
        assertEquals("root.localhost", details.hostname)
    }

    @Test
    fun `updates user info on account change`() {
        userState += User("acidBurn")

        handler.processEvent(ircClient, UserAccountChanged(TestConstants.time, User("acidBurn", "libby", "root.localhost"), "acidBurn"))

        val details = userState["acidBurn"]?.details!!
        assertEquals("acidBurn", details.account)
    }

    @Test
    fun `updates local nickname for local nick changes`() {
        val user = User("acidBurn", "libby", "root.localhost")
        whenever(ircClient.isLocalUser(user)).doReturn(true)

        handler.processEvent(ircClient, UserNickChanged(TestConstants.time, user, "acid~"))

        assertEquals("acid~", serverState.localNickname)
    }

    @Test
    fun `updates nickname for remote nick changes`() {
        val user = User("acidBurn", "libby", "root.localhost")
        userState += User("AcidBurn")

        handler.processEvent(ircClient, UserNickChanged(TestConstants.time, user, "acid~"))

        assertNotNull(userState["acid~"])
        assertNull(userState["AcidBurn"])
        assertEquals("acid~", userState["acid~"]?.details?.nickname)
    }
}
