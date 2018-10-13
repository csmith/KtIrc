package com.dmdirc.ktirc.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class LoggingTest {

    private val log by logger()

    @Test
    fun `logger gives logger with correct class name`() {
        assertEquals("com.dmdirc.ktirc.util.LoggingTest", log.name)
    }

}