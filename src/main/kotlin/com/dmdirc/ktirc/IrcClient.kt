package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.EventHandler
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.events.eventHandlers
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

    val caseMapping: CaseMapping
        get() = serverState.features[ServerFeature.ServerCaseMapping] ?: CaseMapping.Rfc

    fun isLocalUser(user: User): Boolean = caseMapping.areEquivalent(user.nickname, serverState.localNickname)

}

// TODO: How should alternative nicknames work?
// TODO: Should IRC Client take a pool of servers and rotate through, or make the caller do that?
// TODO: Should there be a default profile?
class IrcClientImpl(private val server: Server, private val profile: Profile) : IrcClient {

    var socketFactory: (String, Int) -> LineBufferedSocket = ::KtorLineBufferedSocket

    override val serverState = ServerState(profile.initialNick)
    override val channelState = ChannelStateMap { caseMapping }

    private val messageHandler = MessageHandler(messageProcessors, eventHandlers + object : EventHandler {
        override suspend fun processEvent(client: IrcClient, event: IrcEvent) {
            when (event) {
                is ServerWelcome -> client.send(joinMessage("#mdbot"))
            }
        }
    })

    private val parser = MessageParser()
    private var socket: LineBufferedSocket? = null

    override suspend fun send(message: String) {
        socket?.sendLine(message)
    }

    suspend fun connect() {
        // TODO: Concurrency!
        check(socket == null)
        coroutineScope {
            with(socketFactory(server.host, server.port)) {
                socket = this
                connect()
                // TODO: CAP LS
                server.password?.let { pass -> sendLine(passwordMessage(pass)) }
                sendLine(nickMessage(profile.initialNick))
                // TODO: Send correct host
                sendLine(userMessage(profile.userName, "localhost", server.host, profile.realName))
                // TODO: This should be elsewhere
                messageHandler.processMessages(this@IrcClientImpl, readLines(this@coroutineScope).map { parser.parse(it) })
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val rootLogger = LogManager.getLogManager().getLogger("")
            rootLogger.level = Level.FINEST
            for (h in rootLogger.handlers) {
                h.level = Level.FINEST
            }

            runBlocking {
                val client = IrcClientImpl(Server("irc.quakenet.org", 6667), Profile("KtIrc", "Kotlin!", "kotlin"))
                client.connect()
            }
        }
    }

}