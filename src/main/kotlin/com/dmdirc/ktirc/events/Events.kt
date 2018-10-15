@file:Suppress("ArrayInDataClass")

package com.dmdirc.ktirc.events

import com.dmdirc.ktirc.model.ServerFeatureMap

sealed class IrcEvent

/**
 * Raised when the server initially welcomes us to the IRC network.
 */
data class ServerWelcome(val localNick: String): IrcEvent()

/**
 * Raised when the features supported by the server have changed. This may occur numerous times during the
 * connection phase.
 */
data class ServerFeaturesUpdated(val serverFeatures: ServerFeatureMap) : IrcEvent()

/**
 * Raised when the connection to the server has been established, configuration information has been received, etc.
 */
object ServerConnected : IrcEvent()

/**
 * Raised whenever a PING is received from the server.
 */
data class PingReceived(val nonce: ByteArray): IrcEvent()