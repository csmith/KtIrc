package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

class UserState(caseMappingProvider: () -> CaseMapping) {

    private val users = UserMap(caseMappingProvider)

    operator fun get(nickname: String) = users[nickname]
    operator fun plusAssign(details: User) { users += KnownUser(details) }
    operator fun minusAssign(details: User) { users -= details.nickname }

    fun update(details: User, oldNick: String = details.nickname) {
        users[oldNick]?.details?.updateFrom(details)
    }

}

class KnownUser(val details: User) {

    val channels = mutableListOf<String>()

    operator fun plusAssign(channel: String) { channels += channel }
    operator fun minusAssign(channel: String) { channels -= channel }
    operator fun contains(channel: String) = channel in channels

}