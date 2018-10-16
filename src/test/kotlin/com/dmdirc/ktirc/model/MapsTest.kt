package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CaseInsensitiveMapTest {

    private var caseMapping = CaseMapping.Rfc
    private val map = object : CaseInsensitiveMap<String>({ caseMapping }, { str -> str.substring(0, 4) }) {}

    @Test
    fun `CaseInsensitiveMap stores values`() {
        val value = "acidBurn"

        map += value

        assertSame(value, map["acid"])
    }

    @Test
    fun `CaseInsensitiveMap disallows the same value twice`() {
        val value = "acidBurn"

        map += value

        assertThrows<IllegalArgumentException> {
            map += value
        }
    }

    @Test
    fun `CaseInsensitiveMap retrieves values using differently cased keys`() {
        val value = "[acidBurn]"
        map += value

        assertSame(value, map["{ACI"])
    }

    @Test
    fun `CaseInsensitiveMap retrieves values if the casemapping changes`() {
        val value = "[acidBurn]"
        map += value

        caseMapping = CaseMapping.Ascii

        assertSame(value, map["[ACI"])
        assertNull(map["{aci"])
    }

    @Test
    fun `CaseInsensitiveMap retrieves null if value not found`() {
        val value = "[acidBurn]"
        map += value

        assertNull(map["acidBurn"])
        assertNull(map["thePlague"])
    }

    @Test
    fun `CaseInsensitiveMap removes values`() {
        map += "acidBurn"
        map -= "ACIDburn"

        assertNull(map["acidBurn"])
        assertNull(map["ACIDburn"])
    }

    @Test
    fun `CaseInsensitiveMap can be iterated`() {
        map += "acidBurn"
        map += "zeroCool"

        val names = map.toList()
        assertEquals(2, names.size)
        assertTrue(names.contains("acidBurn"))
        assertTrue(names.contains("zeroCool"))
    }

    @Test
    fun `ChannelInsensitiveMap indicates if it contains a value or not`() {
        map += "acidBurn"

        assertTrue("acid" in map)
        assertFalse("theP" in map)
    }

    @Test
    fun `ChannelInsensitiveMap can be cleared`() {
        map += "acidBurn"
        map += "zeroCool"

        map.clear()

        assertFalse("acid" in map)
        assertFalse("zero" in map)
        assertEquals(0, map.count())
    }

}

internal class ChannelStateMapTest {

    @Test
    fun `ChannelStateMap maps channels on name`() {
        val channelUserMap = ChannelStateMap { CaseMapping.Rfc }
        channelUserMap += ChannelState("#dumpsterDiving") { CaseMapping.Rfc }
        assertTrue("#dumpsterDiving" in channelUserMap)
        assertTrue("#dumpsterdiving" in channelUserMap)
        assertFalse("#thegibson" in channelUserMap)
    }

}

internal class ChannelUserMapTest {

    @Test
    fun `ChannelUserMap maps users on nickname`() {
        val channelUserMap = ChannelUserMap { CaseMapping.Rfc }
        channelUserMap += ChannelUser("acidBurn")
        assertTrue("acidBurn" in channelUserMap)
        assertTrue("acidburn" in channelUserMap)
        assertFalse("zerocool" in channelUserMap)
    }

}