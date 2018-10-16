package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelNamesFinished
import com.dmdirc.ktirc.events.ChannelNamesReceived
import com.dmdirc.ktirc.io.IrcMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NamesProcessorTest {

    private val processor = NamesProcessor()

    @Test
    fun `NamesProcessor handles end of names reply`() {
        val events = processor.process(IrcMessage(null, ":the.gibson".toByteArray(), "366", listOf("AcidBurn", "#root", "End of /NAMES list").map { it.toByteArray() }))

        assertEquals(1, events.size)
        assertEquals("#root", (events[0] as ChannelNamesFinished).channel)
    }

    @Test
    fun `NamesProcessor handles names reply`() {
        val events = processor.process(IrcMessage(null, ":the.gibson".toByteArray(), "353", listOf("AcidBurn", "@", "#root", "AcidBurn @ZeroCool +ThePlague").map { it.toByteArray() }))

        assertEquals(1, events.size)
        val event = events[0] as ChannelNamesReceived
        assertEquals("#root", event.channel)
        assertEquals(listOf("AcidBurn", "@ZeroCool", "+ThePlague"), event.names)
    }

}