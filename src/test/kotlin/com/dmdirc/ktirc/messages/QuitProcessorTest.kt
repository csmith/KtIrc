package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.UserQuit
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.model.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class QuitProcessorTest {

    @Test
    fun `QuitProcessor raises quit event without message`() {
        val events = QuitProcessor().process(
                IrcMessage(null, "acidburn!libby@root.localhost".toByteArray(), "QUIT", emptyList()))
        Assertions.assertEquals(1, events.size)
        Assertions.assertEquals(UserQuit(User("acidburn", "libby", "root.localhost")), events[0])
    }

    @Test
    fun `QuitProcessor raises quit event with message`() {
        val events = QuitProcessor().process(
                IrcMessage(null, "acidburn!libby@root.localhost".toByteArray(), "QUIT", listOf("Hack the planet!".toByteArray())))
        Assertions.assertEquals(1, events.size)
        Assertions.assertEquals(UserQuit(User("acidburn", "libby", "root.localhost"), "Hack the planet!"), events[0])
    }

    @Test
    fun `QuitProcessor does nothing if prefix missing`() {
        val events = QuitProcessor().process(
                IrcMessage(null, null, "QUIT", listOf("Hack the planet!".toByteArray())))
        Assertions.assertEquals(0, events.size)
    }

}