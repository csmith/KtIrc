package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CaseInsensitiveMapTest {

    private data class Wrapper(val name: String)

    private var caseMapping = CaseMapping.Rfc
    private val map = object : CaseInsensitiveMap<Wrapper>({ caseMapping }, { it -> it.name }) {}

    @Test
    fun `CaseInsensitiveMap stores values`() {
        val value = Wrapper("acidBurn")

        map += value

        assertSame(value, map["acidBurn"])
    }

    @Test
    fun `CaseInsensitiveMap disallows the same value twice`() {
        val value = Wrapper("acidBurn")

        map += value

        assertThrows<IllegalArgumentException> {
            map += value
        }
    }

    @Test
    fun `CaseInsensitiveMap retrieves values using differently cased keys`() {
        val value = Wrapper("[acidBurn]")
        map += value

        assertSame(value, map["{ACIDBURN}"])
    }

    @Test
    fun `CaseInsensitiveMap retrieves values if the casemapping changes`() {
        val value = Wrapper("[acidBurn]")
        map += value

        caseMapping = CaseMapping.Ascii

        assertSame(value, map["[ACIDBURN]"])
        assertNull(map["{acidburn}"])
    }

    @Test
    fun `CaseInsensitiveMap retrieves null if value not found`() {
        val value = Wrapper("[acidBurn]")
        map += value

        assertNull(map["acidBurn"])
        assertNull(map["thePlague"])
    }

    @Test
    fun `CaseInsensitiveMap removes values`() {
        map += Wrapper("acidBurn")
        map -= "ACIDburn"

        assertNull(map["acidBurn"])
    }

    @Test
    fun `CaseInsensitiveMap can be iterated`() {
        map += Wrapper("acidBurn")
        map += Wrapper("zeroCool")

        val names = map.map { it.name }.toList()
        assertEquals(2, names.size)
        assertTrue(names.contains("acidBurn"))
        assertTrue(names.contains("zeroCool"))
    }

    @Test
    fun `ChannelInsensitiveMap indicates if it contains a value or not`() {
        map += Wrapper("acidBurn")

        assertTrue("acidBurn" in map)
        assertFalse("thePlague" in map)
    }

    @Test
    fun `ChannelInsensitiveMap can be cleared`() {
        map += Wrapper("acidBurn")
        map += Wrapper("zeroCool")

        map.clear()

        assertFalse("acidBurn" in map)
        assertFalse("zeroCool" in map)
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