package com.dmdirc.ktirc.util

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.model.ServerState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class LabelsTest {

    private val fakeServerState = ServerState("", "")
    private val ircClient = mockk<IrcClient> {
        every { serverState } returns fakeServerState
    }

    @Test
    fun `increments the label id when generating labels`() {
        assertEquals(0L, fakeServerState.asyncResponseState.labelCounter.get())
        defaultGenerateLabel(ircClient)
        assertEquals(1L, fakeServerState.asyncResponseState.labelCounter.get())
        defaultGenerateLabel(ircClient)
        assertEquals(2L, fakeServerState.asyncResponseState.labelCounter.get())
    }

    @Test
    fun `generates unique labels in same instant`() {
        currentTimeProvider = { TestConstants.time }
        assertNotEquals(defaultGenerateLabel(ircClient), defaultGenerateLabel(ircClient))
        assertNotEquals(defaultGenerateLabel(ircClient), defaultGenerateLabel(ircClient))
    }

    @Test
    fun `generates unique labels at different times with the same counter value`() {
        currentTimeProvider = { TestConstants.time }
        val label1 = defaultGenerateLabel(ircClient)
        fakeServerState.asyncResponseState.labelCounter.set(0L)
        currentTimeProvider = { TestConstants.otherTime }
        val label2 = defaultGenerateLabel(ircClient)
        assertNotEquals(label1, label2)
    }

}
