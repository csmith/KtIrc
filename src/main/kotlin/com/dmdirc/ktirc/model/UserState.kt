package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

class UserState(private val caseMappingProvider: () -> CaseMapping): Iterable<KnownUser> {

    private val users = UserMap(caseMappingProvider)

    operator fun get(nickname: String) = users[nickname]
    operator fun get(user: User) = users[user.nickname]

    operator fun plusAssign(details: User) { users += KnownUser(caseMappingProvider, details) }
    operator fun minusAssign(details: User) { users -= details.nickname }

    override operator fun iterator() = users.iterator()

    fun removeIf(predicate: (KnownUser) -> Boolean) = users.removeIf(predicate)

    fun update(user: User, oldNick: String = user.nickname) {
        users[oldNick]?.details?.updateFrom(user)
    }

    fun addToChannel(user: User, channel: String) {
        users[user.nickname]?.let {
            it += channel
        } ?: run {
            users += KnownUser(caseMappingProvider, user).apply { channels += channel }
        }
    }

}

class KnownUser(caseMappingProvider: () -> CaseMapping, val details: User) {

    val channels = CaseInsensitiveSet(caseMappingProvider)

    operator fun plusAssign(channel: String) { channels += channel }
    operator fun minusAssign(channel: String) { channels -= channel }
    operator fun contains(channel: String) = channel in channels

}