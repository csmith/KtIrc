package com.dmdirc.ktirc

import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.model.Profile
import com.dmdirc.ktirc.model.Server
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.User
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class IrcClientImplTest {

    companion object {
        private const val HOST = "thegibson.com"
        private const val PORT = 12345
        private const val NICK = "AcidBurn"
        private const val REAL_NAME = "Kate Libby"
        private const val USER_NAME = "acidb"
        private const val PASSWORD = "HackThePlanet"
    }

    private val readLineChannel = Channel<ByteArray>(10)

    private val mockSocket = mock<LineBufferedSocket> {
        on { readLines(any()) } doReturn readLineChannel
    }

    private val mockSocketFactory = mock<(String, Int) -> LineBufferedSocket> {
        on { invoke(HOST, PORT) } doReturn mockSocket
    }

    @Test
    fun `IrcClientImpl uses socket factory to create a new socket on connect`() {
        runBlocking {
            val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
            client.socketFactory = mockSocketFactory
            readLineChannel.close()

            client.connect()

            verify(mockSocketFactory).invoke(HOST, PORT)
        }
    }

    @Test
    fun `IrcClientImpl throws if socket already exists`() {
        runBlocking {
            val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
            client.socketFactory = mockSocketFactory
            readLineChannel.close()

            client.connect()

            assertThrows<IllegalStateException> {
                runBlocking {
                    client.connect()
                }
            }
        }
    }

    @Test
    fun `IrcClientImpl sends basic connection strings`() {
        runBlocking {
            val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
            client.socketFactory = mockSocketFactory
            readLineChannel.close()

            client.connect()

            with(inOrder(mockSocket).verify(mockSocket)) {
                sendLine("NICK :$NICK")
                sendLine("USER $USER_NAME localhost $HOST :$REAL_NAME")
            }
        }
    }

    @Test
    fun `IrcClientImpl sends password first, when present`() {
        runBlocking {
            val client = IrcClientImpl(Server(HOST, PORT, password = PASSWORD), Profile(NICK, REAL_NAME, USER_NAME))
            client.socketFactory = mockSocketFactory
            readLineChannel.close()

            client.connect()

            with(inOrder(mockSocket).verify(mockSocket)) {
                sendLine("PASS :$PASSWORD")
                sendLine("NICK :$NICK")
            }
        }
    }

    @Test
    fun `IrcClient gets case mapping from server features`() {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.serverState.features[ServerFeature.ServerCaseMapping] = CaseMapping.RfcStrict
        assertEquals(CaseMapping.RfcStrict, client.caseMapping)
    }

    @Test
    fun `IrcClient indicates if user is local user or not`() {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.serverState.localNickname = "[acidBurn]"

        assertTrue(client.isLocalUser(User("{acidBurn}", "libby", "root.localhost")))
        assertFalse(client.isLocalUser(User("acid-Burn", "libby", "root.localhost")))
    }

    @Test
    fun `IrcClient uses current case mapping to check local user`() {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.serverState.localNickname = "[acidBurn]"
        client.serverState.features[ServerFeature.ServerCaseMapping] = CaseMapping.Ascii
        assertFalse(client.isLocalUser(User("{acidBurn}", "libby", "root.localhost")))
    }

}