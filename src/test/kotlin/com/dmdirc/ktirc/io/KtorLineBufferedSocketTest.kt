package com.dmdirc.ktirc.io

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.ServerSocket

@Execution(ExecutionMode.SAME_THREAD)
internal class KtorLineBufferedSocketTest {

    @Test
    fun `KtorLineBufferedSocket can connect to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            val clientSocketAsync = GlobalScope.async { serverSocket.accept() }

            socket.connect()

            assertNotNull(clientSocketAsync.await())
        }
    }

    @Test
    fun `KtorLineBufferedSocket can send a whole byte array to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            val clientBytesAsync = GlobalScope.async {
                ByteArray(13).apply {
                    serverSocket.accept().getInputStream().read(this)
                }
            }

            socket.connect()
            socket.sendLine("Hello World".toByteArray())

            val bytes = clientBytesAsync.await()
            assertNotNull(bytes)
            assertEquals("Hello World\r\n", String(bytes))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can send a string to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            val clientBytesAsync = GlobalScope.async {
                ByteArray(13).apply {
                    serverSocket.accept().getInputStream().read(this)
                }
            }

            socket.connect()
            socket.sendLine("Hello World")

            val bytes = clientBytesAsync.await()
            assertNotNull(bytes)
            assertEquals("Hello World\r\n", String(bytes))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can send a partial byte array to a server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            val clientBytesAsync = GlobalScope.async {
                ByteArray(7).apply {
                    serverSocket.accept().getInputStream().read(this)
                }
            }

            socket.connect()
            socket.sendLine("Hello World".toByteArray(), 6, 5)

            val bytes = clientBytesAsync.await()
            assertNotNull(bytes)
            assertEquals("World\r\n", String(bytes))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive a line of CRLF delimited text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\r\n".toByteArray())
            }

            socket.connect()
            assertEquals("Hi there", String(socket.readLines(GlobalScope).receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive a line of LF delimited text`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\n".toByteArray())
            }

            socket.connect()
            assertEquals("Hi there", String(socket.readLines(GlobalScope).receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive multiple lines of text in one packet`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            GlobalScope.launch {
                serverSocket.accept().getOutputStream().write("Hi there\nThis is a test\r".toByteArray())
            }

            socket.connect()
            val lineProducer = socket.readLines(GlobalScope)
            assertEquals("Hi there", String(lineProducer.receive()))
            assertEquals("This is a test", String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket can receive one line of text over multiple packets`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
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
            val lineProducer = socket.readLines(GlobalScope)
            assertEquals("Hi there", String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket returns from readLines when socket is closed`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            GlobalScope.launch {
                with(serverSocket.accept()) {
                    getOutputStream().write("Hi there\r\n".toByteArray())
                    close()
                }
            }

            socket.connect()
            val lineProducer = socket.readLines(GlobalScope)
            assertEquals("Hi there", String(lineProducer.receive()))
        }
    }

    @Test
    fun `KtorLineBufferedSocket disconnects from server`() = runBlocking {
        ServerSocket(12321).use { serverSocket ->
            val socket = KtorLineBufferedSocket("localhost", 12321)
            val clientSocketAsync = GlobalScope.async { serverSocket.accept() }

            socket.connect()
            socket.disconnect()

            assertEquals(-1, clientSocketAsync.await().getInputStream().read()) { "Server socket should EOF after KtorLineBufferedSocket disconnects" }
        }
    }

}