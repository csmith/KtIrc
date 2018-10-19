package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UserStateTest {

    private val userState = UserState { CaseMapping.Rfc }

    @Test
    fun `UserState adds and gets new users`() {
        userState += User("acidBurn", "libby", "root.localhost")
        val user = userState["acidburn"]
        assertNotNull(user)
        assertEquals("acidBurn", user?.details?.nickname)
        assertEquals("libby", user?.details?.ident)
        assertEquals("root.localhost", user?.details?.hostname)
    }

    @Test
    fun `UserState removes users`() {
        userState += User("acidBurn", "libby", "root.localhost")
        userState -= User("ACIDBURN")
        assertNull(userState["acidburn"])
    }

    @Test
    fun `UserState updates existing user with same nickname`() {
        userState += User("acidBurn", "libby", "root.localhost")
        userState.update(User("acidBurn", realName = "Libby", awayMessage = "Hacking"))

        val user = userState["acidburn"]!!
        assertEquals("acidBurn", user.details.nickname)
        assertEquals("libby", user.details.ident)
        assertEquals("root.localhost", user.details.hostname)
        assertEquals("Libby", user.details.realName)
        assertEquals("Hacking", user.details.awayMessage)
    }

    @Test
    fun `UserState updates existing user with new nickname`() {
        userState += User("acidBurn", "libby", "root.localhost")
        userState.update(User("acidBurn2", realName = "Libby", awayMessage = "Hacking"), "acidBurn")

        val user = userState["acidburn2"]!!
        assertEquals("acidBurn2", user.details.nickname)
        assertEquals("libby", user.details.ident)
        assertEquals("root.localhost", user.details.hostname)
        assertEquals("Libby", user.details.realName)
        assertEquals("Hacking", user.details.awayMessage)

        assertNull(userState["acidBurn"])
    }

}