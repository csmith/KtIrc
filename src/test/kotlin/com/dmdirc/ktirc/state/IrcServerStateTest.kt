package com.dmdirc.ktirc.state

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

internal class IrcServerStateTest {

    @Test
    fun `IrcServerState should return defaults for unspecified features`() {
        val serverState = IrcServerState("")
        assertEquals(200, serverState.getFeature(ServerFeature.MaximumChannelNameLength))
    }

    @Test
    fun `IrcServerState should return null for unspecified features with no default`() {
        val serverState = IrcServerState("")
        assertNull(serverState.getFeature(ServerFeature.ChannelModes))
    }

    @Test
    fun `IrcServerState should return previously set value for features`() {
        val serverState = IrcServerState("")
        serverState.setFeature(ServerFeature.MaximumChannels, 123)
        assertEquals(123, serverState.getFeature(ServerFeature.MaximumChannels))
    }

    @Test
    fun `IrcServerState should return default set value for features that were reset`() {
        val serverState = IrcServerState("")
        serverState.setFeature(ServerFeature.MaximumChannels, 123)
        serverState.resetFeature(ServerFeature.MaximumChannels)
        assertNull(serverState.getFeature(ServerFeature.MaximumChannels))
    }

    @Test
    fun `IrcServerState should throw if a feature is set with the wrong type`() {
        val serverState = IrcServerState("")
        assertThrows(IllegalArgumentException::class.java) {
            serverState.setFeature(ServerFeature.MaximumChannels, "123")
        }
    }

    @Test
    fun `IrcServerState should use the initial nickname as local nickname`() {
        val serverState = IrcServerState("acidBurn")
        assertEquals("acidBurn", serverState.localNickname)
    }

}