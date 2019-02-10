package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.ConnectionError
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.User
import java.time.LocalDateTime

/** Base class for all events. */
sealed class IrcEvent(val time: LocalDateTime)

/** Raised when a connection to the server is being established. */
class ServerConnecting(time: LocalDateTime) : IrcEvent(time)

/** Raised when the connection to the server has been established. The server will not be ready for use yet. */
class ServerConnected(time: LocalDateTime) : IrcEvent(time)

/** Raised when the connection to the server has ended. */
class ServerDisconnected(time: LocalDateTime) : IrcEvent(time)

/** Raised when an error occurred trying to connect. */
class ServerConnectionError(time: LocalDateTime, val error: ConnectionError, val details: String?) : IrcEvent(time)

/** Raised when the server is ready for use. */
class ServerReady(time: LocalDateTime) : IrcEvent(time)

/** Raised when the server initially welcomes us to the IRC network. */
class ServerWelcome(time: LocalDateTime, val server: String, val localNick: String) : IrcEvent(time)

/** Raised when the features supported by the server have changed. This may occur numerous times. */
class ServerFeaturesUpdated(time: LocalDateTime, val serverFeatures: ServerFeatureMap) : IrcEvent(time)

/** Raised whenever a PING is received from the server. */
class PingReceived(time: LocalDateTime, val nonce: ByteArray) : IrcEvent(time)

/** Raised when a user joins a channel. */
class ChannelJoined(time: LocalDateTime, val user: User, val channel: String) : IrcEvent(time)

/** Raised when a user leaves a channel. */
class ChannelParted(time: LocalDateTime, val user: User, val channel: String, val reason: String = "") : IrcEvent(time)

/** Raised when a [victim] is kicked from a channel. */
class ChannelUserKicked(time: LocalDateTime, val user: User, val channel: String, val victim: String, val reason: String = "") : IrcEvent(time)

/** Raised when a user quits, and is in a channel. */
class ChannelQuit(time: LocalDateTime, val user: User, val channel: String, val reason: String = "") : IrcEvent(time)

/** Raised when a user changes nickname, and is in a channel. */
class ChannelNickChanged(time: LocalDateTime, val user: User, val channel: String, val newNick: String) : IrcEvent(time)

/** Raised when a batch of the channel's member list has been received. More batches may follow. */
class ChannelNamesReceived(time: LocalDateTime, val channel: String, val names: List<String>) : IrcEvent(time)

/** Raised when the entirety of the channel's member list has been received. */
class ChannelNamesFinished(time: LocalDateTime, val channel: String) : IrcEvent(time)

/** Raised when a channel topic is discovered (not changed). Usually followed by [ChannelTopicMetadataDiscovered]. */
class ChannelTopicDiscovered(time: LocalDateTime, val channel: String, val topic: String) : IrcEvent(time)

/** Raised when a channel topic's metadata is discovered. */
class ChannelTopicMetadataDiscovered(time: LocalDateTime, val channel: String, val user: User, val setTime: LocalDateTime) : IrcEvent(time)

/** Raised when a message is received. */
class MessageReceived(time: LocalDateTime, val user: User, val target: String, val message: String, val messageId: String? = null) : IrcEvent(time)

/**
 * Raised when a notice is received.
 *
 * The [user] may in fact be a server, or have a nickname of `*` while connecting.
 */
class NoticeReceived(time: LocalDateTime, val user: User, val target: String, val message: String) : IrcEvent(time)

/** Raised when an action is received. */
class ActionReceived(time: LocalDateTime, val user: User, val target: String, val action: String, val messageId: String? = null) : IrcEvent(time)

/** Raised when a CTCP is received. */
class CtcpReceived(time: LocalDateTime, val user: User, val target: String, val type: String, val content: String) : IrcEvent(time)

/** Raised when a CTCP reply is received. */
class CtcpReplyReceived(time: LocalDateTime, val user: User, val target: String, val type: String, val content: String) : IrcEvent(time)

/** Raised when a user quits. */
class UserQuit(time: LocalDateTime, val user: User, val reason: String = "") : IrcEvent(time)

/** Raised when a user changes nickname. */
class UserNickChanged(time: LocalDateTime, val user: User, val newNick: String) : IrcEvent(time)

/**
 * Raised when a user's account changes (i.e., they auth'd or deauth'd with services).
 *
 * This event is only raised if the server supports the `account-notify` capability.
 */
class UserAccountChanged(time: LocalDateTime, val user: User, val newAccount: String?) : IrcEvent(time)

/** Raised when available server capabilities are received. More batches may follow. */
class ServerCapabilitiesReceived(time: LocalDateTime, val capabilities: Map<Capability, String>) : IrcEvent(time)

/** Raised when our requested capabilities are acknowledged. More batches may follow. */
class ServerCapabilitiesAcknowledged(time: LocalDateTime, val capabilities: Map<Capability, String>) : IrcEvent(time)

/** Raised when the server has finished sending us capabilities. */
class ServerCapabilitiesFinished(time: LocalDateTime) : IrcEvent(time)

/** Raised when a line of the Message Of the Day has been received. */
class MotdLineReceived(time: LocalDateTime, val line: String, val first: Boolean = false) : IrcEvent(time)

/** Raised when a Message Of the Day has completed. */
class MotdFinished(time: LocalDateTime, val missing: Boolean = false) : IrcEvent(time)

/**
 * Raised when a mode change occurs.
 *
 * If [discovered] is true then the event is in response to the server providing the full set of modes on the target,
 * and the given modes are thus exhaustive. Otherwise, the modes are a sequence of changes to apply to the existing
 * state.
 */
class ModeChanged(time: LocalDateTime, val target: String, val modes: String, val arguments: Array<String>, val discovered: Boolean = false) : IrcEvent(time)

/** Raised when an AUTHENTICATION message is received. [argument] is `null` if the server sent an empty reply ("+") */
class AuthenticationMessage(time: LocalDateTime, val argument: String?) : IrcEvent(time)

/** Raised when a SASL attempt finishes, successfully or otherwise. */
class SaslFinished(time: LocalDateTime, var success: Boolean) : IrcEvent(time)

/** Raised when the server says our SASL mechanism isn't available, but gives us a list of others. */
class SaslMechanismNotAvailableError(time: LocalDateTime, var mechanisms: Collection<String>) : IrcEvent(time)
