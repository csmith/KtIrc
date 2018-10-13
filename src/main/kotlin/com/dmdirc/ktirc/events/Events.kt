@file:Suppress("ArrayInDataClass")

package com.dmdirc.ktirc.events

sealed class IrcEvent
object ServerConnected : IrcEvent()
data class PingReceived(val nonce: ByteArray): IrcEvent()