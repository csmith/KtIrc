package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test

internal class ExternalMechanismTest {

    @Test
    fun `sends + when receiving server message`() {
        val mechanism = ExternalMechanism()
        val client = mock<IrcClient>()

        mechanism.handleAuthenticationEvent(client, null)

        verify(client).send("AUTHENTICATE +")
    }

}
