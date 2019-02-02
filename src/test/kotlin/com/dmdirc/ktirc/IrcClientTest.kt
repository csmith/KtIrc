package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.model.*
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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

    private val mockSocketFactory = mock<(String, Int, Boolean) -> LineBufferedSocket> {
        on { invoke(eq(HOST), eq(PORT), any()) } doReturn mockSocket
    }

    private val mockEventHandler = mock<(IrcEvent) -> Unit>()

    @BeforeEach
    fun setUp() {
        IrcMessage.currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `IrcClientImpl uses socket factory to create a new socket on connect`() {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        verify(mockSocketFactory, timeout(500)).invoke(HOST, PORT, false)
    }

    @Test
    fun `IrcClientImpl uses socket factory to create a new tls on connect`() {
        val client = IrcClientImpl(Server(HOST, PORT, true), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        verify(mockSocketFactory, timeout(500)).invoke(HOST, PORT, true)
    }

    @Test
    fun `IrcClientImpl throws if socket already exists`() {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        assertThrows<IllegalStateException> {
            client.connect()
        }
    }

    @Test
    fun `IrcClientImpl sends basic connection strings`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        with(inOrder(mockSocket).verify(mockSocket, timeout(500))) {
            sendLine("CAP LS 302")
            sendLine("NICK :$NICK")
            sendLine("USER $USER_NAME localhost $HOST :$REAL_NAME")
        }
    }

    @Test
    fun `IrcClientImpl sends password first, when present`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT, password = PASSWORD), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        with(inOrder(mockSocket).verify(mockSocket, timeout(500))) {
            sendLine("CAP LS 302")
            sendLine("PASS :$PASSWORD")
            sendLine("NICK :$NICK")
        }
    }

    @Test
    fun `IrcClientImpl sends events to provided event handler`() {
        val client = IrcClientImpl(Server(HOST, PORT, password = PASSWORD), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.onEvent(mockEventHandler)

        GlobalScope.launch {
            readLineChannel.send(":the.gibson 001 acidBurn :Welcome to the IRC!".toByteArray())
        }

        client.connect()

        verify(mockEventHandler, timeout(500)).invoke(isA<ServerWelcome>())
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

    @Test
    fun `IrcClientImpl join blocks when socket is open`() {
        val client = IrcClientImpl(Server(HOST, PORT, password = PASSWORD), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory

        GlobalScope.launch {
            readLineChannel.send(":the.gibson 001 acidBurn :Welcome to the IRC!".toByteArray())
        }

        client.connect()
        runBlocking {
            assertNull(withTimeoutOrNull(100L) {
                client.join()
                true
            })
        }
    }

    @Test
    fun `IrcClientImpl join returns when socket is closed`() {
        val client = IrcClientImpl(Server(HOST, PORT, password = PASSWORD), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory

        GlobalScope.launch {
            readLineChannel.send(":the.gibson 001 acidBurn :Welcome to the IRC!".toByteArray())
            readLineChannel.close()
        }

        client.connect()
        runBlocking {
            assertEquals(true, withTimeoutOrNull(500L) {
                client.join()
                true
            })
        }
    }

    @Test
    fun `IrcClientImpl sends text to socket`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        // Wait for it to connect
        verify(mockSocket, timeout(500)).sendLine("CAP LS 302")

        client.send("testing 123")

        verify(mockSocket, timeout(500)).sendLine("testing 123")
    }

    @Test
    fun `IrcClientImpl disconnects the socket`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        // Wait for it to connect
        verify(mockSocket, timeout(500)).sendLine("CAP LS 302")

        client.disconnect()

        verify(mockSocket, timeout(500)).disconnect()
    }


}