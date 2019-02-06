package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.*
import com.dmdirc.ktirc.messages.*
import com.dmdirc.ktirc.model.*
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.logger
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.map
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Primary interface for interacting with KtIrc.
 */
interface IrcClient {

    val serverState: ServerState
    val channelState: ChannelStateMap
    val userState: UserState
    val profile: Profile

    val caseMapping: CaseMapping
        get() = serverState.features[ServerFeature.ServerCaseMapping] ?: CaseMapping.Rfc

    /**
     * Begins a connection attempt to the IRC server.
     *
     * This method will return immediately, and the attempt to connect will be executed in a coroutine on the
     * IO scheduler. To check the status of the connection, monitor events using [onEvent].
     */
    fun connect()

    /**
     * Disconnect immediately from the IRC server, without sending a QUIT.
     */
    fun disconnect()

    /**
     * Sends the given raw line to the IRC server, followed by a carriage return and line feed.
     *
     * Standard IRC messages can be constructed using the methods in [com.dmdirc.ktirc.messages]
     * such as [sendJoin].
     *
     * @param message The line to be sent to the IRC server.
     */
    fun send(message: String)

    /**
     * Registers a new handler for all events on this connection.
     *
     * All events are subclasses of [IrcEvent]; the idiomatic way to handle them is using a `when` statement:
     *
     * ```
     * client.onEvent {
     *     when(it) {
     *         is MessageReceived -> println(it.message)
     *     }
     * }
     * ```
     *
     * *Note*: at present handlers cannot be removed; they last the lifetime of the [IrcClient].
     *
     * @param handler The method to call when a new event occurs.
     */
    fun onEvent(handler: (IrcEvent) -> Unit)

    /**
     * Utility method to determine if the given user is the one we are connected to IRC as.
     */
    fun isLocalUser(user: User) = isLocalUser(user.nickname)

    /**
     * Utility method to determine if the given user is the one we are connected to IRC as.
     */
    fun isLocalUser(nickname: String) = caseMapping.areEquivalent(nickname, serverState.localNickname)

}

/**
 * Concrete implementation of an [IrcClient].
 *
 * @param server The server to connect to.
 * @param profile The user details to use when connecting.
 */
// TODO: How should alternative nicknames work?
// TODO: Should IRC Client take a pool of servers and rotate through, or make the caller do that?
// TODO: Should there be a default profile?
class IrcClientImpl(private val server: Server, override val profile: Profile) : IrcClient, CoroutineScope {

    private val log by logger()

    @ExperimentalCoroutinesApi
    override val coroutineContext = GlobalScope.newCoroutineContext(Dispatchers.IO)

    @ExperimentalCoroutinesApi
    @KtorExperimentalAPI
    internal var socketFactory: (CoroutineScope, String, Int, Boolean) -> LineBufferedSocket = ::KtorLineBufferedSocket

    override val serverState = ServerState(profile.initialNick, server.host)
    override val channelState = ChannelStateMap { caseMapping }
    override val userState = UserState { caseMapping }

    private val messageHandler = MessageHandler(messageProcessors.toList(), eventHandlers.toMutableList())

    private val parser = MessageParser()
    private var socket: LineBufferedSocket? = null

    private val connecting = AtomicBoolean(false)

    override fun send(message: String) {
        socket?.sendChannel?.offer(message.toByteArray()) ?: log.warning { "No send channel for message: $message"}
    }

    override fun connect() {
        check(!connecting.getAndSet(true))

        @Suppress("EXPERIMENTAL_API_USAGE")
        with(socketFactory(this, server.host, server.port, server.tls)) {
            // TODO: Proper error handling - what if connect() fails?
            socket = this

            emitEvent(ServerConnecting(currentTimeProvider()))

            launch {
                connect()
                emitEvent(ServerConnected(currentTimeProvider()))
                sendCapabilityList()
                sendPasswordIfPresent()
                sendNickChange(profile.initialNick)
                // TODO: Send correct host
                sendUser(profile.userName, profile.realName)
                messageHandler.processMessages(this@IrcClientImpl, receiveChannel.map { parser.parse(it) })
                reset()
                emitEvent(ServerDisconnected(currentTimeProvider()))
            }
        }
    }

    override fun disconnect() {
        socket?.disconnect()
    }

    override fun onEvent(handler: (IrcEvent) -> Unit) {
        messageHandler.handlers.add(object : EventHandler {
            override fun processEvent(client: IrcClient, event: IrcEvent): List<IrcEvent> {
                handler(event)
                return emptyList()
            }
        })
    }

    private fun emitEvent(event: IrcEvent) = messageHandler.emitEvent(this, event)
    private fun sendPasswordIfPresent() = server.password?.let(this::sendPassword)

    internal fun reset() {
        serverState.reset()
        channelState.clear()
        userState.reset()
        socket = null
        connecting.set(false)
    }

}
