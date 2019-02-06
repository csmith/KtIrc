package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.util.logger
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import java.net.InetSocketAddress
import java.security.SecureRandom
import javax.net.ssl.X509TrustManager

internal interface LineBufferedSocket {

    fun connect()
    fun disconnect()

    val sendChannel: SendChannel<ByteArray>
    val receiveChannel: ReceiveChannel<ByteArray>

}

/**
 * Asynchronous socket that buffers incoming data and emits individual lines.
 */
// TODO: Expose advanced TLS options
@KtorExperimentalAPI
@ExperimentalCoroutinesApi
internal class KtorLineBufferedSocket(coroutineScope: CoroutineScope, private val host: String, private val port: Int, private val tls: Boolean = false) : CoroutineScope, LineBufferedSocket {

    companion object {
        const val CARRIAGE_RETURN = '\r'.toByte()
        const val LINE_FEED = '\n'.toByte()
    }

    override val coroutineContext = coroutineScope.newCoroutineContext(Dispatchers.IO)
    override val sendChannel: Channel<ByteArray> = Channel(Channel.UNLIMITED)

    var tlsTrustManager: X509TrustManager? = null

    private val log by logger()

    private lateinit var socket: Socket
    private lateinit var readChannel: ByteReadChannel
    private lateinit var writeChannel: ByteWriteChannel

    override fun connect() {
        runBlocking {
            log.info { "Connecting..." }
            socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(host, port))
            if (tls) {
                socket = socket.tls(
                        coroutineContext = this@KtorLineBufferedSocket.coroutineContext,
                        randomAlgorithm = SecureRandom.getInstanceStrong().algorithm,
                        trustManager = tlsTrustManager)
            }
            readChannel = socket.openReadChannel()
            writeChannel = socket.openWriteChannel()
        }
        launch { writeLines() }
    }

    override fun disconnect() {
        log.info { "Disconnecting..." }
        socket.close()
        coroutineContext.cancel()
    }

    override val receiveChannel
        get() = produce {
            val lineBuffer = ByteArray(4096)
            var index = 0
            while (!readChannel.isClosedForRead) {
                var start = index
                val count = readChannel.readAvailable(lineBuffer, index, lineBuffer.size - index)
                for (i in index until index + count) {
                    if (lineBuffer[i] == CARRIAGE_RETURN || lineBuffer[i] == LINE_FEED) {
                        if (start < i) {
                            val line = lineBuffer.sliceArray(start until i)
                            log.fine { "<<< ${String(line)}" }
                            send(line)
                        }
                        start = i + 1
                    }
                }
                lineBuffer.copyInto(lineBuffer, 0, start)
                index = count + index - start
            }
        }

    private suspend fun writeLines() {
        for (line in sendChannel) {
            with(writeChannel) {
                log.fine { ">>> ${String(line)}" }
                writeAvailable(line, 0, line.size)
                writeByte(CARRIAGE_RETURN)
                writeByte(LINE_FEED)
                flush()
            }
        }
    }
}
