package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

abstract class CaseInsensitiveMap<T>(private val caseMappingProvider: () -> CaseMapping, private val nameOf: (T) -> String) : Iterable<T> {

    private val values = HashSet<T>()

    operator fun get(name: String) = values.find { caseMappingProvider().areEquivalent(nameOf(it), name) }

    operator fun plusAssign(value: T) {
        require(get(nameOf(value)) == null) { "Value already registered: ${nameOf(value)}"}
        values.add(value)
    }

    operator fun minusAssign(name: String) {
        values.removeIf { caseMappingProvider().areEquivalent(nameOf(it), name) }
    }

    operator fun contains(name: String) = get(name) != null

    override fun iterator() = values.iterator()

    fun clear() = values.clear()

}

class ChannelStateMap(caseMappingProvider: () -> CaseMapping) : CaseInsensitiveMap<ChannelState>(caseMappingProvider, ChannelState::name)
class ChannelUserMap(caseMappingProvider: () -> CaseMapping) : CaseInsensitiveMap<ChannelUser>(caseMappingProvider, ChannelUser::nickname)
