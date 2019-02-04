package com.dmdirc.ktirc.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerStateTest {

    @Test
    fun `ServerState should use the initial nickname as local nickname`() {
        val serverState = ServerState("acidBurn", "")
        assertEquals("acidBurn", serverState.localNickname)
    }

    @Test
    fun `ServerState should use the initial name as server name`() {
        val serverState = ServerState("", "the.gibson")
        assertEquals("the.gibson", serverState.serverName)
    }

    @Test
    fun `ServerState should default status to connecting`() {
        val serverState = ServerState("acidBurn", "")
        assertEquals(ServerStatus.Connecting, serverState.status)
    }

}

internal class ModePrefixMappingTest {

    @Test
    fun `ModePrefixMapping identifies which chars are prefixes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertTrue(mapping.isPrefix('+'))
        assertTrue(mapping.isPrefix('@'))
        assertFalse(mapping.isPrefix('!'))
        assertFalse(mapping.isPrefix('o'))
    }

    @Test
    fun `ModePrefixMapping maps prefixes to modes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertEquals('o', mapping.getMode('+'))
        assertEquals('a', mapping.getMode('@'))
        assertEquals('v', mapping.getMode('-'))
    }

    @Test
    fun `ModePrefixMapping maps prefix strings to modes`() {
        val mapping = ModePrefixMapping("oav", "+@-")
        assertEquals("oa", mapping.getModes("+@"))
        assertEquals("o", mapping.getModes("+"))
        assertEquals("", mapping.getModes(""))
        assertEquals("vao", mapping.getModes("-@+"))
    }

}