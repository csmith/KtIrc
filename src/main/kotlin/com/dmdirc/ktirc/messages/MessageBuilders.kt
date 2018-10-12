package com.dmdirc.ktirc.messages

fun joinMessage(channel: String) = "JOIN :$channel"
fun nickMessage(nick: String) = "NICK :$nick"
fun passwordMessage(password: String) = "PASS :$password"
fun pongMessage(nonce: ByteArray) = "PONG :${String(nonce)}"
fun userMessage(userName: String, localHostName: String, serverHostName: String, realName: String) = "USER $userName $localHostName $serverHostName :$realName"
