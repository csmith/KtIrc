package com.dmdirc.ktirc.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CaseMappingTest {

    @Test
    fun `Equal unicode strings are equivalent regardless of case mapping`() {
        val unicode = "\uD83D\uDC69\u200D\uD83C\uDF73 \uD83D\uDC68\u200D\uD83C\uDF73 \uD83D\uDC69\u200D\uD83C\uDF93 \uD83D\uDC68\u200D\uD83C\uDF93"
        assertTrue(CaseMapping.Ascii.areEquivalent(unicode, unicode))
        assertTrue(CaseMapping.Rfc.areEquivalent(unicode, unicode))
        assertTrue(CaseMapping.RfcStrict.areEquivalent(unicode, unicode))
    }

    @Test
    fun `Different length strings are always not equivalent`() {
        val left = "abc"
        val right = "ABC "
        assertFalse(CaseMapping.Ascii.areEquivalent(left, right))
        assertFalse(CaseMapping.Rfc.areEquivalent(left, right))
        assertFalse(CaseMapping.RfcStrict.areEquivalent(left, right))
    }

    @Test
    fun `ASCII characters are equivalent for all mappings`() {
        val left = "the Quick Brown fox Jumps over the lazy dog"
        val right = "THE qUICK bROWN fox jUMPS OVER THE LAZY DOG"
        assertTrue(CaseMapping.Ascii.areEquivalent(left, right))
        assertTrue(CaseMapping.Rfc.areEquivalent(left, right))
        assertTrue(CaseMapping.RfcStrict.areEquivalent(left, right))
    }

    @Test
    fun `RFC characters are equivalent for RFC mappings not ASCII`() {
        val left = "[Hello\\There}"
        val right = "{Hello|There]"
        assertFalse(CaseMapping.Ascii.areEquivalent(left, right))
        assertTrue(CaseMapping.Rfc.areEquivalent(left, right))
        assertTrue(CaseMapping.RfcStrict.areEquivalent(left, right))
    }

    @Test
    fun `Tilde and caret are equivalent only for RFC mapping`() {
        val left = "~~^~~"
        val right = "~^^^^"
        assertFalse(CaseMapping.Ascii.areEquivalent(left, right))
        assertTrue(CaseMapping.Rfc.areEquivalent(left, right))
        assertFalse(CaseMapping.RfcStrict.areEquivalent(left, right))
    }

    @Test
    fun `FromName returns matching mapping`() {
        assertEquals(CaseMapping.Ascii, CaseMapping.fromName("ascii"))
        assertEquals(CaseMapping.Ascii, CaseMapping.fromName("Ascii"))
        assertEquals(CaseMapping.Rfc, CaseMapping.fromName("rfc1459"))
        assertEquals(CaseMapping.Rfc, CaseMapping.fromName("RFC1459"))
        assertEquals(CaseMapping.RfcStrict, CaseMapping.fromName("rfc1459-strict"))
        assertEquals(CaseMapping.RfcStrict, CaseMapping.fromName("Rfc1459-Strict"))
    }

    @Test
    fun `FromName falls back to RFC`() {
        assertEquals(CaseMapping.Rfc, CaseMapping.fromName(""))
        assertEquals(CaseMapping.Rfc, CaseMapping.fromName("foo"))
    }

}