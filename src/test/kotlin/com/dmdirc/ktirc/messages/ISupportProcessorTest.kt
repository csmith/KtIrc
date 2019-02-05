package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.ModePrefixMapping
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.ServerFeatureMap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ISupportProcessorTest {

    private val processor = ISupportProcessor()

    @Test
    fun `ISupportProcessor can handle 005s`() {
        assertTrue(processor.commands.contains("005")) { "ISupportProcessor should handle 005 messages" }
    }

    @Test
    fun `ISupportProcessor handles multiple numeric arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "MAXCHANNELS=123", "CHANNELLEN=456", "are supported blah blah").map { it.toByteArray() }))

        assertEquals(123, events[0].serverFeatures[ServerFeature.MaximumChannels])
        assertEquals(456, events[0].serverFeatures[ServerFeature.MaximumChannelNameLength])
    }

    @Test
    fun `ISupportProcessor handles string arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "NETWORK=abcd", "are supported blah blah").map { it.toByteArray() }))

        assertEquals("abcd", events[0].serverFeatures[ServerFeature.Network])
    }

    @Test
    fun `ISupportProcessor handles string array arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "CHANMODES=abcd,efg,,hij", "are supported blah blah").map { it.toByteArray() }))

        val modes = events[0].serverFeatures[ServerFeature.ChannelModes]!!
        assertEquals("abcd", modes[0])
        assertEquals("efg", modes[1])
        assertEquals("", modes[2])
        assertEquals("hij", modes[3])
    }

    @Test
    fun `ISupportProcessor handles resetting arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "-CHANMODES", "are supported blah blah").map { it.toByteArray() }))

        val oldFeatures = ServerFeatureMap()
        oldFeatures[ServerFeature.ChannelModes] = arrayOf("abc")
        oldFeatures.setAll(events[0].serverFeatures)
        assertNull(oldFeatures[ServerFeature.ChannelModes])
    }

    @Test
    fun `ISupportProcessor handles case mapping arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "CASEMAPPING=rfc1459-strict", "are supported blah blah").map { it.toByteArray() }))

        assertEquals(CaseMapping.RfcStrict, events[0].serverFeatures[ServerFeature.ServerCaseMapping])
    }

    @Test
    fun `ISupportProcessor handles mode prefix arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "PREFIX=(ovd)@+%", "are supported blah blah").map { it.toByteArray() }))

        assertEquals(ModePrefixMapping("ovd", "@+%"), events[0].serverFeatures[ServerFeature.ModePrefixes])
    }

    @Test
    fun `ISupportProcessor handles network arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "NETWORK=gibson", "are supported blah blah").map { it.toByteArray() }))

        assertEquals("gibson", events[0].serverFeatures[ServerFeature.Network])
    }

    @Test
    fun `ISupportProcessor handles boolean features with no arguments`() {
        val events = processor.process(IrcMessage(emptyMap(), "server.com".toByteArray(), "005",
                listOf("nickname", "WHOX", "are supported blah blah").map { it.toByteArray() }))

        assertEquals(true, events[0].serverFeatures[ServerFeature.WhoxSupport])
    }

}