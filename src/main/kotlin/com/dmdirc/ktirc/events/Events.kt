@file:Suppress("ArrayInDataClass")

package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.User

sealed class IrcEvent

/** Raised when the server initially welcomes us to the IRC network. */
data class ServerWelcome(val localNick: String) : IrcEvent()

/** Raised when the features supported by the server have changed. This may occur numerous times. */
data class ServerFeaturesUpdated(val serverFeatures: ServerFeatureMap) : IrcEvent()

/** Raised when the connection to the server has been established, configuration information has been received, etc. */
// TODO: Implement
object ServerConnected : IrcEvent()

/** Raised whenever a PING is received from the server. */
data class PingReceived(val nonce: ByteArray) : IrcEvent()

/** Raised when a user joins a channel. */
data class ChannelJoined(val user: User, val channel: String) : IrcEvent()

/** Raised when a user leaves a channel. */
data class ChannelParted(val user: User, val channel: String, val reason: String = "") : IrcEvent()

/** Raised when a batch of the channel's member list has been received. More batches may follow. */
data class ChannelNamesReceived(val channel: String, val names: List<String>) : IrcEvent()

/** Raised when the entirety of the channel's member list has been received. */
data class ChannelNamesFinished(val channel: String) : IrcEvent()
