package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.messages.tagMap
import com.dmdirc.ktirc.model.*
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.generateLabel
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.sync.Mutex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.channels.UnresolvedAddressException
import java.security.cert.CertificateException
import java.util.concurrent.atomic.AtomicReference

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

    private val mockSocket = mockk<LineBufferedSocket> {
        every { receiveChannel } returns readLineChannel
        every { sendChannel } returns sendLineChannel
    }

    private val mockSocketFactory = mockk<(CoroutineScope, String, Int, Boolean) -> LineBufferedSocket> {
        every { this@mockk.invoke(any(), eq(HOST), eq(PORT), any()) } returns mockSocket
    }

    private val mockEventHandler = mockk<(IrcEvent) -> Unit> {
        every { this@mockk.invoke(any()) } just Runs
    }

    private val profileConfig = ProfileConfig().apply {
        nickname = NICK
        realName = REAL_NAME
        username = USER_NAME
    }

    private val serverConfig = ServerConfig().apply {
        host = HOST
        port = PORT
    }

    private val normalConfig = IrcClientConfig(serverConfig, profileConfig, BehaviourConfig(), null)

    @BeforeEach
    fun setUp() {
        currentTimeProvider = { TestConstants.time }
    }

    @Test
    fun `uses socket factory to create a new socket on connect`() {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        verify(timeout = 500) { mockSocketFactory(client, HOST, PORT, false) }
    }

    @Test
    fun `uses socket factory to create a new tls on connect`() {
        val client = IrcClientImpl(IrcClientConfig(ServerConfig().apply {
            host = HOST
            port = PORT
            useTls = true
        }, profileConfig, BehaviourConfig(), null))
        client.socketFactory = mockSocketFactory
        client.connect()

        verify(timeout = 500) { mockSocketFactory(client, HOST, PORT, true) }
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

        val connectingSlot = slot<ServerConnecting>()
        val connectedSlot = slot<ServerConnected>()

        every { mockEventHandler.invoke(capture(connectingSlot)) } just Runs
        every { mockEventHandler.invoke(capture(connectedSlot)) } just Runs

        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.onEvent(mockEventHandler)
        client.connect()

        verify(timeout = 500) {
            mockEventHandler(ofType<ServerConnecting>())
            mockEventHandler(ofType<ServerConnected>())
        }

        assertEquals(TestConstants.time, connectingSlot.captured.metadata.time)
        assertEquals(TestConstants.time, connectedSlot.captured.metadata.time)
    }

    @Test
    fun `sends basic connection strings`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        assertEquals("CAP LS 302", String(sendLineChannel.receive()))
        assertEquals("NICK $NICK", String(sendLineChannel.receive()))
        assertEquals("USER $USER_NAME 0 * :$REAL_NAME", String(sendLineChannel.receive()))
    }

    @Test
    fun `sends password first, when present`() = runBlocking {
        val client = IrcClientImpl(IrcClientConfig(ServerConfig().apply {
            host = HOST
            port = PORT
            password = PASSWORD
        }, profileConfig, BehaviourConfig(), null))
        client.socketFactory = mockSocketFactory
        client.connect()

        assertEquals("CAP LS 302", String(sendLineChannel.receive()))
        assertEquals("PASS $PASSWORD", String(sendLineChannel.receive()))
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

        verify(timeout = 500) {
            mockEventHandler(ofType<ServerWelcome>())
        }
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
    @SuppressWarnings("deprecation")
    fun `sends text to socket`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.send("testing 123")

        assertLineReceived("testing 123")
    }

    @Test
    fun `sends structured text to socket`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.send("testing", "123", "456")

        assertLineReceived("testing 123 456")
    }

    @Test
    fun `echoes message event when behaviour is set and cap is unsupported`() = runBlocking {
        val config = IrcClientConfig(serverConfig, profileConfig, BehaviourConfig().apply { alwaysEchoMessages = true }, null)
        val client = IrcClientImpl(config)
        client.socketFactory = mockSocketFactory

        val slot = slot<MessageReceived>()
        val mockkEventHandler = mockk<(IrcEvent) -> Unit>(relaxed = true)
        every { mockkEventHandler(capture(slot)) } just Runs

        client.onEvent(mockkEventHandler)
        client.connect()

        client.send("PRIVMSG", "#thegibson", "Mess with the best, die like the rest")

        assertTrue(slot.isCaptured)
        val event = slot.captured
        assertEquals("#thegibson", event.target)
        assertEquals("Mess with the best, die like the rest", event.message)
        assertEquals(NICK, event.user.nickname)
        assertEquals(TestConstants.time, event.metadata.time)
    }

    @Test
    fun `does not echo message event when behaviour is set and cap is supported`() = runBlocking {
        val config = IrcClientConfig(serverConfig, profileConfig, BehaviourConfig().apply { alwaysEchoMessages = true }, null)
        val client = IrcClientImpl(config)
        client.socketFactory = mockSocketFactory
        client.serverState.capabilities.enabledCapabilities[Capability.EchoMessages] = ""
        client.connect()

        client.onEvent(mockEventHandler)
        client.send("PRIVMSG", "#thegibson", "Mess with the best, die like the rest")

        verify(inverse = true) {
            mockEventHandler(ofType<MessageReceived>())
        }
    }

    @Test
    fun `does not echo message event when behaviour is unset`() = runBlocking {
        val config = IrcClientConfig(serverConfig, profileConfig, BehaviourConfig().apply { alwaysEchoMessages = false }, null)
        val client = IrcClientImpl(config)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.onEvent(mockEventHandler)
        client.send("PRIVMSG", "#thegibson", "Mess with the best, die like the rest")

        verify(inverse = true) {
            mockEventHandler(ofType<MessageReceived>())
        }    }

    @Test
    fun `sends structured text to socket with tags`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.send(tagMap(MessageTag.AccountName to "acidB"), "testing", "123", "456")

        assertLineReceived("@account=acidB testing 123 456")
    }

    @Test
    fun `sends text to socket without label if cap is missing`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.sendWithLabel(tagMap(), "testing", "123")

        assertLineReceived("testing 123")
    }

    @Test
    fun `sends text to socket with added tags and label`() = runBlocking {
        generateLabel = { "abc123" }
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.serverState.capabilities.enabledCapabilities[Capability.LabeledResponse] = ""
        client.connect()

        client.sendWithLabel(tagMap(), "testing", "123")

        assertLineReceived("@draft/label=abc123 testing 123")
    }

    @Test
    fun `sends tagged text to socket with label`() = runBlocking {
        generateLabel = { "abc123" }
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.serverState.capabilities.enabledCapabilities[Capability.LabeledResponse] = ""
        client.connect()

        client.sendWithLabel(tagMap(MessageTag.AccountName to "x"), "testing", "123")

        assertLineReceived("@account=x;draft/label=abc123 testing 123")
    }

    @Test
    fun `disconnects the socket`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        client.disconnect()

        verify(timeout = 500) {
            mockSocket.disconnect()
        }
    }

    @Test
    @ObsoleteCoroutinesApi
    fun `sends messages in order`() = runBlocking {
        val client = IrcClientImpl(normalConfig)
        client.socketFactory = mockSocketFactory
        client.connect()

        (0..100).forEach { client.send("TEST", "$it") }

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
    fun `defaults local nickname to profile`() {
        val client = IrcClientImpl(normalConfig)
        assertEquals(NICK, client.serverState.localNickname)
    }

    @Test
    fun `defaults server name to host name`() {
        val client = IrcClientImpl(normalConfig)
        assertEquals(HOST, client.serverState.serverName)
    }

    @Test
    fun `exposes behaviour config`() {
        val client = IrcClientImpl(IrcClientConfig(
                ServerConfig().apply { host = HOST },
                profileConfig,
                BehaviourConfig().apply { requestModesOnJoin = true },
                null))

        assertTrue(client.behaviour.requestModesOnJoin)
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

    @Test
    fun `sends connect error when host is unresolvable`() = runBlocking {
        every { mockSocket.connect() } throws UnresolvedAddressException()
        with(IrcClientImpl(normalConfig)) {
            socketFactory = mockSocketFactory
            withTimeout(500) {
                launch {
                    delay(50)
                    connect()
                }
                val event = waitForEvent<ServerConnectionError>()
                assertEquals(ConnectionError.UnresolvableAddress, event.error)
            }
        }
    }

    @Test
    fun `sends connect error when tls certificate is bad`() = runBlocking {
        every { mockSocket.connect() } throws CertificateException("Boooo")
        with(IrcClientImpl(normalConfig)) {
            socketFactory = mockSocketFactory
            withTimeout(500) {
                launch {
                    delay(50)
                    connect()
                }
                val event = waitForEvent<ServerConnectionError>()
                assertEquals(ConnectionError.BadTlsCertificate, event.error)
                assertEquals("Boooo", event.details)
            }
        }
    }

    @Test
    fun `identifies channels that have a prefix in the chantypes feature`() {
        with(IrcClientImpl(normalConfig)) {
            serverState.features[ServerFeature.ChannelTypes] = "&~"
            assertTrue(isChannel("&dumpsterdiving"))
            assertTrue(isChannel("~hacktheplanet"))
            assertFalse(isChannel("#root"))
            assertFalse(isChannel("acidBurn"))
            assertFalse(isChannel(""))
            assertFalse(isChannel("acidBurn#~"))
        }
    }

    private suspend inline fun <reified T : IrcEvent> IrcClient.waitForEvent(): T {
        val mutex = Mutex(true)
        val value = AtomicReference<T>()
        onEvent {
            if (it is T) {
                value.set(it)
                mutex.unlock()
            }
        }
        mutex.lock()
        return value.get()
    }

    private suspend fun assertLineReceived(expected: String) {
        assertEquals(true, withTimeoutOrNull(500) {
            for (line in sendLineChannel.map { String(it) }) {
                println(line)
                if (line == expected) {
                    return@withTimeoutOrNull true
                }
            }
            false
        }) { "Expected to receive $expected" }
    }


}