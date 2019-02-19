package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.model.ServerState
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ScramMechanismTest {

    private val serverState = ServerState("", "")
    private val ircClient = mock<IrcClient> {
        on { serverState } doReturn serverState
    }

    @Test
    fun `sends first message when no state is present`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        mechanism.handleAuthenticationEvent(ircClient, "+".toByteArray())

        val nonce = (serverState.sasl.mechanismState as ScramState).clientNonce
        verify(ircClient).send("AUTHENTICATE", "n,,n=user,r=$nonce".toByteArray().toBase64())
    }

    @Test
    fun `aborts if the server's first message contains extensions`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage)

        mechanism.handleAuthenticationEvent(ircClient, "m=future".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's first message contains an error`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage)

        mechanism.handleAuthenticationEvent(ircClient, "e=whoops".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's first message lacks an iteration count`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage)

        mechanism.handleAuthenticationEvent(ircClient, "r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's first message has an invalid iteration count`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage)

        mechanism.handleAuthenticationEvent(ircClient, "r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=leet".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's first message lacks a nonce`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage)

        mechanism.handleAuthenticationEvent(ircClient, "rs=QSXCR+Q6sek8bf92,i=4096".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's first message lacks a salt`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage)

        mechanism.handleAuthenticationEvent(ircClient, "r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,i=4096".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `updates state after receiving server's first message`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage, clientNonce = "fyko+d2lbbFgONRv9qkxdawL")

        mechanism.handleAuthenticationEvent(ircClient, "r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096".toByteArray())

        (serverState.sasl.mechanismState as ScramState).let {
            assertEquals(ScramStage.Finishing, it.scramStage)
            assertEquals("fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j", it.serverNonce)
            assertEquals(4096, it.iterCount)
            assertEquals("QSXCR+Q6sek8bf92", it.salt.toBase64())
            assertEquals("HZbuOlKbWl+eR8AfIposuKbhX30=", it.saltedPassword.toBase64())
            assertEquals("n=user,r=fyko+d2lbbFgONRv9qkxdawL,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096,c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j", String(it.authMessage))
        }
    }

    @Test
    fun `responds to server's first message`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.SendingSecondMessage, clientNonce = "fyko+d2lbbFgONRv9qkxdawL")

        mechanism.handleAuthenticationEvent(ircClient, "r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,p=v0X8v3Bz2T0CJGbJQyF0X+HI4Ts=".toByteArray().toBase64())
    }

    @Test
    fun `aborts if the server's final message contains extensions`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.Finishing)

        mechanism.handleAuthenticationEvent(ircClient, "m=future".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's final message contains an error`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(scramStage = ScramStage.Finishing)

        mechanism.handleAuthenticationEvent(ircClient, "e=whoops".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's final message doesn't contain a verifier`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(
                scramStage = ScramStage.Finishing,
                saltedPassword = "HZbuOlKbWl+eR8AfIposuKbhX30=".fromBase64(),
                authMessage = "n=user,r=fyko+d2lbbFgONRv9qkxdawL,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096,c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j".toByteArray())

        mechanism.handleAuthenticationEvent(ircClient, "".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `aborts if the server's verifier doesn't match`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(
                scramStage = ScramStage.Finishing,
                saltedPassword = "HZbuOlKbWl+eR8AfIposuKbhX30=".fromBase64(),
                authMessage = "n=user,r=fyko+d2lbbFgONRv9qkxdawL,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096,c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j".toByteArray())

        mechanism.handleAuthenticationEvent(ircClient, "v=rmF9pqV8S7suAoZWja4dJRkF=".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "*")
    }

    @Test
    fun `sends final message if server's verifier matches`() {
        val mechanism = ScramMechanism("SHA-1", 1, SaslConfig().apply {
            username = "user"
            password = "pencil"
        })

        serverState.sasl.mechanismState = ScramState(
                scramStage = ScramStage.Finishing,
                saltedPassword = "HZbuOlKbWl+eR8AfIposuKbhX30=".fromBase64(),
                authMessage = "n=user,r=fyko+d2lbbFgONRv9qkxdawL,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096,c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j".toByteArray())

        mechanism.handleAuthenticationEvent(ircClient, "v=rmF9pqV8S7suAoZWja4dJRkFsKQ=".toByteArray())

        verify(ircClient).send("AUTHENTICATE", "+")
    }

}
