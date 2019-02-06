package com.dmdirc.ktirc.util

import com.dmdirc.ktirc.sasl.fromBase64
import com.dmdirc.ktirc.sasl.toBase64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Base64Test {

    @Test
    fun `encodes byte arrays into base64`() {
        assertEquals("SGFjayB0aGUgUGxhbmV0", "Hack the Planet".toByteArray().toBase64())
    }

    @Test
    fun `decodes byte arrays from base64`() {
        assertEquals("Hack the Planet", String("SGFjayB0aGUgUGxhbmV0".fromBase64()))
    }

}
