package com.dmdirc.ktirc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class IrcClientConfigBuilderTest {

    @Test
    fun `throws if server is defined twice`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                server { host = "1" }
                server { host = "2" }
            }
        }
    }

    @Test
    fun `throws if no host is provided`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                server {}
            }
        }
    }

    @Test
    fun `throws if profile is defined twice`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                profile { nickname = "acidBurn" }
                profile { nickname = "zeroCool" }
            }
        }
    }

    @Test
    fun `throws if no nickname is provided`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                profile {}
            }
        }
    }

    @Test
    fun `throws if sasl is defined twice`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                sasl {}
                sasl {}
            }
        }
    }

    @Test
    fun `throws if server is not defined`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                profile { nickname = "acidBurn" }
            }.build()
        }
    }

    @Test
    fun `throws if profile is not defined`() {
        assertThrows<IllegalStateException> {
            IrcClientConfigBuilder().apply {
                server { host = "thegibson.com" }
            }.build()
        }
    }

    @Test
    fun `applies server settings`() {
        val config = IrcClientConfigBuilder().apply {
            profile { nickname = "acidBurn" }
            server {
                host = "thegibson.com"
                port = 1337
                password = "h4cktheplan3t"
                useTls = true
            }
        }.build()

        assertEquals("thegibson.com", config.server.host)
        assertEquals(1337, config.server.port)
        assertEquals("h4cktheplan3t", config.server.password)
        assertTrue(config.server.useTls)
    }

    @Test
    fun `applies profile settings`() {
        val config = IrcClientConfigBuilder().apply {
            profile {
                nickname = "acidBurn"
                username = "acidB"
                realName = "Kate"
            }
            server { host = "thegibson.com" }
        }.build()

        assertEquals("acidBurn", config.profile.nickname)
        assertEquals("acidB", config.profile.username)
        assertEquals("Kate", config.profile.realName)
    }

    @Test
    fun `applies sasl settings`() {
        val config = IrcClientConfigBuilder().apply {
            profile { nickname = "acidBurn" }
            server { host = "thegibson.com" }
            sasl {
                username = "acidBurn"
                password = "h4ckthepl@net"
            }
        }.build()

        assertEquals("acidBurn", config.sasl?.username)
        assertEquals("h4ckthepl@net", config.sasl?.password)
    }

}

internal class SaslConfigTest {

    @Test
    fun `mechanisms function clears all existing mechanisms`() {
        val config = SaslConfig().apply {
            mechanisms += "TEST"
            mechanisms("FOO", "BAR")
        }

        assertEquals(setOf("FOO", "BAR"), config.mechanisms)
    }

    @Test
    fun `defaults to plain and scram mechanisms`() {
        val config = SaslConfig()
        assertEquals(setOf("PLAIN", "SCRAM-SHA-1", "SCRAM-SHA-256"), config.mechanisms)
    }

}
