package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.model.MessageTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MessageBuilderTest {

    private val builder = MessageBuilder()

    @Test
    fun `builds a command on its own`() =
            assertEquals("TEST", String(builder.build(emptyMap(), "TEST", emptyArray())))

    @Test
    fun `handles a single argument`() =
            assertEquals("TEST foo", String(builder.build(emptyMap(), "TEST", arrayOf("foo"))))

    @Test
    fun `handles a single argument starting with a colon`() =
            assertEquals("TEST ::foo", String(builder.build(emptyMap(), "TEST", arrayOf(":foo"))))

    @Test
    fun `handles a single argument with spaces`() =
            assertEquals("TEST :foo bar", String(builder.build(emptyMap(), "TEST", arrayOf("foo bar"))))

    @Test
    fun `handles many arguments`() =
            assertEquals("TEST foo bar baz", String(builder.build(emptyMap(), "TEST", arrayOf("foo", "bar", "baz"))))

    @Test
    fun `handles many arguments with spaces in the last`() =
            assertEquals("TEST foo bar :baz quux", String(builder.build(emptyMap(), "TEST", arrayOf("foo", "bar", "baz quux"))))

    @Test
    fun `handles single tag`() =
            assertEquals(
                    "@draft/label=abc TEST foo bar",
                    String(builder.build(
                            mapOf(MessageTag.Label to "abc"),
                            "TEST",
                            arrayOf("foo", "bar"))))

    @Test
    fun `handles multiple tags`() =
            assertEquals(
                    "@draft/label=abc;account=acidB TEST foo bar",
                    String(builder.build(
                            mapOf(MessageTag.Label to "abc", MessageTag.AccountName to "acidB"),
                            "TEST",
                            arrayOf("foo", "bar"))))

    @Test
    fun `escapes tag values`() =
            assertEquals(
                    "@draft/label=\\\\hack\\sthe\\r\\nplanet\\: TEST foo bar",
                    String(builder.build(
                            mapOf(MessageTag.Label to "\\hack the\r\nplanet;"),
                            "TEST",
                            arrayOf("foo", "bar"))))

}
