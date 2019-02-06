package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.IrcClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test

internal class MessageBuildersTest {

    private val mockClient = mock<IrcClient>()

    @Test
    fun `sendCapabilityRequest sends CAP REQ message with single argument`() {
        mockClient.sendCapabilityRequest(listOf("a"))
        verify(mockClient).send("CAP REQ :a")
    }

    @Test
    fun `sendCapabilityRequest sends CAP REQ message with multiple args`() {
        mockClient.sendCapabilityRequest(listOf("a b c"))
        verify(mockClient).send("CAP REQ :a b c")
    }

    @Test
    fun `sendJoin sends correct JOIN message`() {
        mockClient.sendJoin("#TheGibson")
        verify(mockClient).send("JOIN :#TheGibson")
    }

    @Test
    fun `sendNickChange sends correct NICK message`() {
        mockClient.sendNickChange("AcidBurn")
        verify(mockClient).send("NICK :AcidBurn")
    }

    @Test
    fun `sendPassword sends correct PASS message`() {
        mockClient.sendPassword("hacktheplanet")
        verify(mockClient).send("PASS :hacktheplanet")
    }

    @Test
    fun `sendPong sends correct PONG message`() {
        mockClient.sendPong("abcdef".toByteArray())
        verify(mockClient).send("PONG :abcdef")
    }

    @Test
    fun `sendMessage sends correct PRIVMSG message`() {
        mockClient.sendMessage("acidBurn", "Hack the planet!")
        verify(mockClient).send("PRIVMSG acidBurn :Hack the planet!")
    }

    @Test
    fun `sendMessage sends correct PRIVMSG message with reply to tag`() {
        mockClient.sendMessage("acidBurn", "Hack the planet!", "abc123")
        verify(mockClient).send("@+draft/reply=abc123 PRIVMSG acidBurn :Hack the planet!")
    }

    @Test
    fun `sendCtcp sends correct CTCP message with no arguments`() {
        mockClient.sendCtcp("acidBurn", "ping")
        verify(mockClient).send("PRIVMSG acidBurn :\u0001PING\u0001")
    }

    @Test
    fun `sendCtcp sends correct CTCP message with arguments`() {
        mockClient.sendCtcp("acidBurn", "ping", "12345")
        verify(mockClient).send("PRIVMSG acidBurn :\u0001PING 12345\u0001")
    }

    @Test
    fun `sendAction sends correct action`() {
        mockClient.sendAction("acidBurn", "hacks the planet")
        verify(mockClient).send("PRIVMSG acidBurn :\u0001ACTION hacks the planet\u0001")
    }

    @Test
    fun `sendUser sends correct USER message`() {
        mockClient.sendUser("AcidBurn","Kate")
        verify(mockClient).send("USER AcidBurn 0 * :Kate")
    }

    @Test
    fun `sendUser sends correct AUTHENTICATE message`() {
        mockClient.sendAuthenticationMessage("SCRAM-MD5")
        verify(mockClient).send("AUTHENTICATE SCRAM-MD5")
    }

    @Test
    fun `sendUser sends correct blank AUTHENTICATE message`() {
        mockClient.sendAuthenticationMessage()
        verify(mockClient).send("AUTHENTICATE +")
    }

}
