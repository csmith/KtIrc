package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class KnownUserTest {

    @Test
    fun `KnownUser can add channels`() {
        val user = KnownUser({ CaseMapping.Rfc }, User("acidBurn"))
        user += "#thegibson"
        user += "#dumpsterdiving"

        assertEquals(2, user.channels.count())
        assertTrue("#thegibson" in user.channels)
        assertTrue("#dumpsterdiving" in user.channels)
    }

    @Test
    fun `KnownUser can remove channels`() {
        val user = KnownUser({ CaseMapping.Rfc }, User("acidBurn"))
        user.channels += "#thegibson"
        user.channels += "#dumpsterdiving"
        user -= "#thegibson"

        assertEquals(1, user.channels.count())
        assertFalse("#thegibson" in user.channels)
        assertTrue("#dumpsterdiving" in user.channels)
    }

    @Test
    fun `KnownUser indicates if a channel is known`() {
        val user = KnownUser({ CaseMapping.Rfc }, User("acidBurn"))
        user.channels += "#thegibson"

        assertTrue("#thegibson" in user)
        assertFalse("#dumpsterdiving" in user)
    }

}