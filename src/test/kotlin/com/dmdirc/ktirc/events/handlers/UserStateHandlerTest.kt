package com.dmdirc.ktirc.events.handlers

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UserStateHandlerTest {

    private val fakeServerState = ServerState("", "")
    private val fakeUserState = UserState { CaseMapping.Rfc }

    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
        every { userState } returns fakeUserState
        every { isLocalUser(any<User>()) } answers { arg<User>(0).nickname == "zeroCool" }
        every { isLocalUser(any<String>()) } answers { arg<String>(0) == "zeroCool" }
    }

    private val handler = UserStateHandler()

    @BeforeEach
    fun setUp() {
        fakeServerState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("ov", "@+")
    }

    @Test
    fun `adds channel to user on join`() {
        fakeUserState += User("acidBurn")

        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("acidBurn", "libby", "root.localhost"), "#thegibson"))

        assertEquals(listOf("#thegibson"), fakeUserState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `updates user info on join`() {
        fakeUserState += User("acidBurn")

        handler.processEvent(ircClient, ChannelJoined(EventMetadata(TestConstants.time), User("acidBurn", "libby", "root.localhost"), "#thegibson"))

        val details = fakeUserState["acidBurn"]?.details!!
        assertEquals("libby", details.ident)
        assertEquals("root.localhost", details.hostname)
    }

    @Test
    fun `removes channel from user on part`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#thegibson")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("acidBurn", "libby", "root.localhost"), "#dumpsterdiving"))

        assertEquals(listOf("#thegibson"), fakeUserState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `removes user on part from last channel`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("acidBurn", "libby", "root.localhost"), "#dumpsterdiving"))

        assertNull(fakeUserState["acidBurn"])
    }

    @Test
    fun `removes channel from all users on local part`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")
        fakeUserState.addToChannel(User("acidBurn"), "#thegibson")

        fakeUserState += User("zeroCool")
        fakeUserState.addToChannel(User("zeroCool"), "#dumpsterdiving")
        fakeUserState.addToChannel(User("zeroCool"), "#thegibson")

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("zeroCool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertEquals(listOf("#thegibson"), fakeUserState["acidBurn"]?.channels?.toList())
        assertEquals(listOf("#thegibson"), fakeUserState["zeroCool"]?.channels?.toList())
    }

    @Test
    fun `removes remote users with no remaining channels on local part`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("zeroCool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertNull(fakeUserState["acidBurn"])
    }

    @Test
    fun `keeps local user with no remaining channels after local part`() {
        fakeUserState += User("zeroCool")
        fakeUserState.addToChannel(User("zeroCool"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelParted(EventMetadata(TestConstants.time), User("zeroCool", "dade", "root.localhost"), "#dumpsterdiving"))

        assertNotNull(fakeUserState["zeroCool"])
    }


    @Test
    fun `removes channel from user on kick`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#thegibson")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("thePlague"), "#dumpsterdiving", "acidBurn"))

        assertEquals(listOf("#thegibson"), fakeUserState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `removes user on kick from last channel`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("thePlague"), "#dumpsterdiving", "acidBurn"))

        assertNull(fakeUserState["acidBurn"])
    }

    @Test
    fun `removes channel from all users on local kick`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")
        fakeUserState.addToChannel(User("acidBurn"), "#thegibson")

        fakeUserState += User("zeroCool")
        fakeUserState.addToChannel(User("zeroCool"), "#dumpsterdiving")
        fakeUserState.addToChannel(User("zeroCool"), "#thegibson")

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("thePlague"), "#dumpsterdiving", "zeroCool"))

        assertEquals(listOf("#thegibson"), fakeUserState["acidBurn"]?.channels?.toList())
        assertEquals(listOf("#thegibson"), fakeUserState["zeroCool"]?.channels?.toList())
    }

    @Test
    fun `removes remote users with no remaining channels on local kick`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("thePlague"), "#dumpsterdiving", "zeroCool"))

        assertNull(fakeUserState["acidBurn"])
    }

    @Test
    fun `keeps local user with no remaining channels after local kick`() {
        fakeUserState += User("zeroCool")
        fakeUserState.addToChannel(User("zeroCool"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelUserKicked(EventMetadata(TestConstants.time), User("thePlague"), "#dumpsterdiving", "zeroCool"))

        assertNotNull(fakeUserState["zeroCool"])
    }

    @Test
    fun `removes user entirely on quit`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, UserQuit(EventMetadata(TestConstants.time), User("acidBurn", "libby", "root.localhost")))

        assertNull(fakeUserState["acidBurn"])
    }

    @Test
    fun `adds users to channels on names received`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@acidBurn")))

        assertEquals(listOf("#dumpsterdiving", "#thegibson"), fakeUserState["acidBurn"]?.channels?.toList())
    }

    @Test
    fun `updates user details on names received`() {
        fakeUserState += User("acidBurn")
        fakeUserState.addToChannel(User("acidBurn"), "#dumpsterdiving")

        handler.processEvent(ircClient, ChannelNamesReceived(EventMetadata(TestConstants.time), "#thegibson", listOf("@acidBurn!libby@root.localhost")))

        val details = fakeUserState["acidBurn"]?.details!!
        assertEquals("libby", details.ident)
        assertEquals("root.localhost", details.hostname)
    }

    @Test
    fun `updates user info on account change`() {
        fakeUserState += User("acidBurn")

        handler.processEvent(ircClient, UserAccountChanged(EventMetadata(TestConstants.time), User("acidBurn", "libby", "root.localhost"), "acidBurn"))

        val details = fakeUserState["acidBurn"]?.details!!
        assertEquals("acidBurn", details.account)
    }

    @Test
    fun `updates local nickname for local nick changes`() {
        val user = User("zeroCool", "dade", "root.localhost")

        handler.processEvent(ircClient, UserNickChanged(EventMetadata(TestConstants.time), user, "crashOverride"))

        assertEquals("crashOverride", fakeServerState.localNickname)
    }

    @Test
    fun `updates nickname for remote nick changes`() {
        val user = User("acidBurn", "libby", "root.localhost")
        fakeUserState += User("AcidBurn")

        handler.processEvent(ircClient, UserNickChanged(EventMetadata(TestConstants.time), user, "acid~"))

        assertNotNull(fakeUserState["acid~"])
        assertNull(fakeUserState["AcidBurn"])
        assertEquals("acid~", fakeUserState["acid~"]?.details?.nickname)
    }

    @Test
    fun `updates details for remote host changes`() {
        val user = User("acidBurn", "libby", "root.localhost")
        fakeUserState += User("AcidBurn")

        handler.processEvent(ircClient, UserHostChanged(EventMetadata(TestConstants.time), user, "burn", "root.gibson"))

        assertEquals("burn", fakeUserState["acidBurn"]?.details?.ident)
        assertEquals("root.gibson", fakeUserState["acidBurn"]?.details?.hostname)
    }

    @Test
    fun `updates details for user back events`() {
        val user = User("acidBurn", "libby", "root.localhost")
        fakeUserState += User("AcidBurn", awayMessage = "Hacking the planet")

        handler.processEvent(ircClient, UserAway(EventMetadata(TestConstants.time), user, null))

        assertNull(fakeUserState["acidBurn"]?.details?.awayMessage)
    }

    @Test
    fun `does not update away details for less detailed events`() {
        val user = User("acidBurn", "libby", "root.localhost")
        fakeUserState += User("AcidBurn", awayMessage = "Hacking the planet")

        handler.processEvent(ircClient, UserAway(EventMetadata(TestConstants.time), user, ""))

        assertEquals("Hacking the planet", fakeUserState["acidBurn"]?.details?.awayMessage)
    }

    @Test
    fun `updates away details for away message`() {
        val user = User("acidBurn", "libby", "root.localhost")
        fakeUserState += User("AcidBurn")

        handler.processEvent(ircClient, UserAway(EventMetadata(TestConstants.time), user, "Hacking the planet"))

        assertEquals("Hacking the planet", fakeUserState["acidBurn"]?.details?.awayMessage)
    }

}
