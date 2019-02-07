package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerConnecting
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.model.ChannelState
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.User
import com.dmdirc.ktirc.util.currentTimeProvider
import com.nhaarman.mockitokotlin2.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.map
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
internal class IrcClientImplTest {

    companion object {
        private const val HOST = "thegibson.com"
        private const val PORT = 12345
        private const val NICK = "AcidBurn"
        private const val REAL_NAME = "Kate Libby"
        private const val USER_NAME = "acidb"
        private const val PASSWORD = "HackThePlanet"
    }

    private val readLineChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val sendLineChannel = Channel<ByteArray>(Channel.UNLIMITED)

    private val mockSocket = mock<LineBufferedSocket> {
        on { receiveChannel } doReturn readLineChannel
        on { sendChannel } doReturn sendLineChannel
    }

    private val mockSocketFactory = mock<(CoroutineScope, String, Int, Boolean) -> LineBufferedSocket> {
        on { invoke(any(), eq(HOST), eq(PORT), any()) } doReturn mockSocket
    }

    private val mockEventHandler = mock<(IrcEvent) -> Unit>()

    private val profileConfig = ProfileConfig().apply {
        nickname = NICK
        realName = REAL_NAME
        username = USER_NAME
    }

    private val normalConfig = IrcClientConfig(ServerConfig().apply {
        host = HOST
        port = PORT
    }, profileConfig, null)

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `uses socket factory to create a new socket on connect`() {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        verify(mockSocketFactory, timeout(500)).invoke(client, HOST, PORT, false)
    }

    @Test
    fun `uses socket factory to create a new tls on connect`() {
        val client = IrcClientImpl(IrcClientConfig(ServerConfig().apply {
            host = HOST
            port = PORT
            useTls = true
        }, profileConfig, null))
        client.socketFactory = mockSocketFactory
        client.connect()

        verify(mockSocketFactory, timeout(500)).invoke(client, HOST, PORT, true)
    }

    @Test
    fun `throws if socket already exists`() {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        assertThrows<IllegalStateException> {
            client.connect()
        }
    }

    @Test
    fun `emits connection events with local time`() = runBlocking {
        currentTimeProvider = { TestConstants.time }
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.onEvent(mockEventHandler)
        client.connect()

        val captor = argumentCaptor<IrcEvent>()
        verify(mockEventHandler, timeout(500).atLeast(2)).invoke(captor.capture())

        assertTrue(captor.firstValue is ServerConnecting)
        assertEquals(TestConstants.time, captor.firstValue.time)

        assertTrue(captor.secondValue is ServerConnected)
        assertEquals(TestConstants.time, captor.secondValue.time)
    }

    @Test
    fun `sends basic connection strings`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        assertEquals("CAP LS 302", String(sendLineChannel.receive()))
        assertEquals("NICK :$NICK", String(sendLineChannel.receive()))
        assertEquals("USER $USER_NAME 0 * :$REAL_NAME", String(sendLineChannel.receive()))
    }

    @Test
    fun `sends password first, when present`() = runBlocking {
        val client = IrcClientImpl(IrcClientConfig(ServerConfig().apply {
            host = HOST
            port = PORT
            password = PASSWORD
        }, profileConfig, null))
        client.socketFactory = mockSocketFactory
        client.connect()

        assertEquals("CAP LS 302", String(sendLineChannel.receive()))
        assertEquals("PASS :$PASSWORD", String(sendLineChannel.receive()))
    }

    @Test
    fun `sends events to provided event handler`() {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.onEvent(mockEventHandler)

        GlobalScope.launch {
            readLineChannel.send(":the.gibson 001 acidBurn :Welcome to the IRC!".toByteArray())
        }

        client.connect()

        verify(mockEventHandler, timeout(500)).invoke(isA<ServerWelcome>())
    }

    @Test
    fun `gets case mapping from server features`() {
        val client = IrcClientImpl(normalConfig)
        client.serverState.features[ServerFeature.ServerCaseMapping] = CaseMapping.RfcStrict
        assertEquals(CaseMapping.RfcStrict, client.caseMapping)
    }

    @Test
    fun `indicates if user is local user or not`() {
        val client = IrcClientImpl(normalConfig)
        client.serverState.localNickname = "[acidBurn]"

        assertTrue(client.isLocalUser(User("{acidBurn}", "libby", "root.localhost")))
        assertFalse(client.isLocalUser(User("acid-Burn", "libby", "root.localhost")))
    }

    @Test
    fun `indicates if nickname is local user or not`() {
        val client = IrcClientImpl(normalConfig)
        client.serverState.localNickname = "[acidBurn]"

        assertTrue(client.isLocalUser("{acidBurn}"))
        assertFalse(client.isLocalUser("acid-Burn"))
    }

    @Test
    fun `uses current case mapping to check local user`() {
        val client = IrcClientImpl(normalConfig)
        client.serverState.localNickname = "[acidBurn]"
        client.serverState.features[ServerFeature.ServerCaseMapping] = CaseMapping.Ascii
        assertFalse(client.isLocalUser(User("{acidBurn}", "libby", "root.localhost")))
    }

    @Test
    fun `sends text to socket`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.send("testing 123")

        assertEquals(true, withTimeoutOrNull(500) {
            var found = false
            for (line in sendLineChannel) {
                if (String(line) == "testing 123") {
                    found = true
                    break
                }
            }
            found
        })
    }

    @Test
    fun `disconnects the socket`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.disconnect()

        verify(mockSocket, timeout(500)).disconnect()
    }

    @Test
    fun `sends messages in order`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        (0..100).forEach { client.send("TEST $it") }

        assertEquals(100, withTimeoutOrNull(500) {
            var next = 0
            for (line in sendLineChannel.map { String(it) }.filter { it.startsWith("TEST ") }) {
                assertEquals("TEST $next", line)
                if (++next == 100) {
                    break
                }
            }
            next
        })
    }

    @Test
    fun `defaults local nickname to profile`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        assertEquals(NICK, client.serverState.localNickname)
    }

    @Test
    fun `defaults server name to host name`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        assertEquals(HOST, client.serverState.serverName)
    }

    @Test
    fun `reset clears all state`() {
        with(IrcClientImpl(normalConfig)) {
            userState += User("acidBurn")
            channelState += ChannelState("#thegibson") { CaseMapping.Rfc }
            serverState.serverName = "root.$HOST"
            reset()

            assertEquals(0, userState.count())
            assertEquals(0, channelState.count())
            assertEquals(HOST, serverState.serverName)
        }
    }


}