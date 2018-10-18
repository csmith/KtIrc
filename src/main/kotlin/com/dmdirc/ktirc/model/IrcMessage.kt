package com.dmdirc.ktirc.model

class IrcMessage(val tags: Map<MessageTag, String>, val prefix: ByteArray?, val command: String, val params: List<ByteArray>)

sealed class MessageTag(val name: String) {
    object AccountName: MessageTag("account")
    object ServerTime : MessageTag("time")
}

val messageTags: Map<String, MessageTag> by lazy {
    MessageTag::class.nestedClasses.map { it.objectInstance as MessageTag }.associateBy { it.name }
}