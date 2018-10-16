package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ChannelStateMapTest {

    @Test
    fun `ChannelStateMap stores channel state`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
        val channelState = ChannelState("#dumpsterdiving")
        channelStateMap += channelState

        assertSame(channelState, channelStateMap["#dumpsterdiving"])
    }

    @Test
    fun `ChannelStateMap disallows setting the same channel twice`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
        channelStateMap += ChannelState("#dumpsterdiving")

        assertThrows<IllegalArgumentException> {
            channelStateMap += ChannelState("#DumpsterDiving")
        }
    }

    @Test
    fun `ChannelStateMap retrieves channels in different cases`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
        val channelState = ChannelState("#dumpsterdiving[]")
        channelStateMap += channelState

        assertSame(channelState, channelStateMap["#dumpsterdiving{}"])
    }

    @Test
    fun `ChannelStateMap returns null if channel not found`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Ascii }
        val channelState = ChannelState("#dumpsterdiving[]")
        channelStateMap += channelState

        assertNull(channelStateMap["#dumpsterdiving{}"])
    }

    @Test
    fun `ChannelStateMap removes channels`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
        val channelState = ChannelState("#dumpsterdiving")
        channelStateMap += channelState
        channelStateMap -= ChannelState("#dumpsterDIVING")

        assertNull(channelStateMap["#dumpsterdiving"])
    }

    @Test
    fun `ChannelStateMap can be iterated`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
        channelStateMap += ChannelState("#dumpsterdiving")
        channelStateMap += ChannelState("#gibson")

        val names = channelStateMap.map { it.name }.toList()
        assertEquals(2, names.size)
        assertTrue(names.contains("#dumpsterdiving"))
        assertTrue(names.contains("#gibson"))
    }

    @Test
    fun `ChannelStateMap indicates if it contains a channel or not`() {
        val channelStateMap = ChannelStateMap { CaseMapping.Rfc }
        channelStateMap += ChannelState("#dumpsterdiving")

        assertTrue("#dumpsterDIVING" in channelStateMap)
        assertFalse("#crashandburn" in channelStateMap)
    }

}