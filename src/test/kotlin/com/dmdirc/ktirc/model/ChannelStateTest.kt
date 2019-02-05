package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

}
