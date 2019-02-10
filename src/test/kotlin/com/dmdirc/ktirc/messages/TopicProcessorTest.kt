package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId

internal class TopicProcessorTest {

    private val processor = TopicProcessor()

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises ChannelTopicDiscovered event when topic is supplied`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "332", params("acidBurn", "#thegibson", "Hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as ChannelTopicDiscovered
        assertEquals(TestConstants.time, event.time)
        assertEquals("#thegibson", event.channel)
        assertEquals("Hack the planet!", event.topic)
    }

    @Test
    fun `raises ChannelTopicMetadataDiscovered event when topic is supplied`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "333", params("acidBurn", "#thegibson", "zeroCool", unixtime(currentTimeZoneProvider()))))
        assertEquals(1, events.size)

        val event = events[0] as ChannelTopicMetadataDiscovered
        assertEquals(TestConstants.time, event.time)
        assertEquals("#thegibson", event.channel)
        assertEquals("zeroCool", event.user.nickname)
        assertEquals(TestConstants.time, event.setTime)
    }

    private fun unixtime(zoneId: ZoneId) = TestConstants.time.toEpochSecond(zoneId.rules.getOffset(TestConstants.time)).toString()

}
