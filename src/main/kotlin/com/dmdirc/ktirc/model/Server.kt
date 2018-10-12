package com.dmdirc.ktirc.model

data class Server(val host: String, val port: Int, val ssl: Boolean = false, val password: String? = null)