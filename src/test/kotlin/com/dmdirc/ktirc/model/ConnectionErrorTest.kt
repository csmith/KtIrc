package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.nio.channels.UnresolvedAddressException
import java.security.cert.CertificateException

internal class ConnectionErrorTest {

    @Test
    fun `maps exceptions to ConnectionError types`() {
        assertEquals(ConnectionError.ConnectionRefused, ConnectException().toConnectionError())
        assertEquals(ConnectionError.BadTlsCertificate, CertificateException().toConnectionError())
        assertEquals(ConnectionError.UnresolvableAddress, UnresolvedAddressException().toConnectionError())
        assertEquals(ConnectionError.Unknown, IllegalArgumentException().toConnectionError())
    }

}
