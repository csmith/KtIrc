package com.dmdirc.ktirc.messages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MessageBuildersTest {

    @Test
    fun `CapabilityRequestMessage creates CAP REQ message with single argument`() = assertEquals("CAP REQ :a", capabilityRequestMessage(listOf("a")))

    @Test
    fun `CapabilityRequestMessage creates CAP REQ message with multiple args`() = assertEquals("CAP REQ :a b c", capabilityRequestMessage(listOf("a b c")))

    @Test
    fun `JoinMessage creates correct JOIN message`() = assertEquals("JOIN :#Test123", joinMessage("#Test123"))

    @Test
    fun `NickMessage creates correct NICK message`() = assertEquals("NICK :AcidBurn", nickMessage("AcidBurn"))

    @Test
    fun `PasswordMessage creates correct PASS message`() = assertEquals("PASS :abcdef", passwordMessage("abcdef"))

    @Test
    fun `PongMessage creates correct PONG message`() = assertEquals("PONG :abcdef", pongMessage("abcdef".toByteArray()))

    @Test
    fun `PrivmsgMessage creates correct PRIVMSG message`() = assertEquals("PRIVMSG acidBurn :Hack the planet!", privmsgMessage("acidBurn", "Hack the planet!"))

    @Test
    fun `UserMessage creates correct USER message`() = assertEquals("USER AcidBurn localhost gibson :Kate", userMessage("AcidBurn", "localhost", "gibson", "Kate"))

}