package com.dmdirc.ktirc.io

/**
 * Supported options for mapping between uppercase and lowercase characters.
 */
enum class CaseMapping(private val lowerToUpperMapping: Pair<IntRange, IntRange>) {

    /** Standard ASCII mapping, `a-z` is mapped to `A-Z`. */
    Ascii(97..122 to 65..90),
    /** Mapping probably intended by RFC14159, `a-z{|}~` is mapped to `A-Z[\]^`. */
    Rfc(97..126 to 65..94),
    /** Mapping according to a strict interpretation of RFC14159, `a-z{|}` is mapped to `A-Z[\]]`. */
    RfcStrict(97..125 to 65..93);

    companion object {
        internal fun fromName(name: String) = when(name.toLowerCase()) {
            "ascii" -> Ascii
            "rfc1459" -> Rfc
            "rfc1459-strict" -> RfcStrict
            else -> Rfc
        }
    }

    /**
     * Determines if the two strings are equivalent under the casemapping rules.
     */
    fun areEquivalent(string1: String, string2: String): Boolean {
        return string1.length == string2.length
                && string1.zip(string2).all { (c1, c2) -> areEquivalent(c1, c2) }
    }

    private fun areEquivalent(char1: Char, char2: Char) = char1 == char2 || char1.toUpper() == char2.toUpper()
    private fun Char.toUpper() = this + if (this.toInt() in lowerToUpperMapping.first) lowerToUpperMapping.second.start - lowerToUpperMapping.first.start else 0

}
