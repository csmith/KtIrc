package com.dmdirc.ktirc.messages

/** Construct a message indicating the end of capability negotiation. */
internal fun capabilityEndMessage() = "CAP END"
/** Construct a message requesting the specified caps are enabled. */
internal fun capabilityRequestMessage(capabilities: List<String>) = "CAP REQ :${capabilities.joinToString(" ")}"
/** Construct a message to join the given channel. */
fun joinMessage(channel: String) = "JOIN :$channel"
/** Construct a message to change to the given nickname. */
fun nickMessage(nick: String) = "NICK :$nick"
/** Construct a message to provide the connection password to the server. */
internal fun passwordMessage(password: String) = "PASS :$password"
/** Construct a message to reply to a PING event. */
internal fun pongMessage(nonce: ByteArray) = "PONG :${String(nonce)}"
/** Construct a message to send a private message to a user or channel. */
fun privmsgMessage(target: String, message: String) = "PRIVMSG $target :$message"
/** Construct a message to register a user with the server. */
internal fun userMessage(userName: String, localHostName: String, serverHostName: String, realName: String) = "USER $userName $localHostName $serverHostName :$realName"
