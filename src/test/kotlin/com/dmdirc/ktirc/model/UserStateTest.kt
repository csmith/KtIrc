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

    @Test
    fun `addToChannel adds new user if not known`() {
        userState.addToChannel(User("acidBurn", "libby", "root.localhost"), "#thegibson")

        val user = userState["acidburn"]!!
        assertEquals("acidBurn", user.details.nickname)
        assertEquals("libby", user.details.ident)
        assertEquals("root.localhost", user.details.hostname)

        assertEquals(1, user.channels.count())
        assertTrue("#thegibson" in user.channels)
    }

    @Test
    fun `addToChannel appends channel to existing user`() {
        userState += User("acidBurn", "libby", "root.localhost")
        userState.addToChannel(User("acidBurn"), "#thegibson")

        val user = userState["acidburn"]!!
        assertEquals(1, user.channels.count())
        assertTrue("#thegibson" in user.channels)
    }

    @Test
    fun `removeIf deletes all matching users`() {
        userState += User("acidBurn", "libby", "root.localhost")
        userState += User("zeroCool", "dade", "root.localhost")
        userState += User("acidBurn2", "libby", "root.localhost")

        userState.removeIf { it.details.nickname.startsWith("acidBurn") }

        assertEquals(1, userState.count())
        assertNotNull(userState["zeroCool"])
    }

}