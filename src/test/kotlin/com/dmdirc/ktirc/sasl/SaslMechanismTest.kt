package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SaslMechanismTest {

    private val client = mockk<IrcClient>()

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
        client.sendAuthenticationData("abcdef")
        verify { client.send("AUTHENTICATE", "YWJjZGVm") }
    }

    @Test
    fun `chunks authentication data into 400 byte lines`() {
        client.sendAuthenticationData("abcdef".repeat(120))
        verifyOrder {
            client.send("AUTHENTICATE", "YWJjZGVm".repeat(50))
            client.send("AUTHENTICATE", "YWJjZGVm".repeat(50))
            client.send("AUTHENTICATE", "YWJjZGVm".repeat(20))
        }
    }

    @Test
    fun `sends blank line if data is exactly 400 bytes`() {
        client.sendAuthenticationData("abcdef".repeat(50))
        verifyOrder {
            client.send("AUTHENTICATE", "YWJjZGVm".repeat(50))
            client.send("AUTHENTICATE", "+")
        }
    }

}
