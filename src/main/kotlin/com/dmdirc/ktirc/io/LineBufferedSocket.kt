package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.io.ByteWriteChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.CertificateException
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal interface LineBufferedSocket {

    @Throws(CertificateException::class)
    fun connect()

    fun disconnect()

    val sendChannel: SendChannel<ByteArray>
    val receiveChannel: ReceiveChannel<ByteArray>

}

/**
 * Asynchronous socket that buffers incoming data and emits individual lines.
 */
// TODO: Expose advanced TLS options
@ExperimentalCoroutinesApi
internal class LineBufferedSocketImpl(coroutineScope: CoroutineScope, private val host: String, private val ip: String, private val port: Int, private val tls: Boolean = false) : CoroutineScope, LineBufferedSocket {

    companion object {
        const val CARRIAGE_RETURN = '\r'.toByte()
        const val LINE_FEED = '\n'.toByte()
    }

    override val coroutineContext = coroutineScope.newCoroutineContext(Dispatchers.IO)
    override val sendChannel: Channel<ByteArray> = Channel(Channel.UNLIMITED)

    var tlsTrustManager: X509TrustManager? = null

    private val log by logger()

    private lateinit var socket: Socket
    private lateinit var writeChannel: ByteWriteChannel

    override fun connect() {
        log.info { "Connecting..." }
        socket = PlainTextSocket(this)

        runBlocking {
            if (tls) {
                with (SSLContext.getInstance("TLSv1.2")) {
                    init(null, tlsTrustManager?.let { arrayOf(it) }, SecureRandom.getInstanceStrong())
                    socket = TlsSocket(this@LineBufferedSocketImpl, socket, this, host)
                }
            }
            socket.connect(InetSocketAddress(ip, port))
            writeChannel = socket.write
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
            val lineBuffer = ByteArray(16384)
            var nextByteOffset = 0
            while (socket.isOpen) {
                var lineStart = 0
                val bytesRead = socket.read(ByteBuffer.wrap(lineBuffer).apply { position(nextByteOffset) })
                for (i in nextByteOffset until nextByteOffset + bytesRead) {
                    if (lineBuffer[i] == CARRIAGE_RETURN || lineBuffer[i] == LINE_FEED) {
                        if (lineStart < i) {
                            val line = lineBuffer.sliceArray(lineStart until i)
                            log.fine { "<<< ${String(line)}" }
                            send(line)
                        }
                        lineStart = i + 1
                    }
                }
                lineBuffer.copyInto(lineBuffer, 0, lineStart)
                nextByteOffset += bytesRead - lineStart
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
