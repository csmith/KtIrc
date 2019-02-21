package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class ExternalMechanismTest {

    @Test
    fun `sends + when receiving server message`() {
        val mechanism = ExternalMechanism()
        val client = mockk<IrcClient>()

        mechanism.handleAuthenticationEvent(client, null)

        verify { client.send("AUTHENTICATE", "+") }
    }

}
