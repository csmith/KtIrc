package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.TestConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

internal class IrcMessageTest {

    @Test
    fun `Gets UTC time from ServerTime tag if present`() {
        IrcMessage.currentTimeZoneProvider = { ZoneId.of("Z") }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1995-09-15T09:00:00.0000Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.time)
    }

    @Test
    fun `Converts time in ServerTime tag to local timezone`() {
        IrcMessage.currentTimeZoneProvider = { ZoneId.of("America/New_York") }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1995-09-15T09:00:00.0000Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T05:00:00"), message.time)
    }

    @Test
    fun `Uses current local time if no tag present`() {
        IrcMessage.currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(emptyMap(), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.time)
    }

}