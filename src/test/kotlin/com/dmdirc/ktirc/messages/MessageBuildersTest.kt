package com.dmdirc.ktirc.messages

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MessageBuildersTest {

    @Test
    fun `JoinMessage creates correct JOIN message`() = assertEquals("JOIN :#Test123", joinMessage("#Test123"))

    @Test
    fun `NickMessage creates correct NICK message`() = assertEquals("NICK :AcidBurn", nickMessage("AcidBurn"))

    @Test
    fun `PasswordMessage creates correct PASS message`() = assertEquals("PASS :abcdef", passwordMessage("abcdef"))

    @Test
    fun `PongMessage creates correct PONG message`() = assertEquals("PONG :abcdef", pongMessage("abcdef".toByteArray()))

    @Test
    fun `UserMessage creates correct USER message`() = assertEquals("USER AcidBurn localhost gibson :Kate", userMessage("AcidBurn", "localhost", "gibson", "Kate"))

}