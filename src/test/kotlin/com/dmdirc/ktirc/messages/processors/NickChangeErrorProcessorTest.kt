package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.TestConstants
import com.dmdirc.ktirc.events.NicknameChangeFailed
import com.dmdirc.ktirc.messages.tagMap
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.MessageTag
import com.dmdirc.ktirc.params
import com.dmdirc.ktirc.util.currentTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NickChangeErrorProcessorTest {

    private val processor = NickChangeErrorProcessor()

    @BeforeEach
    fun setup() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `raises error event when nick in use`() {
        val events = processor.process(IrcMessage(tagMap(), null, "433", params("Nickname already in use")))
        assertEquals(1, events.size)
        assertEquals(NicknameChangeFailed.NicknameChangeError.AlreadyInUse, events[0].cause)
    }

    @Test
    fun `raises error event when nick is erroneous`() {
        val events = processor.process(IrcMessage(tagMap(), null, "432", params("Nickname not allowed")))
        assertEquals(1, events.size)
        assertEquals(NicknameChangeFailed.NicknameChangeError.ErroneousNickname, events[0].cause)
    }

    @Test
    fun `raises error event when nick collides`() {
        val events = processor.process(IrcMessage(tagMap(), null, "436", params("Nick collision")))
        assertEquals(1, events.size)
        assertEquals(NicknameChangeFailed.NicknameChangeError.Collision, events[0].cause)
    }

    @Test
    fun `raises error event when nick not provided`() {
        val events = processor.process(IrcMessage(tagMap(), null, "431", params("No nickname given")))
        assertEquals(1, events.size)
        assertEquals(NicknameChangeFailed.NicknameChangeError.NoNicknameGiven, events[0].cause)
    }

    @Test
    fun `throws if invoked with other numeric`() {
        assertThrows<IllegalArgumentException> {
            processor.process(IrcMessage(tagMap(), null, "123", params("Hack the planet!")))
        }
    }

    @Test
    fun `raises error event with correct metadata`() {
        val events = processor.process(IrcMessage(tagMap(MessageTag.Label to "abc"), null, "433", params("Nickname already in use")))
        assertEquals(1, events.size)
        assertEquals(TestConstants.time, events[0].metadata.time)
        assertEquals("abc", events[0].metadata.label)
    }

}
