package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.SaslConfig
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

}