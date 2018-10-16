package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class UserTest {

    @Test
    fun `ByteArray asUser returns user with just nickname`() {
        val user = "acidBurn".toByteArray().asUser()
        assertEquals("acidBurn", user.nickname)
        assertNull(user.ident)
        assertNull(user.hostname)
    }

    @Test
    fun `ByteArray asUser returns user and ident`() {
        val user = "acidBurn!libby".toByteArray().asUser()
        assertEquals("acidBurn", user.nickname)
        assertEquals("libby", user.ident)
        assertNull(user.hostname)
    }

    @Test
    fun `ByteArray asUser returns user ident and host`() {
        val user = "acidBurn!libby@root.localhost".toByteArray().asUser()
        assertEquals("acidBurn", user.nickname)
        assertEquals("libby", user.ident)
        assertEquals("root.localhost", user.hostname)
    }

}