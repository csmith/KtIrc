package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CaseInsensitiveSetTest {

    val set = CaseInsensitiveSet { CaseMapping.Rfc }

    @Test
    fun `isEmpty returns true when there are no items`() {
        assertTrue(set.isEmpty())

        set += "gibson"
        assertFalse(set.isEmpty())

        set -= "gibson"
        assertTrue(set.isEmpty())
    }

    @Test
    fun `items can be added to the set`() {
        set += "libby"
        assertEquals(setOf("libby"), set.toSet())

        set += "dade"
        assertEquals(setOf("libby", "dade"), set.toSet())
    }

    @Test
    fun `items can be removed from the set`() {
        set += "acidBurn"
        set += "zeroCool"
        set -= "acidBurn"
        assertEquals(setOf("zeroCool"), set.toSet())
    }

    @Test
    fun `items can be removed from the set using a different case`() {
        set += "acidBurn"
        set += "zeroCool"
        set -= "ACIDburn"
        assertEquals(setOf("zeroCool"), set.toSet())
    }

    @Test
    fun `adding the same item in a different case has no effect`() {
        set += "acidBurn[]"
        set += "Acidburn[]"
        set += "acidBurn{}"
        assertEquals(1, set.count())
    }

    @Test
    fun `contains indicates if a case-insensitive match is in the set`() {
        set += "acidBurn[]"
        assertTrue("acidBurn[]" in set)
        assertTrue("AcidBurn[]" in set)
        assertTrue("acidBurn{}" in set)
        assertFalse("zeroCool" in set)
    }

}