package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.model.Capability
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.User
import java.time.LocalDateTime

sealed class IrcEvent(val time: LocalDateTime)

/** Raised when the server initially welcomes us to the IRC network. */
class ServerWelcome(time: LocalDateTime, val localNick: String) : IrcEvent(time)

/** Raised when the features supported by the server have changed. This may occur numerous times. */
class ServerFeaturesUpdated(time: LocalDateTime, val serverFeatures: ServerFeatureMap) : IrcEvent(time)

/** Raised when the connection to the server has been established, configuration information has been received, etc. */
// TODO: Implement
class ServerConnected(time: LocalDateTime) : IrcEvent(time)

/** Raised whenever a PING is received from the server. */
class PingReceived(time: LocalDateTime, val nonce: ByteArray) : IrcEvent(time)

/** Raised when a user joins a channel. */
class ChannelJoined(time: LocalDateTime, val user: User, val channel: String) : IrcEvent(time)

/** Raised when a user leaves a channel. */
class ChannelParted(time: LocalDateTime, val user: User, val channel: String, val reason: String = "") : IrcEvent(time)

/** Raised when a batch of the channel's member list has been received. More batches may follow. */
class ChannelNamesReceived(time: LocalDateTime, val channel: String, val names: List<String>) : IrcEvent(time)

/** Raised when the entirety of the channel's member list has been received. */
class ChannelNamesFinished(time: LocalDateTime, val channel: String) : IrcEvent(time)

/** Raised when a message is received. */
class MessageReceived(time: LocalDateTime, val user: User, val target: String, val message: String) : IrcEvent(time)

/** Raised when a user quits. */
class UserQuit(time: LocalDateTime, val user: User, val reason: String = "") : IrcEvent(time)

/** Raised when available server capabilities are received. More batches may follow. */
class ServerCapabilitiesReceived(time: LocalDateTime, val capabilities: Map<Capability, String>) : IrcEvent(time)

/** Raised when our requested capabilities are acknowledged. More batches may follow. */
class ServerCapabilitiesAcknowledged(time: LocalDateTime, val capabilities: Map<Capability, String>) : IrcEvent(time)

/** Raised when the server has finished sending us capabilities. */
class ServerCapabilitiesFinished(time: LocalDateTime) : IrcEvent(time)