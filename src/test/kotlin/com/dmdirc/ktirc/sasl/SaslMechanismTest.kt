package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SaslMechanismTest {

    @Test
    fun `creates sasl mechanisms by name`() {
        val mechanisms = SaslConfig().apply { mechanisms("PLAIN", "EXTERNAL") }.createSaslMechanism()
        assertEquals(2, mechanisms.size)
        assertTrue(mechanisms[0] is PlainMechanism)
        assertTrue(mechanisms[1] is ExternalMechanism)
    }

    @Test
    fun `ignores unknown sasl mechanisms`() {
        val mechanisms = SaslConfig().apply { mechanisms("PLAIN", "SPICY") }.createSaslMechanism()
        assertEquals(1, mechanisms.size)
        assertTrue(mechanisms[0] is PlainMechanism)
    }

    @Test
    fun `base64 encodes authentication data`() {
        val client = mock<IrcClient>()
        client.sendAuthenticationData("abcdef")
        verify(client).send("AUTHENTICATE", "YWJjZGVm")
    }

    @Test
    fun `chunks authentication data into 400 byte lines`() {
        val client = mock<IrcClient>()
        client.sendAuthenticationData("abcdef".repeat(120))
        with (inOrder(client)) {
            verify(client, times(2)).send("AUTHENTICATE", "YWJjZGVm".repeat(50))
            verify(client).send("AUTHENTICATE", "YWJjZGVm".repeat(20))
        }
    }

    @Test
    fun `sends blank line if data is exactly 400 bytes`() {
        val client = mock<IrcClient>()
        client.sendAuthenticationData("abcdef".repeat(50))
        with (inOrder(client)) {
            verify(client).send("AUTHENTICATE", "YWJjZGVm".repeat(50))
            verify(client).send("AUTHENTICATE", "+")
        }
    }

}
