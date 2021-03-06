package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import java.util.*

/**
 * Provides a case-insensitive mapping from a String to some value, according to the provided [CaseMapping].
 */
abstract class CaseInsensitiveMap<T>(private val caseMappingProvider: () -> CaseMapping, private val nameOf: (T) -> String) : Iterable<T> {

    private val values = Collections.synchronizedSet(HashSet<T>())

    /** Gets the value of the given key, if present. */
    operator fun get(name: String) = synchronized(values) { values.find { caseMappingProvider().areEquivalent(nameOf(it), name) } }

    internal operator fun plusAssign(value: T): Unit = synchronized(values) {
        values.add(value)
    }

    internal operator fun minusAssign(name: String): Unit = synchronized(values) {
        values.removeIf { caseMappingProvider().areEquivalent(nameOf(it), name) }
    }

    /** Returns true if the given key exists in this map. */
    operator fun contains(name: String) = get(name) != null

    /** Provides a read-only iterator over the values in this map. */
    override fun iterator() = HashSet(values).iterator().iterator()

    internal fun clear() = values.clear()

    internal fun removeIf(predicate: (T) -> Boolean): Unit = synchronized(values) {
        values.removeIf(predicate)
    }

}

/** Maps a channel name to a [ChannelState] instance. */
class ChannelStateMap(caseMappingProvider: () -> CaseMapping) : CaseInsensitiveMap<ChannelState>(caseMappingProvider, ChannelState::name)

/** Maps a nickname to a [ChannelUser] instance. */
class ChannelUserMap(caseMappingProvider: () -> CaseMapping) : CaseInsensitiveMap<ChannelUser>(caseMappingProvider, ChannelUser::nickname)

/** Maps a nickname to a [KnownUser] instance. */
class UserMap(caseMappingProvider: () -> CaseMapping) : CaseInsensitiveMap<KnownUser>(caseMappingProvider, { it.details.nickname })
