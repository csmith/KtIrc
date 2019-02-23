package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ChangeHostProcessorTest {

    private val processor = ChangeHostProcessor()

    @Test
    fun `does nothing if no prefix is supplied`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "CHGHOST", emptyList()))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `raises event when chghost is received`() {
        val events = processor.process(IrcMessage(emptyMap(), "acidBurn".toByteArray(), "CHGHOST", params("libby", "root.gibson")))
        assertEquals(1, events.size)
        assertEquals(User("acidBurn"), events[0].user)
        assertEquals("libby", events[0].newIdent)
        assertEquals("root.gibson", events[0].newHost)
    }

}
