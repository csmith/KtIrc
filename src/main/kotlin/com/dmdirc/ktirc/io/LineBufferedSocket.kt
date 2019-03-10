package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.io.core.String
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
            defaultPool.borrow { lineBuffer ->
                while (socket.isOpen) {
                    defaultPool.borrow { buffer ->
                        val bytesRead = socket.read(buffer)
                        var lastLine = 0
                        for (i in 0 until bytesRead) {
                            if (buffer[i] == CARRIAGE_RETURN || buffer[i] == LINE_FEED) {
                                val length = i - lastLine + lineBuffer.position()

                                if (length > 1) {
                                    val output = ByteBuffer.allocate(length)

                                    lineBuffer.flip()
                                    output.put(lineBuffer)
                                    lineBuffer.clear()

                                    output.put(buffer.array(), lastLine, i - lastLine)
                                    log.fine { "<<< ${String(output.array())}" }
                                    send(output.array())
                                }

                                lastLine = i + 1
                            }
                        }
                        lineBuffer.put(buffer.array(), lastLine, bytesRead - lastLine)
                    }
                }
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
