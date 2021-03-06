package com.dmdirc.ktirc.model

/**
 * Describes a user on IRC.
 */
data class User(
        var nickname: String,
        var ident: String? = null,
        var hostname: String? = null,
        var account: String? = null,
        var realName: String? = null,
        var awayMessage: String? = null
) {
    internal fun updateFrom(other: User) {
        nickname = other.nickname
        other.ident?.let { ident = it }
        other.hostname?.let { hostname = it }
        other.account?.let { account = it }
        other.realName?.let { realName = it }
        other.awayMessage?.let { awayMessage = it }
    }

    internal fun reset(newNickname: String) {
        nickname = newNickname
        ident = null
        hostname = null
        account = null
        realName = null
        awayMessage = null
    }
}

internal fun ByteArray.asUser() = String(this).asUser()

internal fun String.asUser(): User {
    val identOffset = indexOf('!')
    return if (identOffset >= 0) {
        val hostOffset = indexOf('@', identOffset)
        if (hostOffset >= 0) {
            User(substring(0 until identOffset), substring(identOffset + 1 until hostOffset), substring(hostOffset + 1))
        } else {
            User(substring(0 until identOffset), substring(identOffset + 1))
        }
    } else {
        User(this)
    }
}
