package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelParted
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PartProcessorTest {

    @Test
    fun `PartProcessor raises part event without message`() {
        val events = PartProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PART", listOf("#crashandburn".toByteArray())))
        Assertions.assertEquals(1, events.size)
        Assertions.assertEquals(ChannelParted(User("acidburn", "libby", "root.localhost"), "#crashandburn"), events[0])
    }

    @Test
    fun `PartProcessor raises part event with message`() {
        val events = PartProcessor().process(
                IrcMessage(emptyMap(), "acidburn!libby@root.localhost".toByteArray(), "PART", listOf("#crashandburn".toByteArray(), "Hack the planet!".toByteArray())))
        Assertions.assertEquals(1, events.size)
        Assertions.assertEquals(ChannelParted(User("acidburn", "libby", "root.localhost"), "#crashandburn", "Hack the planet!"), events[0])
    }

    @Test
    fun `PartProcessor does nothing if prefix missing`() {
        val events = JoinProcessor().process(
                IrcMessage(emptyMap(), null, "PART", listOf("#crashandburn".toByteArray())))
        Assertions.assertEquals(0, events.size)
    }

}