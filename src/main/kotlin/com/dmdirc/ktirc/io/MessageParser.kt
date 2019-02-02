package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.messageTags
import com.dmdirc.ktirc.util.logger

/**
 * Parses a message received from an IRC server.
 *
 * IRC messages consist of:
 *
 * - Optionally, IRCv3 message tags. Identified with an '@' character
 * - Optionally, a prefix, identified with an ':' character
 * - A command in the form of a consecutive sequence of letters or exactly three numbers
 * - Some number of space-separated parameters
 * - Optionally, a final 'trailing' parameter prefixed with a ':' character
 *
 * For example:
 *
 * ```
 * @aaa=bbb;ccc;example.com/ddd=eee :nick!ident@host.com PRIVMSG #someChannel :This is a test message
 * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^ ^^^^^^^ ^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^
 * IRCv3 tags                       Prefix               Cmd     Param #1     Trailing parameter
 * ```
 *
 * or:
 *
 * ```
 * PING 12345678
 * ^^^^ ^^^^^^^^
 * Cmd  Param #1
 * ```
 */
internal class MessageParser {

    companion object {
        private const val AT = '@'.toByte()
        private const val COLON = ':'.toByte()
    }

    private val log by logger()

    fun parse(message: ByteArray) = CursorByteArray(message).run {
        IrcMessage(takeTags(), takePrefix(), String(takeWord()), takeParams())
    }

    /**
     * Attempts to read IRCv3 tags from the message.
     */
    private fun CursorByteArray.takeTags() = takeOptionalPrefixedSection(AT).toTagMap()

    /**
     * Attempts to read a prefix from the message.
     */
    private fun CursorByteArray.takePrefix() = takeOptionalPrefixedSection(COLON)

    /**
     * Read a single parameter from the message. If the parameter is a trailing one, the entire message will be
     * consumed.
     */
    private fun CursorByteArray.takeParam() = when (peek()) {
        COLON -> takeRemaining(skip = 1)
        else -> takeWord()
    }

    /**
     * Reads all remaining parameters from the message.
     */
    private fun CursorByteArray.takeParams() = sequence {
        while (!exhausted()) {
            yield(takeParam())
        }
    }.toList()

    /**
     * If the next word starts with the given prefix, takes and returns it, otherwise returns null.
     */
    private fun CursorByteArray.takeOptionalPrefixedSection(prefix: Byte) = when {
        exhausted() -> null
        peek() == prefix -> takeWord(skip = 1)
        else -> null
    }

    /**
     * Parses the bytes as a list of message tags. Unknown tags are discarded.
     */
    private fun ByteArray?.toTagMap() = sequence {
        forEachPart(';') { tag ->
            val index = tag.indexOf('=')
            val name = if (index == -1) tag else tag.substring(0 until index)
            messageTags[name]?.let {
                yield(it to if (index == -1) "" else tag.substring(index + 1).unescapeTagValue())
            } ?: log.severe { "Unknown message tag: $name"}
        }
    }.toMap()

    /**
     * Resolves any backslash escaped characters in a tag value.
     */
    private fun String.unescapeTagValue() = String(sequence {
        var escaped = false
        forEach { char ->
            when {
                escaped -> {
                    char.unescaped()?.let { yield(it) }
                    escaped = false
                }
                char == '\\' -> escaped = true
                else -> yield(char)
            }
        }
    }.toList().toCharArray())

    /**
     * Maps an escaped character in a tag value back to its real form. Returns null if the sequence is invalid.
     */
    private fun Char.unescaped() = when (this) {
        ':' -> ';'
        'n' -> '\n'
        'r' -> '\r'
        's' -> ' '
        '\\' -> '\\'
        else -> null
    }

    private inline fun ByteArray?.forEachPart(delimiter: Char, action: (String) -> Unit) = this?.let {
        String(it).split(delimiter).forEach(action)
    }

}

/**
 * A ByteArray with a 'cursor' that tracks the current read position.
 */
internal class CursorByteArray(private val data: ByteArray, var cursor: Int = 0) {

    companion object {
        private const val SPACE = ' '.toByte()
    }

    /**
     * Returns whether or not the cursor has reached the end of the array.
     */
    fun exhausted() = cursor >= data.size

    /**
     * Returns the next byte in the array without advancing the cursor.
     *
     * @throws ArrayIndexOutOfBoundsException If the array is [exhausted].
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    fun peek() = data[cursor]

    /**
     * Returns the next "word" in the byte array - that is, all non-space characters up until the next space.
     *
     * After calling this method, the cursor will be advanced to the start of the next word (i.e., it will skip over
     * any number of space characters).
     *
     * @param skip Number of bytes to omit from the start of the word
     */
    fun takeWord(skip: Int = 0) = data.sliceArray(cursor + skip until seekTo { it == SPACE }).apply { seekTo { it != SPACE } }

    /**
     * Takes all remaining bytes from the cursor until the end of the array.
     *
     * @param skip Number of bytes to omit from the start of the remainder
     */
    fun takeRemaining(skip: Int = 0) = data.sliceArray(cursor + skip until data.size).apply { cursor = data.size }

    private fun seekTo(matcher: (Byte) -> Boolean): Int {
        while (!exhausted() && !matcher(peek())) {
            cursor++
        }
        return cursor
    }

}
