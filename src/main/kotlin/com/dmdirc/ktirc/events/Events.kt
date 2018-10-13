package com.dmdirc.ktirc.events

sealed class IrcEvent
object ServerConnected : IrcEvent()
