package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChannelStateTest {

    @Test
    fun `ChannelModeType identifies which modes need parameters to set`() {
        assertTrue(ChannelModeType.List.needsParameterToSet)
        assertTrue(ChannelModeType.SetUnsetParameter.needsParameterToSet)
        assertTrue(ChannelModeType.SetParameter.needsParameterToSet)
        assertFalse(ChannelModeType.NoParameter.needsParameterToSet)
    }

    @Test
    fun `ChannelModeType identifies which modes need parameters to unset`() {
        assertTrue(ChannelModeType.List.needsParameterToUnset)
        assertTrue(ChannelModeType.SetUnsetParameter.needsParameterToUnset)
        assertFalse(ChannelModeType.SetParameter.needsParameterToUnset)
        assertFalse(ChannelModeType.NoParameter.needsParameterToUnset)
    }

    @Test
    fun `reset resets all state`() = with(ChannelState("#thegibson") { CaseMapping.Rfc }) {
        receivingUserList = true
        modesDiscovered = true
        topicDiscovered = true
        modes['a'] = "b"
        users += ChannelUser("acidBurn")
        topic = ChannelTopic("Hack the planet!")

        reset()

        assertFalse(receivingUserList)
        assertFalse(modesDiscovered)
        assertFalse(topicDiscovered)
        assertTrue(modes.isEmpty())
        assertEquals(0, users.count())
        assertEquals(ChannelTopic(), topic)
    }

}
