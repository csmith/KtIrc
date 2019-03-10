package com.dmdirc.ktirc.io

import kotlinx.io.pool.DefaultPool
import java.nio.ByteBuffer

private const val BUFFER_SIZE = 32768
private const val MAXIMUM_POOL_SIZE = 1024

internal val byteBufferPool = ByteBufferPool(MAXIMUM_POOL_SIZE, BUFFER_SIZE)

internal class ByteBufferPool(maximumPoolSize: Int, private val bufferSize: Int) : DefaultPool<ByteBuffer>(maximumPoolSize) {
    override fun produceInstance(): ByteBuffer = ByteBuffer.allocate(bufferSize)
    override fun clearInstance(instance: ByteBuffer): ByteBuffer = instance.apply { clear() }
}
