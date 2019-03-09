package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.events.handlers.eventHandlers
import com.dmdirc.ktirc.events.mutators.eventMutators
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.io.LineBufferedSocketImpl
import com.dmdirc.ktirc.io.MessageHandler
import com.dmdirc.ktirc.io.MessageParser
import com.dmdirc.ktirc.messages.*
import com.dmdirc.ktirc.messages.processors.messageProcessors
import com.dmdirc.ktirc.model.*
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.generateLabel
import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.map
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

/**
 * Concrete implementation of an [IrcClient].
 */
// TODO: How should alternative nicknames work?
// TODO: Should IRC Client take a pool of servers and rotate through, or make the caller do that?
internal class IrcClientImpl(private val config: IrcClientConfig) : ExperimentalIrcClient, CoroutineScope {

    private val log by logger()

    @ExperimentalCoroutinesApi
    override val coroutineContext = GlobalScope.newCoroutineContext(Dispatchers.IO)

    @ExperimentalCoroutinesApi
    internal var socketFactory: (CoroutineScope, String, Int, Boolean) -> LineBufferedSocket = ::LineBufferedSocketImpl

    internal var asyncTimeout = Duration.ofSeconds(20)

    override var behaviour = config.behaviour

    override val serverState = ServerState(config.profile.nickname, config.server.host, config.sasl)
    override val channelState = ChannelStateMap { caseMapping }
    override val userState = UserState { caseMapping }

    private val messageHandler = MessageHandler(messageProcessors, eventMutators, eventHandlers)
    private val messageBuilder = MessageBuilder()

    private val parser = MessageParser()
    private var socket: LineBufferedSocket? = null

    private val connecting = AtomicBoolean(false)

    override fun send(message: String) {
        socket?.sendChannel?.offer(message.toByteArray()) ?: log.warning { "No send channel for message: $message" }
    }

    override fun send(tags: Map<MessageTag, String>, command: String, vararg arguments: String) {
        maybeEchoMessage(command, arguments)
        socket?.sendChannel?.offer(messageBuilder.build(tags, command, arguments))
                ?: log.warning { "No send channel for command: $command" }
    }

    override fun sendAsync(tags: Map<MessageTag, String>, command: String, arguments: Array<String>, matcher: (IrcEvent) -> Boolean) = async {
        val label = generateLabel(this@IrcClientImpl)
        val channel = Channel<IrcEvent>(1)

        if (serverState.asyncResponseState.supportsLabeledResponses) {
            serverState.asyncResponseState.pendingResponses[label] = channel to { event -> event.metadata.label == label }
            send(tags + (MessageTag.Label to label), command, *arguments)
        } else {
            serverState.asyncResponseState.pendingResponses[label] = channel to matcher
            send(tags, command, *arguments)
        }

        withTimeoutOrNull(asyncTimeout.toMillis()) {
            channel.receive()
        }.also { serverState.asyncResponseState.pendingResponses.remove(label) }
    }

    override fun connect() {
        check(!connecting.getAndSet(true))

        @Suppress("EXPERIMENTAL_API_USAGE")
        with(socketFactory(this, config.server.host, config.server.port, config.server.useTls)) {
            socket = this

            emitEvent(ServerConnecting(EventMetadata(currentTimeProvider())))

            launch {
                try {
                    connect()
                    emitEvent(ServerConnected(EventMetadata(currentTimeProvider())))
                    sendCapabilityList()
                    sendPasswordIfPresent()
                    sendNickChange(config.profile.nickname)
                    sendUser(config.profile.username, config.profile.realName)
                    messageHandler.processMessages(this@IrcClientImpl, receiveChannel.map { parser.parse(it) })
                } catch (ex: Exception) {
                    log.log(Level.SEVERE, ex) { "Error connecting to ${config.server.host}:${config.server.port}" }
                    emitEvent(ServerConnectionError(EventMetadata(currentTimeProvider()), ex.toConnectionError(), ex.localizedMessage))
                }

                reset()
                emitEvent(ServerDisconnected(EventMetadata(currentTimeProvider())))
            }
        }
    }

    override fun disconnect() {
        socket?.disconnect()
    }

    override fun onEvent(handler: (IrcEvent) -> Unit) = messageHandler.addEmitter(handler)

    private fun emitEvent(event: IrcEvent) = messageHandler.handleEvent(this, event)
    private fun sendPasswordIfPresent() = config.server.password?.let(this::sendPassword)

    private fun maybeEchoMessage(command: String, arguments: Array<out String>) {
        // TODO: Is this the best place to do it? It'd be nicer to actually build the message and
        //       reflect the raw line back through all the processors etc.
        if (command == "PRIVMSG" && behaviour.alwaysEchoMessages && !serverState.capabilities.enabledCapabilities.contains(Capability.EchoMessages)) {
            emitEvent(MessageReceived(
                    EventMetadata(currentTimeProvider()),
                    userState[serverState.localNickname]?.details ?: User(serverState.localNickname),
                    arguments[0],
                    arguments[1]
            ))
        }
    }

    internal fun reset() {
        serverState.reset()
        channelState.clear()
        userState.reset()
        socket = null
        connecting.set(false)
    }

}
