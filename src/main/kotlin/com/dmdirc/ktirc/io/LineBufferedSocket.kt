package com.dmdirc.ktirc.io

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import java.net.InetSocketAddress

interface LineBufferedSocket {

    // TODO: This is a bit pants.
    var debugReceiver: ((String) -> Unit)?

    suspend fun connect()
    fun disconnect()

    suspend fun sendLine(line: ByteArray, offset: Int = 0, length: Int = line.size)
    suspend fun sendLine(line: String)

    fun readLines(coroutineScope: CoroutineScope): ReceiveChannel<ByteArray>

}

/**
 * Asynchronous socket that buffers incoming data and emits individual lines.
 */
// TODO: TLS options
class KtorLineBufferedSocket(private val host: String, private val port: Int): LineBufferedSocket {

    companion object {
        const val CARRIAGE_RETURN = '\r'.toByte()
        const val LINE_FEED = '\n'.toByte()
    }

    // TODO: This is a bit pants.
    override var debugReceiver: ((String) -> Unit)? = null

    private lateinit var socket: Socket
    private lateinit var readChannel: ByteReadChannel
    private lateinit var writeChannel: ByteWriteChannel

    override suspend fun connect() {
        socket = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().connect(InetSocketAddress(host, port))
        readChannel = socket.openReadChannel()
        writeChannel = socket.openWriteChannel()
    }

    override fun disconnect() {
        socket.close()
    }

    override suspend fun sendLine(line: ByteArray, offset: Int, length: Int) {
        with (writeChannel) {
            debugReceiver?.let { it(">>> ${String(line, offset, length)}") }
            writeAvailable(line, offset, length)
            writeByte(CARRIAGE_RETURN)
            writeByte(LINE_FEED)
            flush()
        }
    }

    override suspend fun sendLine(line: String) = sendLine(line.toByteArray())

    override fun readLines(coroutineScope: CoroutineScope) = coroutineScope.produce {
        val lineBuffer = ByteArray(4096)
        var index = 0
        while (!readChannel.isClosedForRead) {
            var start = index
            val count = readChannel.readAvailable(lineBuffer, index, lineBuffer.size - index)
            for (i in index until index + count) {
                if (lineBuffer[i] == CARRIAGE_RETURN || lineBuffer[i] == LINE_FEED) {
                    if (start < i) {
                        val line = lineBuffer.sliceArray(start until i)
                        debugReceiver?.let { it("<<< ${String(line)}") }
                        send(line)
                    }
                    start = i + 1
                }
            }
            lineBuffer.copyInto(lineBuffer, 0, start)
            index = count + index - start
        }
    }
}