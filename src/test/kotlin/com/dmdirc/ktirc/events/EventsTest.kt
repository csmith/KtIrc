package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class EventsTest {

    @Test
    fun `channel joined event implements membership changed correctly`() {
        val event = ChannelJoined(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson")
        assertEquals("acidBurn", event.addedUser)
        assertNull(event.removedUser)
        assertNull(event.replacedUsers)
    }

    @Test
    fun `channel parted event implements membership changed correctly`() {
        val event = ChannelParted(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson")
        assertNull(event.addedUser)
        assertEquals("acidBurn", event.removedUser)
        assertNull(event.replacedUsers)
    }

    @Test
    fun `channel kicked event implements membership changed correctly`() {
        val event = ChannelUserKicked(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson", "zeroCool")
        assertNull(event.addedUser)
        assertEquals("zeroCool", event.removedUser)
        assertNull(event.replacedUsers)
    }

    @Test
    fun `channel quit event implements membership changed correctly`() {
        val event = ChannelQuit(EventMetadata(TestConstants.time), User("acidBurn"), "#thegibson")
        assertNull(event.addedUser)
        assertEquals("acidBurn", event.removedUser)
        assertNull(event.replacedUsers)
    }

    @Test
    fun `channel nick change implements membership changed correctly`() {
        val event = ChannelNickChanged(EventMetadata(TestConstants.time), User("zeroCool"), "#thegibson", "crashOverride")
        assertEquals("crashOverride", event.addedUser)
        assertEquals("zeroCool", event.removedUser)
        assertNull(event.replacedUsers)
    }

}