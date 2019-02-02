package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.*
import com.dmdirc.ktirc.messages.*
import com.dmdirc.ktirc.model.*
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.logging.Level
import java.util.logging.LogManager


interface IrcClient {

    suspend fun send(message: String)

    val serverState: ServerState
    val channelState: ChannelStateMap
    val userState: UserState

    fun onEvent(handler: (IrcEvent) -> Unit)

    val caseMapping: CaseMapping
        get() = serverState.features[ServerFeature.ServerCaseMapping] ?: CaseMapping.Rfc

    fun isLocalUser(user: User): Boolean = caseMapping.areEquivalent(user.nickname, serverState.localNickname)

}

// TODO: How should alternative nicknames work?
// TODO: Should IRC Client take a pool of servers and rotate through, or make the caller do that?
// TODO: Should there be a default profile?
class IrcClientImpl(private val server: Server, private val profile: Profile) : IrcClient {

    var socketFactory: (String, Int, Boolean) -> LineBufferedSocket = ::KtorLineBufferedSocket

    override val serverState = ServerState(profile.initialNick)
    override val channelState = ChannelStateMap { caseMapping }
    override val userState = UserState { caseMapping }

    private val messageHandler = MessageHandler(messageProcessors.toList(), eventHandlers.toMutableList())

    private val parser = MessageParser()
    private var socket: LineBufferedSocket? = null

    // TODO: It would be cleaner if this didn't suspend but returned immediately
    override suspend fun send(message: String) {
        socket?.sendLine(message)
    }

    suspend fun connect() {
        // TODO: Concurrency!
        check(socket == null)
        coroutineScope {
            with(socketFactory(server.host, server.port, server.tls)) {
                socket = this
                connect()
                sendLine("CAP LS 302")
                server.password?.let { pass -> sendLine(passwordMessage(pass)) }
                sendLine(nickMessage(profile.initialNick))
                // TODO: Send correct host
                sendLine(userMessage(profile.userName, "localhost", server.host, profile.realName))
                // TODO: This should be elsewhere
                messageHandler.processMessages(this@IrcClientImpl, readLines(this@coroutineScope).map { parser.parse(it) })
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }

    override fun onEvent(handler: (IrcEvent) -> Unit) {
        messageHandler.handlers.add(object : EventHandler {
            override suspend fun processEvent(client: IrcClient, event: IrcEvent) {
                handler(event)
            }
        })
    }
}

fun main() {
    val rootLogger = LogManager.getLogManager().getLogger("")
    rootLogger.level = Level.FINEST
    for (h in rootLogger.handlers) {
        h.level = Level.FINEST
    }

    runBlocking {
        val client = IrcClientImpl(Server("testnet.inspircd.org", 6667), Profile("KtIrc", "Kotlin!", "kotlin"))
        client.onEvent { event ->
            runBlocking {
                when (event) {
                    is ServerWelcome -> client.send(joinMessage("#ktirc"))
                    is MessageReceived -> if (event.message == "!test") client.send(privmsgMessage(event.target, "Test successful!"))
                }
            }
        }
        client.connect()
    }
}