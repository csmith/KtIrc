package com.dmdirc.ktirc.model

data class User(val nickname: String, val ident: String? = null, val hostname: String? = null)

fun ByteArray.asUser(): User {
    val string = String(this)
    val identOffset = string.indexOf('!')
    return if (identOffset >= 0) {
        val hostOffset = string.indexOf('@', identOffset)
        if (hostOffset >= 0) {
            User(string.substring(0 until identOffset), string.substring(identOffset + 1 until hostOffset), string.substring(hostOffset + 1))
        } else {
            User(string.substring(0 until identOffset), string.substring(identOffset + 1))
        }
    } else {
        User(string)
    }
}