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

    @Test
    fun `User updates non-null fields from other instance`() {
        val user1 = User("acidBurn", "libby", awayMessage = "Hacking the planet")
        user1.updateFrom(User("acidBurn", null, "root.localhost", "acidBurn", "Libby"))

        assertEquals("acidBurn", user1.nickname)
        assertEquals("libby", user1.ident)
        assertEquals("root.localhost", user1.hostname)
        assertEquals("acidBurn", user1.account)
        assertEquals("Libby", user1.realName)
        assertEquals("Hacking the planet", user1.awayMessage)

        val user2 = User("acidBurn", null, "root.localhost", "acidBurn", "Libby")
        user2.updateFrom(User("acidBurn", "libby", awayMessage = "Hacking the planet"))

        assertEquals("acidBurn", user2.nickname)
        assertEquals("libby", user2.ident)
        assertEquals("root.localhost", user2.hostname)
        assertEquals("acidBurn", user2.account)
        assertEquals("Libby", user2.realName)
        assertEquals("Hacking the planet", user2.awayMessage)
    }

}