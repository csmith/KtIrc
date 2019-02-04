package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.model.Profile
import com.dmdirc.ktirc.model.Server
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.util.currentTimeProvider
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
        currentTimeProvider = { TestConstants.time }
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
    fun `IrcClientImpl emits connected event with local time`() = runBlocking {
        currentTimeProvider = { TestConstants.time }
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.onEvent(mockEventHandler)
        client.connect()

        val captor = argumentCaptor<ServerConnected>()
        verify(mockEventHandler, timeout(500)).invoke(captor.capture())
        assertEquals(TestConstants.time, captor.firstValue.time)
    }

    @Test
    fun `IrcClientImpl sends basic connection strings`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        client.blockUntilConnected()

        assertEquals("CAP LS 302", String(client.writeChannel!!.receive()))
        assertEquals("NICK :$NICK", String(client.writeChannel!!.receive()))
        assertEquals("USER $USER_NAME localhost $HOST :$REAL_NAME", String(client.writeChannel!!.receive()))
    }

    @Test
    fun `IrcClientImpl sends password first, when present`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT, password = PASSWORD), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        client.blockUntilConnected()

        assertEquals("CAP LS 302", String(client.writeChannel!!.receive()))
        assertEquals("PASS :$PASSWORD", String(client.writeChannel!!.receive()))
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

        client.blockUntilConnected()

        client.send("testing 123")

        assertEquals(true, withTimeoutOrNull(500) {
            var found = false
            for (line in client.writeChannel!!) {
                if (String(line) == "testing 123") {
                    found = true
                    break
                }
            }
            found
        })
    }

    @Test
    fun `IrcClientImpl disconnects the socket`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        client.blockUntilConnected()

        client.disconnect()

        verify(mockSocket, timeout(500)).disconnect()
    }

    @Test
    fun `IrcClientImpl sends messages in order`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        client.socketFactory = mockSocketFactory
        client.connect()

        client.blockUntilConnected()

        (0..100).forEach { client.send("TEST $it") }

        assertEquals(100, withTimeoutOrNull(500) {
            var next = 0
            for (line in client.writeChannel!!) {
                val stringy = String(line)
                if (stringy.startsWith("TEST ")) {
                    assertEquals("TEST $next", stringy)
                    if (++next == 100) {
                        break
                    }
                }
            }
            next
        })
    }

    @Test
    fun `IrcClientImpl defaults local nickname to profile`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        assertEquals(NICK, client.serverState.localNickname)
    }

    @Test
    fun `IrcClientImpl defaults server name to host name`() = runBlocking {
        val client = IrcClientImpl(Server(HOST, PORT), Profile(NICK, REAL_NAME, USER_NAME))
        assertEquals(HOST, client.serverState.serverName)
    }

    private suspend fun IrcClientImpl.blockUntilConnected() {
        // Yuck. Maybe connect should be asynchronous?
        while (writeChannel == null) {
            delay(50)
        }
    }


}