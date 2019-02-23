package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.EventMetadata
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerStateTest {

    @Test
    fun `ServerState should use the initial nickname as local nickname`() {
        val serverState = ServerState("acidBurn", "")
        assertEquals("acidBurn", serverState.localNickname)
    }

    @Test
    fun `ServerState should use the initial name as server name`() {
        val serverState = ServerState("", "the.gibson")
        assertEquals("the.gibson", serverState.serverName)
    }

    @Test
    fun `ServerState should default status to disconnected`() {
        val serverState = ServerState("acidBurn", "")
        assertEquals(ServerStatus.Disconnected, serverState.status)
    }

    @Test
    fun `returns mode type for known channel mode`() {
        val serverState = ServerState("acidBurn", "")
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")
        assertEquals(ChannelModeType.List, serverState.channelModeType('a'))
        assertEquals(ChannelModeType.SetUnsetParameter, serverState.channelModeType('d'))
        assertEquals(ChannelModeType.SetParameter, serverState.channelModeType('e'))
        assertEquals(ChannelModeType.NoParameter, serverState.channelModeType('g'))
    }

    @Test
    fun `returns whether a mode is a channel user mode or not`() {
        val serverState = ServerState("acidBurn", "")
        serverState.features[ServerFeature.ModePrefixes] = ModePrefixMapping("oqv", "@~+")
        assertTrue(serverState.isChannelUserMode('o'))
        assertTrue(serverState.isChannelUserMode('q'))
        assertTrue(serverState.isChannelUserMode('v'))
        assertFalse(serverState.isChannelUserMode('@'))
        assertFalse(serverState.isChannelUserMode('!'))
        assertFalse(serverState.isChannelUserMode('z'))
    }

    @Test
    fun `returns NoParameter for unknown channel mode`() {
        val serverState = ServerState("acidBurn", "")
        serverState.features[ServerFeature.ChannelModes] = arrayOf("ab", "cd", "ef", "gh")
        assertEquals(ChannelModeType.NoParameter, serverState.channelModeType('z'))
    }

    @Test
    fun `returns NoParameter for channel modes if feature doesn't exist`() {
        val serverState = ServerState("acidBurn", "")
        assertEquals(ChannelModeType.NoParameter, serverState.channelModeType('b'))
    }

    @Test
    fun `indicates labels are enabled when cap is present`() {
        val serverState = ServerState("acidBurn", "")
        serverState.capabilities.enabledCapabilities[Capability.LabeledResponse] = ""
        assertTrue(serverState.supportsLabeledResponses)
    }

    @Test
    fun `indicates labels are not enabled when cap is absent`() {
        val serverState = ServerState("acidBurn", "")
        assertFalse(serverState.supportsLabeledResponses)
    }

    @Test
    fun `reset clears all state`() = with(ServerState("acidBurn", "")) {
        receivedWelcome = true
        status = ServerStatus.Connecting
        localNickname = "acidBurn3"
        serverName = "root.the.gibson"
        features[ServerFeature.Network] = "gibson"
        capabilities.advertisedCapabilities["sasl"] = "sure"
        sasl.saslBuffer = "in progress"
        batches["batch"] = Batch("type", emptyList(), EventMetadata(TestConstants.time))
        labelCounter.set(100)
        labelChannels["#thegibson"] = Channel(1)

        reset()

        assertFalse(receivedWelcome)
        assertEquals(ServerStatus.Disconnected, status)
        assertEquals("acidBurn", localNickname)
        assertEquals("", serverName)
        assertTrue(features.isEmpty())
        assertTrue(capabilities.advertisedCapabilities.isEmpty())
        assertEquals("", sasl.saslBuffer)
        assertTrue(batches.isEmpty())
        assertEquals(0, labelCounter.get())
        assertTrue(labelChannels.isEmpty())
    }

}

internal class ModePrefixMappingTest {

    @Test
    fun `ModePrefixMapping identifies which chars are prefixes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertTrue(mapping.isPrefix('+'))
        assertTrue(mapping.isPrefix('@'))
        assertFalse(mapping.isPrefix('!'))
        assertFalse(mapping.isPrefix('o'))
    }

    @Test
    fun `ModePrefixMapping identifies which chars are modes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertFalse(mapping.isMode('+'))
        assertFalse(mapping.isMode('@'))
        assertFalse(mapping.isMode('!'))
        assertTrue(mapping.isMode('o'))
    }

    @Test
    fun `ModePrefixMapping maps prefixes to modes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertEquals('o', mapping.getMode('+'))
        assertEquals('a', mapping.getMode('@'))
        assertEquals('v', mapping.getMode('-'))
    }

    @Test
    fun `ModePrefixMapping maps prefix strings to modes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertEquals("oa", mapping.getModes("+@"))
        assertEquals("o", mapping.getModes("+"))
        assertEquals("", mapping.getModes(""))
        assertEquals("vao", mapping.getModes("-@+"))
    }

}