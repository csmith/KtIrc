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

    internal operator fun plusAssign(item: String) {
        if (!contains(item)) {
            items += item
        }
    }

    internal operator fun minusAssign(item: String) {
        items.removeIf { caseMappingProvider().areEquivalent(it, item) }
    }

    /**
     * Determines if this set contains the given item, case-insensitively.
     */
    operator fun contains(item: String) = items.any { caseMappingProvider().areEquivalent(it, item) }

    /**
     * Returns a read-only iterator over items in this set.
     */
    override operator fun iterator() = items.iterator().iterator()

    /**
     * Returns true if this set is empty.
     */
    fun isEmpty() = items.isEmpty()

}
