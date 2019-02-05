package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

/**
 * Keeps track of all known users that are in a common channel.
 */
class UserState(private val caseMappingProvider: () -> CaseMapping): Iterable<KnownUser> {

    private val users = UserMap(caseMappingProvider)

    /** Gets the [KnownUser] with the given nickname. */
    operator fun get(nickname: String) = users[nickname]
    /** Gets the [KnownUser] for a given user. */
    operator fun get(user: User) = users[user.nickname]

    internal operator fun plusAssign(details: User) { users += KnownUser(caseMappingProvider, details) }
    internal operator fun minusAssign(details: User) { users -= details.nickname }
    internal operator fun minusAssign(nickname: String) { users -= nickname }

    /** Provides a read-only iterator of all users. */
    override operator fun iterator() = users.iterator().iterator()

    internal fun removeIf(predicate: (KnownUser) -> Boolean) = users.removeIf(predicate)

    internal fun update(user: User, oldNick: String = user.nickname) {
        users[oldNick]?.details?.updateFrom(user)
    }

    internal fun addToChannel(user: User, channel: String) {
        users[user.nickname]?.let {
            it += channel
        } ?: run {
            users += KnownUser(caseMappingProvider, user).apply { channels += channel }
        }
    }

}

/**
 * Describes a user we know about and the channels that caused them to be known.
 */
class KnownUser(caseMappingProvider: () -> CaseMapping, val details: User) {

    /** The channels we have in common with the user. */
    val channels = CaseInsensitiveSet(caseMappingProvider)

    internal operator fun plusAssign(channel: String) { channels += channel }
    internal operator fun minusAssign(channel: String) { channels -= channel }

    /** Determines if the user is in the specified channel. */
    operator fun contains(channel: String) = channel in channels

}
