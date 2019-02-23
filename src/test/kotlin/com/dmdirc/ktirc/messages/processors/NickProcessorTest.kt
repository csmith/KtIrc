package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NickProcessorTest {

    @Test
    fun `raises UserChangedNick event if prefix is valid`() {
        val processor = NickProcessor()
        val events = processor.process(IrcMessage(emptyMap(), "acidBurn!acidb@localhost".toByteArray(), "NICK", params("acidB")))

        assertEquals(1, events.size)
        val event = events[0]
        assertEquals("acidBurn", event.user.nickname)
        assertEquals("acidb", event.user.ident)
        assertEquals("localhost", event.user.hostname)
        assertEquals("acidB", event.newNick)
    }

    @Test
    fun `does nothing if prefix is missing`() {
        val processor = NickProcessor()
        val events = processor.process(IrcMessage(emptyMap(), null, "NICK", params("acidB")))
        assertTrue(events.isEmpty())
    }

}
