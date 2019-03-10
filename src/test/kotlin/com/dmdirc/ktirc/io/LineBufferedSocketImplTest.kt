package com.dmdirc.ktirc.io

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.ServerSocket

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
@Execution(ExecutionMode.SAME_THREAD)
internal class LineBufferedSocketImplTest {

    @Test
    fun `can connect to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
            val clientSocketAsync = GlobalScope.async { serverSocket.accept() }

            socket.connect()

            assertNotNull(clientSocketAsync.await())
        }
    }

    @Test
    fun `can send a byte array to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
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
    fun `can send a string to a server over TLS`() = runBlocking {
        tlsServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321, true)
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
    fun `can receive a line of CRLF delimited text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\r\n".toByteArray())
            }

            socket.connect()
            assertEquals("Hi there", String(socket.receiveChannel.receive()))
        }
    }

    @Test
    fun `can receive a line of LF delimited text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\n".toByteArray())
            }

            socket.connect()
            assertEquals("Hi there", String(socket.receiveChannel.receive()))
        }
    }

    @Test
    fun `can receive multiple lines of text in one packet`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
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
    fun `can receive multiple long lines of text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
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
    fun `can receive one line of text over multiple packets`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
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
    fun `returns from readLines when socket is closed`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
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
    fun `disconnects from server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = LineBufferedSocketImpl(GlobalScope, "localhost", "localhost", 12321)
            val clientSocketAsync = GlobalScope.async { serverSocket.accept() }

            socket.connect()
            socket.disconnect()

            assertEquals(-1, clientSocketAsync.await().getInputStream().read()) { "Server socket should EOF after KtorLineBufferedSocket disconnects" }
        }
    }

}