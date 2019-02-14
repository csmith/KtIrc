package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.ChannelTopicChanged
import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("#thegibson", event.channel)
        assertEquals("Hack the planet!", event.topic)
    }

    @Test
    fun `raises ChannelTopicDiscovered event when no topic is set`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "331", params("acidBurn", "#thegibson", "No topic set")))
        assertEquals(1, events.size)

        val event = events[0] as ChannelTopicDiscovered
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("#thegibson", event.channel)
        assertNull(event.topic)
    }

    @Test
    fun `raises ChannelTopicMetadataDiscovered event when metadata is supplied`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "333", params("acidBurn", "#thegibson", "zeroCool", unixtime())))
        assertEquals(1, events.size)

        val event = events[0] as ChannelTopicMetadataDiscovered
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals("#thegibson", event.channel)
        assertEquals("zeroCool", event.user.nickname)
        assertEquals(TestConstants.otherTime, event.setTime)
    }

    @Test
    fun `raises ChannelTopicChanged event when topic is changed`() {
        val events = processor.process(IrcMessage(emptyMap(), "acidBurn!acidB@the.gibson".toByteArray(), "TOPIC", params("#thegibson", "Hack the planet!")))
        assertEquals(1, events.size)

        val event = events[0] as ChannelTopicChanged
        assertEquals(TestConstants.time, event.metadata.time)
        assertEquals(User("acidBurn", "acidB", "the.gibson"), event.user)
        assertEquals("#thegibson", event.channel)
        assertEquals("Hack the planet!", event.topic)
    }

    @Test
    fun `does nothing when topic is changed with no source`() {
        val events = processor.process(IrcMessage(emptyMap(), null, "TOPIC", params("#thegibson", "Hack the planet!")))
        assertEquals(0, events.size)
    }

    private fun unixtime() = TestConstants.otherTime.toEpochSecond(currentTimeZoneProvider().rules.getOffset(TestConstants.time)).toString()

}
