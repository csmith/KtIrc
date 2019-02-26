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
    fun `gets UTC time from ServerTime tag if present`() {
        currentTimeZoneProvider = { ZoneId.of("Z") }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1995-09-15T09:00:00.0000Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.metadata.time)
    }

    @Test
    fun `converts time in ServerTime tag to local timezone`() {
        currentTimeZoneProvider = { ZoneId.of("America/New_York") }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1995-09-15T09:00:00.0000Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T05:00:00"), message.metadata.time)
    }

    @Test
    fun `uses current time if ServerTime tag is malformed`() {
        currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(hashMapOf(MessageTag.ServerTime to "1996-05-03T13:00:00.***Z"), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.metadata.time)
    }

    @Test
    fun `uses current local time if no tag present`() {
        currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(emptyMap(), null, "", emptyList())
        assertEquals(LocalDateTime.parse("1995-09-15T09:00:00"), message.metadata.time)
    }

    @Test
    fun `populates batch field if present`() {
        currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(hashMapOf(MessageTag.Batch to "abc123"), null, "", emptyList())
        assertEquals("abc123", message.metadata.batchId)
    }

    @Test
    fun `populates message id if present`() {
        currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(hashMapOf(MessageTag.MessageId to "abc123"), null, "", emptyList())
        assertEquals("abc123", message.metadata.messageId)
    }

    @Test
    fun `populates label if present`() {
        currentTimeProvider = { TestConstants.time }
        val message = IrcMessage(hashMapOf(MessageTag.Label to "abc123"), null, "", emptyList())
        assertEquals("abc123", message.metadata.label)
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
    fun `uses account-name tag when creating a source user`() {
        val message = IrcMessage(hashMapOf(MessageTag.AccountName to "acidBurn"), "acidBurn!libby@root.localhost".toByteArray(), "", emptyList())
        val user = message.sourceUser!!

        assertEquals("acidBurn", user.nickname)
        assertEquals("libby", user.ident)
        assertEquals("root.localhost", user.hostname)
        assertEquals("acidBurn", user.account)
    }

}