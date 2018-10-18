package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PrivmsgProcessorTest {

    @Test
    fun `PrivsgProcessor raises message received event`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PRIVMSG", listOf("#crashandburn".toByteArray(), "hack the planet!".toByteArray())))
        Assertions.assertEquals(1, events.size)
        Assertions.assertEquals(MessageReceived(User("acidburn", "libby", "root.localhost"), "#crashandburn", "hack the planet!"), events[0])
    }

    @Test
    fun `PrivsgProcessor does nothing if prefix missing`() {
        val events = PrivmsgProcessor().process(
                IrcMessage(emptyMap(), null, "PRIVMSG", listOf("#crashandburn".toByteArray(), "hack the planet!".toByteArray())))
        Assertions.assertEquals(0, events.size)
    }
}