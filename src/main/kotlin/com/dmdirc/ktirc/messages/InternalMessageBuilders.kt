package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.IrcClient

/** Sends a message to ask the server to list capabilities. */
internal fun IrcClient.sendCapabilityList() = send("CAP", "LS", "302")

/** Sends a message indicating the end of capability negotiation. */
internal fun IrcClient.sendCapabilityEnd() = send("CAP", "END")

/** Sends a message requesting the specified caps are enabled. */
internal fun IrcClient.sendCapabilityRequest(capabilities: Collection<String>) =
        send("CAP", "REQ", capabilities.joinToString(" "))

/** Sends the connection password to the server. */
internal fun IrcClient.sendPassword(password: String) = send("PASS", password)

/** Sends a response to a PING event. */
internal fun IrcClient.sendPong(nonce: ByteArray) = send("PONG", String(nonce))

/** Sends a message to register a user with the server. */
internal fun IrcClient.sendUser(userName: String, realName: String) = send("USER", userName, "0", "*", realName)

/** Starts an authentication request. */
internal fun IrcClient.sendAuthenticationMessage(data: String = "+") = send("AUTHENTICATE", data)