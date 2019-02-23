package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ChannelNamesFinished
import com.dmdirc.ktirc.events.ChannelNamesReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NamesProcessorTest {

    private val processor = NamesProcessor()

    @Test
    fun `NamesProcessor handles end of names reply`() {
        val events = processor.process(IrcMessage(emptyMap(), ":the.gibson".toByteArray(), "366", params("AcidBurn", "#root", "End of /NAMES list")))

        assertEquals(1, events.size)
        assertEquals("#root", (events[0] as ChannelNamesFinished).target)
    }

    @Test
    fun `NamesProcessor handles names reply`() {
        val events = processor.process(IrcMessage(emptyMap(), ":the.gibson".toByteArray(), "353", params("AcidBurn", "@", "#root", "AcidBurn @ZeroCool +ThePlague")))

        assertEquals(1, events.size)
        val event = events[0] as ChannelNamesReceived
        assertEquals("#root", event.target)
        assertEquals(listOf("AcidBurn", "@ZeroCool", "+ThePlague"), event.names)
    }

}