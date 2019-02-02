package com.dmdirc.ktirc.io

enum class CaseMapping(private val lowerToUpperMapping: Pair<IntRange, IntRange>) {

    Ascii(97..122 to 65..90),
    Rfc(97..126 to 65..94),
    RfcStrict(97..125 to 65..93);

    companion object {
        internal fun fromName(name: String) = when(name.toLowerCase()) {
            "ascii" -> Ascii
            "rfc1459" -> Rfc
            "rfc1459-strict" -> RfcStrict
            else -> Rfc
        }
    }

    fun areEquivalent(string1: String, string2: String): Boolean {
        return string1.length == string2.length
                && string1.zip(string2).all { (c1, c2) -> areEquivalent(c1, c2) }
    }

    private fun areEquivalent(char1: Char, char2: Char) = char1 == char2 || char1.toUpper() == char2.toUpper()
    private fun Char.toUpper() = this + if (this.toInt() in lowerToUpperMapping.first) lowerToUpperMapping.second.start - lowerToUpperMapping.first.start else 0

}
