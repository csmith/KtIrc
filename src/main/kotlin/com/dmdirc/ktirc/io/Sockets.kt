package com.dmdirc.ktirc.io

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.close
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.pool.useInstance
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal interface Socket {
    fun bind(socketAddress: SocketAddress)
    suspend fun connect(socketAddress: SocketAddress)
    suspend fun read(): ByteBuffer?
    fun close()
    val write: ByteWriteChannel
    val isOpen: Boolean
}

internal class PlainTextSocket(private val scope: CoroutineScope) : Socket {

    private val client = AsynchronousSocketChannel.open()
    private var writeChannel = ByteChannel(autoFlush = true)

    override val write: ByteWriteChannel
        get() = writeChannel

    override val isOpen: Boolean
        get() = client.isOpen

    override fun bind(socketAddress: SocketAddress) {
        client.bind(socketAddress)
    }

    override suspend fun connect(socketAddress: SocketAddress) {
        writeChannel = ByteChannel(autoFlush = true)

        suspendCancellableCoroutine<Unit> { continuation ->
            client.closeOnCancel(continuation)
            client.connect(socketAddress, continuation, AsyncVoidIOHandler)
        }

        scope.launch { writeLoop() }
    }

    override fun close() {
        writeChannel.close()
        client.close()
    }

    override suspend fun read(): ByteBuffer? {
        val buffer = byteBufferPool.borrow()
        try {
            val bytes = suspendCancellableCoroutine<Int> { continuation ->
                client.closeOnCancel(continuation)
                client.read(buffer, continuation, asyncIOHandler())
            }

            if (bytes == -1) {
                close()
            }

            buffer.flip()
            return buffer
        } catch (_: ClosedChannelException) {
            // Ignore
            byteBufferPool.recycle(buffer)
            return null
        }
    }

    private suspend fun writeLoop() {
        while (client.isOpen) {
            byteBufferPool.useInstance { buffer ->
                writeChannel.readAvailable(buffer)
                buffer.flip()
                try {
                    suspendCancellableCoroutine<Int> { continuation ->
                        client.closeOnCancel(continuation)
                        client.write(buffer, continuation, asyncIOHandler())
                    }
                } catch (_: ClosedChannelException) {
                    // Ignore
                }
            }
        }
    }

}

private fun Channel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        try {
            close()
        } catch (ex: Throwable) {
            // Specification says that it is Ok to call it any time, but reality is different,
            // so we have just to ignore exception
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> asyncIOHandler(): CompletionHandler<T, CancellableContinuation<T>> =
        AsyncIOHandlerAny as CompletionHandler<T, CancellableContinuation<T>>

private object AsyncIOHandlerAny : CompletionHandler<Any, CancellableContinuation<Any>> {
    override fun completed(result: Any, cont: CancellableContinuation<Any>) {
        cont.resume(result)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Any>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}

private object AsyncVoidIOHandler : CompletionHandler<Void?, CancellableContinuation<Unit>> {
    override fun completed(result: Void?, cont: CancellableContinuation<Unit>) {
        cont.resume(Unit)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Unit>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}