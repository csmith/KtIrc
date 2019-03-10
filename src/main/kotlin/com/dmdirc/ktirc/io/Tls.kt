package com.dmdirc.ktirc.io

import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.launch
import kotlinx.io.pool.useInstance
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.regex.Pattern
import javax.naming.ldap.LdapName
import javax.naming.ldap.Rdn
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult


internal class TlsSocket(
        private val scope: CoroutineScope,
        private val socket: Socket,
        private val sslContext: SSLContext,
        private val hostname: String
) : Socket {

    private val log by logger()
    private var engine: SSLEngine = sslContext.createSSLEngine()

    private var incomingNetBuffer = ByteBuffer.allocate(0)
    private var incomingAppBuffer = ByteBuffer.allocate(0)
    private var outgoingAppBuffers = Channel<ByteBuffer>(capacity = Channel.UNLIMITED)

    private var writeChannel = ByteChannel(autoFlush = true)

    override val write: ByteWriteChannel
        get() = writeChannel

    override val isOpen: Boolean
        get() = socket.isOpen

    override fun bind(socketAddress: SocketAddress) {
        socket.bind(socketAddress)
    }

    override suspend fun connect(socketAddress: SocketAddress) {
        writeChannel = ByteChannel(autoFlush = true)

        engine = sslContext.createSSLEngine().apply {
            useClientMode = true
        }

        incomingNetBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
        outgoingAppBuffers = Channel(capacity = Channel.UNLIMITED)
        incomingAppBuffer = ByteBuffer.allocate(engine.session.applicationBufferSize)

        socket.connect(socketAddress)

        engine.beginHandshake()

        sslLoop()
    }

    private suspend fun sslLoop(initialResult: SSLEngineResult? = null) {
        var result: SSLEngineResult? = initialResult
        var handshakeStatus = result?.handshakeStatus ?: engine.handshakeStatus
        while (true) {
            when (handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    engine.delegatedTask.run()
                    handshakeStatus = engine.handshakeStatus
                }
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    result = wrap()
                    handshakeStatus = result?.handshakeStatus
                }

                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                    result = unwrap()
                    handshakeStatus = result?.handshakeStatus
                }

                SSLEngineResult.HandshakeStatus.FINISHED -> {
                    val certs = engine.session.peerCertificates
                    if (certs.isEmpty() || (certs[0] as? X509Certificate)?.validFor(hostname) == false) {
                        throw CertificateException("Certificate is not valid for $hostname")
                    }
                    scope.launch { readLoop() }
                    scope.launch { writeLoop() }
                    return
                }

                else -> return
            }
        }
    }

    override suspend fun read() = try {
        outgoingAppBuffers.receive()
    } catch (_: ClosedReceiveChannelException) {
        null
    }

    private suspend fun wrap(): SSLEngineResult? {
        var result: SSLEngineResult? = null
        byteBufferPool.useInstance { netBuffer ->
            if (engine.handshakeStatus <= SSLEngineResult.HandshakeStatus.FINISHED) {
                writeChannel.readAvailable(incomingAppBuffer)
            }
            incomingAppBuffer.flip()
            result = engine.wrap(incomingAppBuffer, netBuffer)
            incomingAppBuffer.compact()

            netBuffer.flip()
            socket.write.writeFully(netBuffer)
        }
        return result
    }

    private suspend fun unwrap(networkRead: Boolean = incomingNetBuffer.position() == 0): SSLEngineResult? {
        if (networkRead) {
            val buffer = socket.read()
            if (buffer == null) {
                close()
                return null
            }
            incomingNetBuffer.put(buffer)
            byteBufferPool.recycle(buffer)
        }

        incomingNetBuffer.flip()

        val buffer = byteBufferPool.borrow()
        val result = engine.unwrap(incomingNetBuffer, buffer)
        incomingNetBuffer.compact()
        if (buffer.position() > 0) {
            buffer.flip()
            outgoingAppBuffers.send(buffer)
        } else {
            byteBufferPool.recycle(buffer)
        }

        return if (result?.status == SSLEngineResult.Status.BUFFER_UNDERFLOW && !networkRead) {
            // We didn't do a network read, but SSLEngine is unhappy; force a read.
            log.finest { "Incoming net buffer underflowed, forcing re-read" }
            unwrap(true)
        } else {
            result
        }
    }

    override fun close() {
        socket.close()

        // Release any buffers we've got queued up
        while (true) {
            outgoingAppBuffers.poll()?.let {
                byteBufferPool.recycle(it)
            } ?: break
        }

        outgoingAppBuffers.close()
    }

    private suspend fun readLoop() {
        while (socket.isOpen) {
            sslLoop(unwrap())
        }
    }

    private suspend fun writeLoop() {
        while (socket.isOpen) {
            sslLoop(wrap())
        }
    }

}

internal fun X509Certificate.validFor(host: String): Boolean {
    val hostParts = host.split('.')
    return allNames
            .map { it.split('.') }
            .filter { it.size == hostParts.size }
            .filter { it[0].wildCardMatches(hostParts[0]) }
            .any { it.zip(hostParts).slice(1 until hostParts.size).all { (part, host) -> part.equals(host, ignoreCase = true) } }
}

private fun String.wildCardMatches(host: String) =
        count { it == '*' } <= 1 &&
                host.matches(Regex(split('*').joinToString(".*") { Pattern.quote(it) }, RegexOption.IGNORE_CASE))

private val X509Certificate.allNames: Sequence<String>
    get() = sequence {
        commonName?.let { yield(it) }
        yieldAll(subjectAlternateNames)
    }

private val X509Certificate.subjectAlternateNames: Set<String>
    get() = nullOnThrow {
        subjectAlternativeNames
                ?.filter { it[0] == 2 }
                ?.map { it[1].toString() }
                ?.toSet()
    } ?: emptySet()

private val X509Certificate.commonName: String?
    get() = nullOnThrow { rdns["CN"]?.firstOrNull()?.value?.toString() }

private val X509Certificate.rdns: Map<String, List<Rdn>>
    get() = LdapName(subjectX500Principal.name).rdns.groupBy { it.type.toUpperCase() }

private inline fun <S> nullOnThrow(block: () -> S?): S? = try {
    block()
} catch (ex: Throwable) {
    null
}
