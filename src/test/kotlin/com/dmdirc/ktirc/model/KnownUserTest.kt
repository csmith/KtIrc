package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class KnownUserTest {

    @Test
    fun `KnownUser can add channels`() {
        val user = KnownUser(User("acidBurn"))
        user += "#thegibson"
        user += "#dumpsterdiving"

        assertEquals(2, user.channels.size)
        assertTrue("#thegibson" in user.channels)
        assertTrue("#dumpsterdiving" in user.channels)
    }

    @Test
    fun `KnownUser can remove channels`() {
        val user = KnownUser(User("acidBurn"))
        user.channels.addAll(listOf("#thegibson", "#dumpsterdiving"))
        user -= "#thegibson"

        assertEquals(1, user.channels.size)
        assertFalse("#thegibson" in user.channels)
        assertTrue("#dumpsterdiving" in user.channels)
    }

    @Test
    fun `KnownUser indicates if a channel is known`() {
        val user = KnownUser(User("acidBurn"))
        user.channels.addAll(listOf("#thegibson"))

        assertTrue("#thegibson" in user)
        assertFalse("#dumpsterdiving" in user)
    }

}