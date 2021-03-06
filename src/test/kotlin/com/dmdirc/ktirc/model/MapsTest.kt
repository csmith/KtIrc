package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CaseInsensitiveMapTest {

    private data class Wrapper(val name: String)

    private var caseMapping = CaseMapping.Rfc
    private val map = object : CaseInsensitiveMap<Wrapper>({ caseMapping }, { it -> it.name }) {}

    @Test
    fun `stores values`() {
        val value = Wrapper("acidBurn")

        map += value

        assertSame(value, map["acidBurn"])
    }

    @Test
    fun `ignores the same value twice`() {
        val value = Wrapper("acidBurn")

        map += value
        map += value

        assertSame(value, map["acidBurn"])
        assertEquals(1, map.count())
    }

    @Test
    fun `retrieves values using differently cased keys`() {
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

    @Test
    fun `removeIf removes matching items`() {
        map += Wrapper("acidBurn")
        map += Wrapper("zeroCool")
        map += Wrapper("thePlague")

        map.removeIf { it.name.length == 8 }

        assertEquals(1, map.count())
        assertTrue("thePlague" in map)
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

internal class UserMapTest {

    @Test
    fun `UserMap maps users on nickname`() {
        val userMap = UserMap { CaseMapping.Rfc }
        userMap += KnownUser({ CaseMapping.Rfc }, User("acidBurn"))
        assertTrue("acidBurn" in userMap)
        assertTrue("acidburn" in userMap)
        assertFalse("zerocool" in userMap)
    }

}