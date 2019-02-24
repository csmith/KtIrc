package com.dmdirc.ktirc

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.model.*
import com.dmdirc.ktirc.util.RemoveIn
import kotlinx.coroutines.Deferred

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

    /**
     * The configured behaviour of the client.
     */
    val behaviour: ClientBehaviour

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
    @Deprecated("Use structured send instead", ReplaceWith("send(command, arguments)"))
    @RemoveIn("2.0.0")
    fun send(message: String)

    /**
     * Sends the given command to the IRC server.
     *
     * This should only be needed to send raw/custom commands; standard messages can be sent using the
     * extension methods in [com.dmdirc.ktirc.messages] such as [sendJoin].
     *
     * This method will return immediately; the message will be delivered by a coroutine. Messages
     * are guaranteed to be delivered in order when this method is called multiple times.
     *
     * @param tags The IRCv3 tags to prefix the message with, if any.
     * @param command The command to be sent.
     * @param arguments The arguments to the command.
     */
    fun send(tags: Map<MessageTag, String>, command: String, vararg arguments: String)

    /**
     * Sends the given command to the IRC server.
     *
     * This should only be needed to send raw/custom commands; standard messages can be sent using the
     * extension methods in [com.dmdirc.ktirc.messages] such as [sendJoin].
     *
     * This method will return immediately; the message will be delivered by a coroutine. Messages
     * are guaranteed to be delivered in order when this method is called multiple times.
     *
     * @param command The command to be sent.
     * @param arguments The arguments to the command.
     */
    fun send(command: String, vararg arguments: String) = send(emptyMap(), command, *arguments)

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
     * Utility method to determine if the given user is the one we are connected to IRC as. Should only be used after a
     * [com.dmdirc.ktirc.events.ServerReady] event has been received.
     */
    fun isLocalUser(user: User) = isLocalUser(user.nickname)

    /**
     * Utility method to determine if the given user is the one we are connected to IRC as. Should only be used after a
     * [com.dmdirc.ktirc.events.ServerReady] event has been received.
     */
    fun isLocalUser(nickname: String) = caseMapping.areEquivalent(nickname, serverState.localNickname)

    /**
     * Determines if the given [target] appears to be a channel or not. Should only be used after a
     * [com.dmdirc.ktirc.events.ServerReady] event has been received.
     */
    fun isChannel(target: String) = target.isNotEmpty() && serverState.channelTypes.contains(target[0])

}

internal interface ExperimentalIrcClient : IrcClient {

    /**
     * Sends the given command to the IRC server, and waits for a response back.
     *
     * This should only be needed to send raw/custom commands; standard messages can be sent using the
     * extension methods in [com.dmdirc.ktirc.messages] such as TODO: sendJoinAsync.
     *
     * This method will return immediately. If the server supports the labeled-responses capability,
     * the returned [Deferred] will be eventually populated with the response from the server. If
     * the server does not support the capability, or the response times out, `null` will be supplied.
     *
     * @param command The command to be sent.
     * @param arguments The arguments to the command.
     * @return A deferred [IrcEvent]? that contains the server's response to the command.
     */
    fun sendAsync(command: String, vararg arguments: String) = sendAsync(emptyMap(),  command, *arguments)

    /**
     * Sends the given command to the IRC server, and waits for a response back.
     *
     * This should only be needed to send raw/custom commands; standard messages can be sent using the
     * extension methods in [com.dmdirc.ktirc.messages] such as TODO: sendJoinAsync.
     *
     * This method will return immediately. If the server supports the labeled-responses capability,
     * the returned [Deferred] will be eventually populated with the response from the server. If
     * the server does not support the capability, or the response times out, `null` will be supplied.
     *
     * @param tags The IRCv3 tags to prefix the message with, if any.
     * @param command The command to be sent.
     * @param arguments The arguments to the command.
     * @return A deferred [IrcEvent]? that contains the server's response to the command.
     */
    fun sendAsync(tags: Map<MessageTag, String>, command: String, vararg arguments: String): Deferred<IrcEvent?>

}

/**
 * Defines the behaviour of an [IrcClient].
 */
interface ClientBehaviour {

    /** Whether or not to request channel modes when we join a channel. */
    val requestModesOnJoin: Boolean

    /**
     * If enabled, all messages (`PRIVMSG`s) sent by the client will always be "echoed" back as a MessageReceived
     * event.
     *
     * This makes the behaviour consistent across ircds that support the echo-message capability and those that
     * don't. If disabled, messages will only be echoed back when the server supports the capability.
     */
    val alwaysEchoMessages: Boolean

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
