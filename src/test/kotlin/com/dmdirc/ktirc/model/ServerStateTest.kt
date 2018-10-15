package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ServerStateTest {

    @Test
    fun `IrcServerState should use the initial nickname as local nickname`() {
        val serverState = ServerState("acidBurn")
        assertEquals("acidBurn", serverState.localNickname)
    }

}