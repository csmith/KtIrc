package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.BatchFinished
import com.dmdirc.ktirc.events.BatchStarted
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BatchProcessorTest {

    private var processor = BatchProcessor()

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises batch finished event when reference starts with a -`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "BATCH", params("-mybatch")))

        assertEquals(1, events.size)
        val event = events[0] as BatchFinished
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("mybatch", event.referenceId)
    }

    @Test
    fun `raises batch started event when reference starts with a +`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "BATCH", params("+mybatch", "mytype", "arg1", "arg2")))

        assertEquals(1, events.size)
        val event = events[0] as BatchStarted
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("mybatch", event.referenceId)
        assertEquals("mytype", event.batchType)
        assertArrayEquals(arrayOf("arg1", "arg2"), event.params)
    }

    @Test
    fun `ignores batches with bad reference ids`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "BATCH", params("~mybatch")))

        assertEquals(0, events.size)
    }

}
