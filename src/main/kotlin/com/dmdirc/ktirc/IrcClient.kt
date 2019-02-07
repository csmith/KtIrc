package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.*
import com.dmdirc.ktirc.messages.*
import com.dmdirc.ktirc.model.*
import com.dmdirc.ktirc.sasl.PlainMechanism
import com.dmdirc.ktirc.sasl.SaslMechanism
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
    val hasSaslConfig: Boolean

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
 * Constructs a new [IrcClient] using a configuration DSL.
 *
 * See [IrcClientConfigBuilder] for details of all options
 */
@IrcClientDsl
@Suppress("FunctionName")
fun IrcClient(block: IrcClientConfigBuilder.() -> Unit): IrcClient =
        IrcClientImpl(IrcClientConfigBuilder().apply(block).build())

/**
 * Concrete implementation of an [IrcClient].
 */
// TODO: How should alternative nicknames work?
// TODO: Should IRC Client take a pool of servers and rotate through, or make the caller do that?
// TODO: Should there be a default profile?
internal class IrcClientImpl(private val config: IrcClientConfig) : IrcClient, CoroutineScope {

    private val log by logger()

    @ExperimentalCoroutinesApi
    override val coroutineContext = GlobalScope.newCoroutineContext(Dispatchers.IO)

    @ExperimentalCoroutinesApi
    @KtorExperimentalAPI
    internal var socketFactory: (CoroutineScope, String, Int, Boolean) -> LineBufferedSocket = ::KtorLineBufferedSocket

    override val serverState = ServerState(config.profile.nickname, config.server.host, getSaslMechanisms())
    override val channelState = ChannelStateMap { caseMapping }
    override val userState = UserState { caseMapping }
    override val hasSaslConfig = config.sasl != null

    private val messageHandler = MessageHandler(messageProcessors.toList(), eventHandlers.toMutableList())

    private val parser = MessageParser()
    private var socket: LineBufferedSocket? = null

    private val connecting = AtomicBoolean(false)

    override fun send(message: String) {
        socket?.sendChannel?.offer(message.toByteArray()) ?: log.warning { "No send channel for message: $message" }
    }

    override fun connect() {
        check(!connecting.getAndSet(true))

        @Suppress("EXPERIMENTAL_API_USAGE")
        with(socketFactory(this, config.server.host, config.server.port, config.server.useTls)) {
            // TODO: Proper error handling - what if connect() fails?
            socket = this

            emitEvent(ServerConnecting(currentTimeProvider()))

            launch {
                connect()
                emitEvent(ServerConnected(currentTimeProvider()))
                sendCapabilityList()
                sendPasswordIfPresent()
                sendNickChange(config.profile.nickname)
                sendUser(config.profile.username, config.profile.realName)
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
    private fun sendPasswordIfPresent() = config.server.password?.let(this::sendPassword)

    internal fun reset() {
        serverState.reset()
        channelState.clear()
        userState.reset()
        socket = null
        connecting.set(false)
    }

    private fun getSaslMechanisms(): Collection<SaslMechanism> {
        // TODO: Move this somewhere else
        // TODO: Allow mechanisms to be configured
        config.sasl?.let {
            return listOf(PlainMechanism(it))
        } ?: return emptyList()
    }

}
