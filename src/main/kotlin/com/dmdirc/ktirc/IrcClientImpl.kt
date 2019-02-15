package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.events.handlers.eventHandlers
import com.dmdirc.ktirc.events.mutators.eventMutators
import com.dmdirc.ktirc.io.KtorLineBufferedSocket
import com.dmdirc.ktirc.io.LineBufferedSocket
import com.dmdirc.ktirc.io.MessageHandler
import com.dmdirc.ktirc.io.MessageParser
import com.dmdirc.ktirc.messages.*
import com.dmdirc.ktirc.model.ChannelStateMap
import com.dmdirc.ktirc.model.ServerState
import com.dmdirc.ktirc.model.UserState
import com.dmdirc.ktirc.model.toConnectionError
import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.logger
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.map
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concrete implementation of an [IrcClient].
 */
// TODO: How should alternative nicknames work?
// TODO: Should IRC Client take a pool of servers and rotate through, or make the caller do that?
internal class IrcClientImpl(private val config: IrcClientConfig) : IrcClient, CoroutineScope {

    private val log by logger()

    @ExperimentalCoroutinesApi
    override val coroutineContext = GlobalScope.newCoroutineContext(Dispatchers.IO)

    @ExperimentalCoroutinesApi
    @KtorExperimentalAPI
    internal var socketFactory: (CoroutineScope, String, Int, Boolean) -> LineBufferedSocket = ::KtorLineBufferedSocket

    override var behaviour = config.behaviour

    override val serverState = ServerState(config.profile.nickname, config.server.host, config.sasl)
    override val channelState = ChannelStateMap { caseMapping }
    override val userState = UserState { caseMapping }

    private val messageHandler = MessageHandler(messageProcessors, eventMutators, eventHandlers)

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
                } catch (ex : Exception) {
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

    internal fun reset() {
        serverState.reset()
        channelState.clear()
        userState.reset()
        socket = null
        connecting.set(false)
    }

}
