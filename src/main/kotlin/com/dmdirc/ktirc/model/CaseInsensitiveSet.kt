package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

/**
 * Maintains a set of strings that are compared case-insensitively according to the provided [CaseMapping]
 *
 * If the case mapping changes during the lifetime of this set, it is presumed that all items will also be
 * unique in the new case mapping. Otherwise, the behaviour of this class is undefined.
 */
class CaseInsensitiveSet(private val caseMappingProvider: () -> CaseMapping) : Iterable<String> {

    private val items = HashSet<String>()

    operator fun plusAssign(item: String) {
        if (!contains(item)) {
            items += item
        }
    }

    operator fun minusAssign(item: String) {
        items.removeIf { caseMappingProvider().areEquivalent(it, item) }
    }

    operator fun contains(item: String) = items.any { caseMappingProvider().areEquivalent(it, item) }

    override operator fun iterator() = items.iterator()

    fun isEmpty() = items.isEmpty()

}
