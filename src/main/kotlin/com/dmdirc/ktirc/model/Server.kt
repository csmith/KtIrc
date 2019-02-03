package com.dmdirc.ktirc.model

/**
 * Describes a server to connect to.
 */
data class Server(val host: String, val port: Int, val tls: Boolean = false, val password: String? = null)
