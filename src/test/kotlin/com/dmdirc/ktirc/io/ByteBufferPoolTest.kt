package com.dmdirc.ktirc.io

import kotlinx.io.pool.useInstance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

internal class ByteBufferPoolTest {

    @Test
    fun `it allows borrowing of multiple unique bytebuffers`() {
        val pool = ByteBufferPool(5, 10)
        val buffer1 = pool.borrow()
        val buffer2 = pool.borrow()
        val buffer3 = pool.borrow()

        assertFalse(buffer1 === buffer2)
        assertFalse(buffer2 === buffer3)
        assertFalse(buffer1 === buffer3)
    }

    @Test
    fun `it produces buffers of the correct size`() {
        val pool = ByteBufferPool(5, 12)
        val buffer = pool.borrow()
        assertEquals(12, buffer.limit())
    }

    @Test
    fun `it reuses recycled buffers`() {
        val pool = ByteBufferPool(1, 10)

        val buffer1 = pool.borrow()
        pool.recycle(buffer1)

        val buffer2 = pool.borrow()
        assertTrue(buffer1 === buffer2)
    }

    @Test
    fun `it resets buffers when reborrowing`() {
        val pool = ByteBufferPool(1, 10)
        val buffer1 = pool.borrow()
        buffer1.put("31137".toByteArray())
        pool.recycle(buffer1)

        val buffer2 = pool.borrow()
        assertTrue(buffer1 === buffer2)
        assertEquals(0, buffer2.position())
        assertEquals(10, buffer2.limit())
    }

    @Test
    fun `borrow with block automatically returns`() {
        val pool = ByteBufferPool(1, 10)
        var buffer1: ByteBuffer? = null
        pool.useInstance {  buffer1 = it }
        val buffer2 = pool.borrow()
        assertTrue(buffer1 === buffer2)
    }

}