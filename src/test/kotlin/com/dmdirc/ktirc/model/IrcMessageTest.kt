package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

internal class IrcMessageTest {

    @Test
    fun `Gets UTC time from ServerTime tag if present`() {
        currentTimeZoneProvider = { ZoneId.of("Z") }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1995-09-15T09:00:00.0000Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.time)
    }

    @Test
    fun `Converts time in ServerTime tag to local timezone`() {
        currentTimeZoneProvider = { ZoneId.of("America/New_York") }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1995-09-15T09:00:00.0000Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T05:00:00"), message.time)
    }

    @Test
    fun `Uses current local time if no tag present`() {
        currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(emptyMap(), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.time)
    }

    @Test
    fun `Can parse the prefix as a source user`() {
        val message = IrcMessage(emptyMap(), "acidBurn!libby@root.localhost".toByteArray(), "", emptyList())
        val user = message.sourceUser!!

        assertEquals("acidBurn", user.nickname)
        assertEquals("libby", user.ident)
        assertEquals("root.localhost", user.hostname)
        assertNull(user.account)
    }

    @Test
    fun `Uses account-name tag when creating a source user`() {
        val message = IrcMessage(hashMapOf(MessageTag.AccountName to "acidBurn"), "acidBurn!libby@root.localhost".toByteArray(), "", emptyList())
        val user = message.sourceUser!!

        assertEquals("acidBurn", user.nickname)
        assertEquals("libby", user.ident)
        assertEquals("root.localhost", user.hostname)
        assertEquals("acidBurn", user.account)
    }

}