package com.dmdirc.ktirc.model

/**
 * Describes the client's profile information that will be provided to a server.
 *
 * @param initialNick The initial nickname to attempt to use
 * @param realName The real name to provide to the IRC server
 * @param userName The username to use if your system doesn't supply an IDENT response (or the server doesn't ask)
 * @param authUsername The username to authenticate over SASL with (e.g. services account)
 * @param authPassword The password to authenticate the [authUsername] account with
 */
data class Profile(
        val initialNick: String,
        val realName: String,
        val userName: String,
        val authUsername: String? = null,
        val authPassword: String? = null
)
