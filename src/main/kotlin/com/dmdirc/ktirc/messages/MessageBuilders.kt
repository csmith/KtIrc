package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.IrcClient

/** Sends a message indicating the end of capability negotiation. */
internal fun IrcClient.sendCapabilityEnd() = send("CAP END")
/** Sends a message requesting the specified caps are enabled. */
internal fun IrcClient.sendCapabilityRequest(capabilities: List<String>) = send("CAP REQ :${capabilities.joinToString(" ")}")
/** Sends a request to join the given channel. */
fun IrcClient.sendJoin(channel: String) = send("JOIN :$channel")
/** Sends a request to change to the given nickname. */
fun IrcClient.sendNickChange(nick: String) = send("NICK :$nick")
/** Sends the connection password to the server. */
internal fun IrcClient.sendPassword(password: String) = send("PASS :$password")
/** Sends a response to a PING event. */
internal fun IrcClient.sendPong(nonce: ByteArray) = send("PONG :${String(nonce)}")
/** Sends a private message to a user or channel. */
fun IrcClient.sendMessage(target: String, message: String) = send("PRIVMSG $target :$message")
/** Sends a message to register a user with the server. */
internal fun IrcClient.sendUser(userName: String, localHostName: String, serverHostName: String, realName: String) = send("USER $userName $localHostName $serverHostName :$realName")
