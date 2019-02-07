package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.model.*

/**
 * Primary interface for interacting with KtIrc.
 */
interface IrcClient {

    /**
     * Holds state relating to the current server, its features, and capabilities.
     */
    val serverState: ServerState

    /**
     * Holds the state for each channel we are currently joined to.
     */
    val channelState: ChannelStateMap

    /**
     * Holds the state for all known users (those in common channels).
     */
    val userState: UserState

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
