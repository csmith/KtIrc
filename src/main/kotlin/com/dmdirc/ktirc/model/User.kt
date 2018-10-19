package com.dmdirc.ktirc.model

data class User(
        var nickname: String,
        var ident: String? = null,
        var hostname: String? = null,
        var account: String? = null,
        var realName: String? = null,
        var awayMessage: String? = null
) {
    fun updateFrom(other: User) {
        nickname = other.nickname
        other.ident?.let { ident = it }
        other.hostname?.let { hostname = it }
        other.account?.let { account = it }
        other.realName?.let { realName = it }
        other.awayMessage?.let { awayMessage = it }
    }
}

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