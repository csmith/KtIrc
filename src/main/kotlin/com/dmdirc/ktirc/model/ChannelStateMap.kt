package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

class ChannelStateMap(private val caseMappingProvider: () -> CaseMapping) : Iterable<ChannelState> {

    private val channels = HashSet<ChannelState>()

    operator fun get(name: String) = channels.find { caseMappingProvider().areEquivalent(it.name, name) }

    operator fun plusAssign(state: ChannelState) {
        require(get(state.name) == null) { "Channel state already registered: ${state.name}"}
        channels.add(state)
    }

    operator fun minusAssign(state: ChannelState) {
        channels.removeIf { caseMappingProvider().areEquivalent(it.name, state.name) }
    }

    operator fun contains(name: String) = get(name) != null

    override fun iterator() = channels.iterator()

}