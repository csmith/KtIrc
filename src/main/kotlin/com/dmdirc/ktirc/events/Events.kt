package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.ConnectionError
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.User
import java.time.LocalDateTime

/**
 * Metadata associated with an event.
 *
 * @param time The best-guess time at which the event occurred.
 * @param batchId The ID of the batch this event is part of, if any.
 */
data class EventMetadata(val time: LocalDateTime, val batchId: String? = null)

/** Base class for all events. */
sealed class IrcEvent(val metadata: EventMetadata) {

    /** The time at which the event occurred. */
    @Deprecated("Only for backwards compatibility; to be removed post-1.0.0", replaceWith = ReplaceWith("metadata.time"))
    val time: LocalDateTime
        get() = metadata.time

}

/** Raised when a connection to the server is being established. */
class ServerConnecting(metadata: EventMetadata) : IrcEvent(metadata)

/** Raised when the connection to the server has been established. The server will not be ready for use yet. */
class ServerConnected(metadata: EventMetadata) : IrcEvent(metadata)

/** Raised when the connection to the server has ended. */
class ServerDisconnected(metadata: EventMetadata) : IrcEvent(metadata)

/** Raised when an error occurred trying to connect. */
class ServerConnectionError(metadata: EventMetadata, val error: ConnectionError, val details: String?) : IrcEvent(metadata)

/** Raised when the server is ready for use. */
class ServerReady(metadata: EventMetadata) : IrcEvent(metadata)

/** Raised when the server initially welcomes us to the IRC network. */
class ServerWelcome(metadata: EventMetadata, val server: String, val localNick: String) : IrcEvent(metadata)

/** Raised when the features supported by the server have changed. This may occur numerous times. */
class ServerFeaturesUpdated(metadata: EventMetadata, val serverFeatures: ServerFeatureMap) : IrcEvent(metadata)

/** Raised whenever a PING is received from the server. */
class PingReceived(metadata: EventMetadata, val nonce: ByteArray) : IrcEvent(metadata)

/** Raised when a user joins a channel. */
class ChannelJoined(metadata: EventMetadata, val user: User, val channel: String) : IrcEvent(metadata)

/** Raised when a user leaves a channel. */
class ChannelParted(metadata: EventMetadata, val user: User, val channel: String, val reason: String = "") : IrcEvent(metadata)

/** Raised when a [victim] is kicked from a channel. */
class ChannelUserKicked(metadata: EventMetadata, val user: User, val channel: String, val victim: String, val reason: String = "") : IrcEvent(metadata)

/** Raised when a user quits, and is in a channel. */
class ChannelQuit(metadata: EventMetadata, val user: User, val channel: String, val reason: String = "") : IrcEvent(metadata)

/** Raised when a user changes nickname, and is in a channel. */
class ChannelNickChanged(metadata: EventMetadata, val user: User, val channel: String, val newNick: String) : IrcEvent(metadata)

/** Raised when a batch of the channel's member list has been received. More batches may follow. */
class ChannelNamesReceived(metadata: EventMetadata, val channel: String, val names: List<String>) : IrcEvent(metadata)

/** Raised when the entirety of the channel's member list has been received. */
class ChannelNamesFinished(metadata: EventMetadata, val channel: String) : IrcEvent(metadata)

/** Raised when a channel topic is discovered (not changed). Usually followed by [ChannelTopicMetadataDiscovered] if the [topic] is non-null. */
class ChannelTopicDiscovered(metadata: EventMetadata, val channel: String, val topic: String?) : IrcEvent(metadata)

/** Raised when a channel topic's metadata is discovered. */
class ChannelTopicMetadataDiscovered(metadata: EventMetadata, val channel: String, val user: User, val setTime: LocalDateTime) : IrcEvent(metadata)

/**
 * Raised when a channel's topic is changed.
 *
 * If the topic has been unset (cleared), [topic] will be `null`
 */
class ChannelTopicChanged(metadata: EventMetadata, val user: User, val channel: String, val topic: String?) : IrcEvent(metadata)

/** Raised when a message is received. */
class MessageReceived(metadata: EventMetadata, val user: User, val target: String, val message: String, val messageId: String? = null) : IrcEvent(metadata)

/**
 * Raised when a notice is received.
 *
 * The [user] may in fact be a server, or have a nickname of `*` while connecting.
 */
class NoticeReceived(metadata: EventMetadata, val user: User, val target: String, val message: String) : IrcEvent(metadata)

/** Raised when an action is received. */
class ActionReceived(metadata: EventMetadata, val user: User, val target: String, val action: String, val messageId: String? = null) : IrcEvent(metadata)

/** Raised when a CTCP is received. */
class CtcpReceived(metadata: EventMetadata, val user: User, val target: String, val type: String, val content: String) : IrcEvent(metadata)

/** Raised when a CTCP reply is received. */
class CtcpReplyReceived(metadata: EventMetadata, val user: User, val target: String, val type: String, val content: String) : IrcEvent(metadata)

/** Raised when a user quits. */
class UserQuit(metadata: EventMetadata, val user: User, val reason: String = "") : IrcEvent(metadata)

/** Raised when a user changes nickname. */
class UserNickChanged(metadata: EventMetadata, val user: User, val newNick: String) : IrcEvent(metadata)

/**
 * Raised when a user's account changes (i.e., they auth'd or deauth'd with services).
 *
 * This event is only raised if the server supports the `account-notify` capability.
 */
class UserAccountChanged(metadata: EventMetadata, val user: User, val newAccount: String?) : IrcEvent(metadata)

/** Raised when available server capabilities are received. More batches may follow. */
class ServerCapabilitiesReceived(metadata: EventMetadata, val capabilities: Map<Capability, String>) : IrcEvent(metadata)

/** Raised when our requested capabilities are acknowledged. More batches may follow. */
class ServerCapabilitiesAcknowledged(metadata: EventMetadata, val capabilities: Map<Capability, String>) : IrcEvent(metadata)

/** Raised when the server has finished sending us capabilities. */
class ServerCapabilitiesFinished(metadata: EventMetadata) : IrcEvent(metadata)

/** Raised when a line of the Message Of the Day has been received. */
class MotdLineReceived(metadata: EventMetadata, val line: String, val first: Boolean = false) : IrcEvent(metadata)

/** Raised when a Message Of the Day has completed. */
class MotdFinished(metadata: EventMetadata, val missing: Boolean = false) : IrcEvent(metadata)

/**
 * Raised when a mode change occurs.
 *
 * If [discovered] is true then the event is in response to the server providing the full set of modes on the target,
 * and the given modes are thus exhaustive. Otherwise, the modes are a sequence of changes to apply to the existing
 * state.
 */
class ModeChanged(metadata: EventMetadata, val target: String, val modes: String, val arguments: Array<String>, val discovered: Boolean = false) : IrcEvent(metadata)

/** Raised when an AUTHENTICATION message is received. [argument] is `null` if the server sent an empty reply ("+") */
class AuthenticationMessage(metadata: EventMetadata, val argument: String?) : IrcEvent(metadata)

/** Raised when a SASL attempt finishes, successfully or otherwise. */
class SaslFinished(metadata: EventMetadata, var success: Boolean) : IrcEvent(metadata)

/** Raised when the server says our SASL mechanism isn't available, but gives us a list of others. */
class SaslMechanismNotAvailableError(metadata: EventMetadata, var mechanisms: Collection<String>) : IrcEvent(metadata)

/** Indicates a batch of messages has begun. */
class BatchStarted(metadata: EventMetadata, val referenceId: String, val batchType: String, val params: Array<String>) : IrcEvent(metadata)

/** Indicates a batch of messages has finished. */
class BatchFinished(metadata: EventMetadata, val referenceId: String) : IrcEvent(metadata)

/** A batch of events that should be handled together. */
class BatchReceived(metadata: EventMetadata, val type: String, val params: Array<String>, val events: List<IrcEvent>) : IrcEvent(metadata)
