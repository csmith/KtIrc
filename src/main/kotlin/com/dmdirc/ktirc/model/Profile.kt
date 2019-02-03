package com.dmdirc.ktirc.model

/** Describes the client's profile information that will be provided to a server. */
data class Profile(val initialNick: String, val realName: String, val userName: String)
