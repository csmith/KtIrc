package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.IrcClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test

internal class MessageBuildersTest {

    private val mockClient = mock<IrcClient>()

    @Test
    fun `CapabilityRequestMessage creates CAP REQ message with single argument`() {
        mockClient.sendCapabilityRequest(listOf("a"))
        verify(mockClient).send("CAP REQ :a")
    }

    @Test
    fun `CapabilityRequestMessage creates CAP REQ message with multiple args`() {
        mockClient.sendCapabilityRequest(listOf("a b c"))
        verify(mockClient).send("CAP REQ :a b c")
    }

    @Test
    fun `JoinMessage creates correct JOIN message`() {
        mockClient.sendJoin("#TheGibson")
        verify(mockClient).send("JOIN :#TheGibson")
    }

    @Test
    fun `NickMessage creates correct NICK message`() {
        mockClient.sendNickChange("AcidBurn")
        verify(mockClient).send("NICK :AcidBurn")
    }

    @Test
    fun `PasswordMessage creates correct PASS message`() {
        mockClient.sendPassword("hacktheplanet")
        verify(mockClient).send("PASS :hacktheplanet")
    }

    @Test
    fun `PongMessage creates correct PONG message`() {
        mockClient.sendPong("abcdef".toByteArray())
        verify(mockClient).send("PONG :abcdef")
    }

    @Test
    fun `PrivmsgMessage creates correct PRIVMSG message`() {
        mockClient.sendMessage("acidBurn", "Hack the planet!")
        verify(mockClient).send("PRIVMSG acidBurn :Hack the planet!")
    }

    @Test
    fun `UserMessage creates correct USER message`() {
        mockClient.sendUser("AcidBurn", "localhost", "gibson", "Kate")
        verify(mockClient).send("USER AcidBurn localhost gibson :Kate")
    }

}