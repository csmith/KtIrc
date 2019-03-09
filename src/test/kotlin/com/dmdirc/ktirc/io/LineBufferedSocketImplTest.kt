package com.dmdirc.ktirc.io

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.ServerSocket
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@ExperimentalCoroutinesApi
@Execution(ExecutionMode.SAME_THREAD)
internal class LineBufferedSocketImplTest {

    @Test
    fun `KtorLineBufferedSocket can connect to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            val clientSocketAsync = GlobalScope.async { serverSocket.accept() }

            socket.connect()

            assertNotNull(clientSocketAsync.await())
        }
    }

    @Test
    fun `KtorLineBufferedSocket can send a byte array to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            val clientBytesAsync = GlobalScope.async {
                ByteArray(13).apply {
                    serverSocket.accept().getInputStream().read(this)
                }
            }

            socket.connect()
            socket.sendChannel.send("Hello World".toByteArray())

            val bytes = clientBytesAsync.await()
            assertNotNull(bytes)
            assertEquals("Hello World\r\n", String(bytes))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can send a string to a server over TLS`() = runBlocking {
        tlsServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321, true)
            socket.tlsTrustManager = getTrustingManager()
            val clientBytesAsync = GlobalScope.async {
                ByteArray(13).apply {
                    serverSocket.accept().getInputStream().read(this)
                }
            }

            socket.connect()
            socket.sendChannel.send("Hello World".toByteArray())

            val bytes = clientBytesAsync.await()
            assertNotNull(bytes)
            assertEquals("Hello World\r\n", String(bytes))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive a line of CRLF delimited text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\r\n".toByteArray())
            }

            socket.connect()
            assertEquals("Hi there", String(socket.receiveChannel.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive a line of LF delimited text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\n".toByteArray())
            }

            socket.connect()
            assertEquals("Hi there", String(socket.receiveChannel.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive multiple lines of text in one packet`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\nThis is a test\r".toByteArray())
            }

            socket.connect()
            val lineProducer = socket.receiveChannel
            assertEquals("Hi there", String(lineProducer.receive()))
            assertEquals("This is a test", String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive multiple long lines of text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            val line1 = "abcdefghijklmnopqrstuvwxyz".repeat(500)
            val line2 = "1234567890987654321[];'#,.".repeat(500)
            val line3 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(500)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("$line1\r\n$line2\r$line3\n".toByteArray())
            }

            socket.connect()
            val lineProducer = socket.receiveChannel
            assertEquals(line1, String(lineProducer.receive()))
            assertEquals(line2, String(lineProducer.receive()))
            assertEquals(line3, String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive one line of text over multiple packets`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            GlobalScope.launch {
                with(serverSocket.accept().getOutputStream()) {
                    write("Hi".toByteArray())
                    flush()
                    write(" t".toByteArray())
                    flush()
                    write("here\r\n".toByteArray())
                    flush()
                }
            }

            socket.connect()
            val lineProducer = socket.receiveChannel
            assertEquals("Hi there", String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket returns from readLines when socket is closed`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            GlobalScope.launch {
                with(serverSocket.accept()) {
                    getOutputStream().write("Hi there\r\n".toByteArray())
                    close()
                }
            }

            socket.connect()
            val lineProducer = socket.receiveChannel
            assertEquals("Hi there", String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket disconnects from server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", 12321)
            val clientSocketAsync = GlobalScope.async { serverSocket.accept() }

            socket.connect()
            socket.disconnect()

            assertEquals(-1, clientSocketAsync.await().getInputStream().read()) { "Server socket should EOF after KtorLineBufferedSocket disconnects" }
        }
    }

    private fun tlsServerSocket(port: Int): ServerSocket {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(LineBufferedSocketImplTest::class.java.getResourceAsStream("localhost.p12"), CharArray(0))

        val keyManagerFactory = KeyManagerFactory.getInstance("PKIX")
        keyManagerFactory.init(keyStore, CharArray(0))

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(keyManagerFactory.keyManagers, null, null)
        return sslContext.serverSocketFactory.createServerSocket(port)
    }

    private fun getTrustingManager() = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate>  = emptyArray()

        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}

        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    }


}