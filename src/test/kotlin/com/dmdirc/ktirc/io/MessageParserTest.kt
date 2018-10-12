package com.dmdirc.ktirc.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


internal class MessageParserTest {

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun ircMessageArgumentsProvider(): Stream<Arguments> = Stream.of(
                arguments("test", null, null, "test", emptyList<String>()),
                arguments("test 1 2", null, null, "test", listOf("1", "2")),
                arguments("test    1     2     ", null, null, "test", listOf("1", "2")),
                arguments("test :1 2", null, null, "test", listOf("1 2")),
                arguments("test :1 2    ", null, null, "test", listOf("1 2    ")),
                arguments("123 :1 2    ", null, null, "123", listOf("1 2    ")),
                arguments(":test abc 1 2    ", null, "test", "abc", listOf("1", "2")),
                arguments("@tags :test abc 1 2 :three four", "tags", "test", "abc", listOf("1", "2", "three four")),
                arguments("@tags abc 1 2 : three four ", "tags", null, "abc", listOf("1", "2", " three four "))
        )
    }

    @ParameterizedTest
    @MethodSource("ircMessageArgumentsProvider")
    fun `Parses IRC messages`(input: String, tags: String?, prefix: String?, command: String, params: List<String>) {
        val parsed = MessageParser().parse(input.toByteArray())

        assertEquals(tags, parsed.tags?.let { String(it) }) { "Expected '$input' to have tags '$tags'" }
        assertEquals(prefix, parsed.prefix?.let { String(it) }) { "Expected '$input' to have prefix '$prefix'" }
        assertEquals(command, parsed.command) { "Expected '$input' to have command '$command'" }
        assertEquals(params, parsed.params.map { String(it) }) { "Expected '$input' to have params '$params'" }
    }

}

internal class CursorByteArrayTest {

    @Test
    fun `Peek returns next byte without advancing cursor`() {
        val cursorByteArray = CursorByteArray(byteArrayOf(0x08, 0x09, 0x10))
        assertEquals(0x08, cursorByteArray.peek()) { "Peek should return the byte at the start" }
        assertEquals(0x08, cursorByteArray.peek()) { "Peek shouldn't advance the cursor" }

        cursorByteArray.cursor = 2
        assertEquals(0x10, cursorByteArray.peek()) { "Peek should return the byte at the current cursor" }

        cursorByteArray.cursor = 3
        assertThrows(ArrayIndexOutOfBoundsException::class.java, { cursorByteArray.peek() }) { "Peek should throw if cursor is out of bounds" }
    }

    @Test
    fun `Exhausted returns true when no more bytes available`() {
        val cursorByteArray = CursorByteArray(byteArrayOf(0x08, 0x09, 0x10))
        assertFalse(cursorByteArray.exhausted()) { "Exhausted should be false with a new array" }

        cursorByteArray.cursor = 1
        assertFalse(cursorByteArray.exhausted()) { "Exhausted should be false with an in-bound cursor" }

        cursorByteArray.cursor = 2
        assertFalse(cursorByteArray.exhausted()) { "Exhausted should be false at the last element" }

        cursorByteArray.cursor = 3
        assertTrue(cursorByteArray.exhausted()) { "Exhausted should be true when past the last element" }

        assertTrue(CursorByteArray(byteArrayOf()).exhausted()) { "Exhausted should be true on an empty array" }
    }

    @Test
    fun `TakeWord reads next word and advances cursor beyond trailing whitespace`() {
        val cursorByteArray = CursorByteArray("Hello this    is a    test".toByteArray())

        assertEquals("Hello", String(cursorByteArray.takeWord())) { "TakeWord should read first word" }
        assertEquals(6, cursorByteArray.cursor) { "TakeWord should advance cursor to next word" }

        assertEquals("this", String(cursorByteArray.takeWord())) { "TakeWord should read word at cursor" }
        assertEquals(14, cursorByteArray.cursor) { "TakeWord should advance cursor past trailing whitespace" }

        assertEquals("s", String(cursorByteArray.takeWord(1))) { "TakeWord should skip given number of bytes" }

        cursorByteArray.cursor = 22
        assertEquals("test", String(cursorByteArray.takeWord())) { "TakeWord should read word at end" }
        assertEquals(26, cursorByteArray.cursor) { "TakeWord should advance cursor past last word" }
    }

    @Test
    fun `TakeRemaining takes all remaining bytes and advances the cursor to exhaustion`() {
        var cursorByteArray = CursorByteArray("Test1234".toByteArray(), 4)
        assertEquals("1234", String(cursorByteArray.takeRemaining())) { "TakeRemaining should return remaining bytes" }
        assertEquals(8, cursorByteArray.cursor) { "TakeRemaining should advance cursor to end of array" }

        cursorByteArray = CursorByteArray("Test1234".toByteArray(), 0)
        assertEquals("est1234", String(cursorByteArray.takeRemaining(1))) { "TakeRemaining should skip specified number of bytes" }
        assertEquals(8, cursorByteArray.cursor) { "TakeRemaining should advance cursor to end of array when skipping" }
    }

    private fun byteArrayOf(vararg bytes: Byte) = ByteArray(bytes.size) { i -> bytes[i] }

}