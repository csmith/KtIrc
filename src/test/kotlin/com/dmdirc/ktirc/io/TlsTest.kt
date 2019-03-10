package com.dmdirc.ktirc.io

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.io.writeFully
import kotlinx.io.core.String
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal class CertificateValidationTest {

    private val cert = mockk<X509Certificate>()

    @Test
    fun `checks common name`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=subdomain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain2.test.ktirc"))
        assertFalse(cert.validFor("testing"))
    }

    @Test
    fun `checks common name with suffixed wildcard`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=subdomain*.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.ktirc"))
        assertFalse(cert.validFor("foo.subdomain.test.ktirc"))
        assertFalse(cert.validFor("1subdomain.test.ktirc"))
    }

    @Test
    fun `checks common name with preixed wildcard`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=*subdomain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertTrue(cert.validFor("1subdomain.test.ktirc"))
        assertFalse(cert.validFor("foo.subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain1.test.ktirc"))
    }

    @Test
    fun `checks common name with infixed wildcard`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=sub*domain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertTrue(cert.validFor("SUB-domain.test.ktirc"))
        assertFalse(cert.validFor("foo.subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain1.test.ktirc"))
    }

    @Test
    fun `ignores wildcards in CN if they're not left-most`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=foo.*domain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertFalse(cert.validFor("foo.domain.test.ktirc"))
        assertFalse(cert.validFor("foo-test.domain.test.ktirc"))
        assertFalse(cert.validFor("foo.test-domain.test.ktirc"))
    }

    @Test
    fun `ignores wildcards in CN if there are too many`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=*domain*.test.ktirc,O=testing,L=London,C=GB"
        }

        assertFalse(cert.validFor("domain.test.ktirc"))
        assertFalse(cert.validFor("subdomain.test.ktirc"))
        assertFalse(cert.validFor("domain1.test.ktirc"))
    }

    @Test
    fun `checks all sans`() {
        every { cert.subjectAlternativeNames } returns listOf(
                listOf(4, "directory.test.ktirc"),
                listOf(2, "subdomain1.test.ktirc"),
                listOf(2, "subdomain2.test.ktirc"),
                listOf(2, "subdomain3.test.ktirc")
        )

        assertTrue(cert.validFor("subdomain1.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.KTIRC"))
        assertTrue(cert.validFor("subdomain3.test.ktirc"))
        assertFalse(cert.validFor("directory.test.ktirc"))
    }

    @Test
    fun `checks wildcard sans`() {
        every { cert.subjectAlternativeNames } returns listOf(
                listOf(4, "directory.test.ktirc"),
                listOf(2, "*domain1.test.ktirc"),
                listOf(2, "subdomain*.test.ktirc"),
                listOf(2, "*foo*.test.ktirc"),
                listOf(2, "foo.*.ktirc")
        )

        assertTrue(cert.validFor("subdomain1.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.ktirc"))
        assertTrue(cert.validFor("gooddomain1.TEST.ktirc"))
        assertFalse(cert.validFor("foo.test.ktirc"))
    }

    @Test
    fun `still uses CN if sans throws`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=subdomain.test.ktirc,O=testing,L=London,C=GB"
        }
        every { cert.subjectAlternativeNames } throws CertificateException("Oops")

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain2.test.ktirc"))
        assertFalse(cert.validFor("testing"))
    }

    @Test
    fun `still uses sans if CN throws`() {
        every { cert.subjectX500Principal } throws CertificateException("Oops")
        every { cert.subjectAlternativeNames } returns listOf(
                listOf(4, "directory.test.ktirc"),
                listOf(2, "subdomain1.test.ktirc"),
                listOf(2, "subdomain2.test.ktirc"),
                listOf(2, "subdomain3.test.ktirc")
        )

        assertTrue(cert.validFor("subdomain1.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.KTIRC"))
        assertTrue(cert.validFor("subdomain3.test.ktirc"))
        assertFalse(cert.validFor("directory.test.ktirc"))
    }


    @Test
    fun `fails if CN and sans missing`() {
        assertFalse(cert.validFor("subdomain1.test.ktirc"))
        assertFalse(cert.validFor("subdomain2.test.KTIRC"))
        assertFalse(cert.validFor("subdomain3.test.ktirc"))
        assertFalse(cert.validFor("directory.test.ktirc"))
    }

}

@Suppress("BlockingMethodInNonBlockingContext")
@Execution(ExecutionMode.SAME_THREAD)
internal class TlsSocketTest {

    @Test
    fun `can send a string to a server over TLS`() = runBlocking {
        withTimeout(5000) {
            tlsServerSocket(12321).use { serverSocket ->
                val plainSocket = PlainTextSocket(GlobalScope)
                val tlsSocket = TlsSocket(GlobalScope, plainSocket, getTrustingContext(), "localhost")
                val clientBytesAsync = GlobalScope.async {
                    ByteArray(13).apply {
                        serverSocket.accept().getInputStream().read(this)
                    }
                }

                tlsSocket.connect(InetSocketAddress("localhost", 12321))
                tlsSocket.write.writeFully("Hello World\r\n".toByteArray())

                val bytes = clientBytesAsync.await()
                assertNotNull(bytes)
                assertEquals("Hello World\r\n", String(bytes))
            }
        }
    }

    @Test
    fun `can read a string from a server over TLS`() = runBlocking<Unit> {
        withTimeout(5000) {
            tlsServerSocket(12321).use { serverSocket ->
                val plainSocket = PlainTextSocket(GlobalScope)
                val tlsSocket = TlsSocket(GlobalScope, plainSocket, getTrustingContext(), "localhost")
                val socket = GlobalScope.async {
                    serverSocket.accept().apply {
                        GlobalScope.launch {
                            getInputStream().read()
                        }
                    }
                }

                tlsSocket.connect(InetSocketAddress("localhost", 12321))

                GlobalScope.launch {
                    with(socket.await().getOutputStream()) {
                        write("Hack the planet!".toByteArray())
                        flush()
                    }
                }

                val buffer = tlsSocket.read()

                assertNotNull(buffer)
                buffer?.let {
                    assertEquals("Hack the planet!", String(it.array(), 0, it.limit()))
                }
            }
        }
    }

    @Test
    fun `read returns null after close`() = runBlocking {
        withTimeout(5000) {
            tlsServerSocket(12321).use { serverSocket ->
                val plainSocket = PlainTextSocket(GlobalScope)
                val tlsSocket = TlsSocket(GlobalScope, plainSocket, getTrustingContext(), "localhost")
                GlobalScope.launch {
                    serverSocket.accept().getInputStream().read()
                }

                tlsSocket.connect(InetSocketAddress("localhost", 12321))

                tlsSocket.close()

                val buffer = tlsSocket.read()

                assertNull(buffer)
            }
        }
    }

    @Test
    fun `throws if the hostname mismatches`() {
        tlsServerSocket(12321).use { serverSocket ->
            val plainSocket = PlainTextSocket(GlobalScope)
            val tlsSocket = TlsSocket(GlobalScope, plainSocket, getTrustingContext(), "127.0.0.1")
            GlobalScope.launch {
                serverSocket.accept().getInputStream().read()
            }

            runBlocking {
                withTimeout(5000) {
                    try {
                        tlsSocket.connect(InetSocketAddress("localhost", 12321))
                        fail<Unit>("Expected an exception")
                    } catch (ex: Exception) {
                        assertTrue(ex is CertificateException)
                    }
                }
            }
        }
    }


}

internal fun tlsServerSocket(port: Int): ServerSocket {
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(CertificateValidationTest::class.java.getResourceAsStream("localhost.p12"), CharArray(0))

    val keyManagerFactory = KeyManagerFactory.getInstance("PKIX")
    keyManagerFactory.init(keyStore, CharArray(0))

    val sslContext = SSLContext.getInstance("TLSv1.2")
    sslContext.init(keyManagerFactory.keyManagers, null, null)
    return sslContext.serverSocketFactory.createServerSocket(port)
}

internal fun getTrustingContext() =
        SSLContext.getInstance("TLSv1.2").apply { init(null, arrayOf(getTrustingManager()), null) }

internal fun getTrustingManager() = object : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}

    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
}