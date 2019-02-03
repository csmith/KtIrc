package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.util.currentTimeProvider
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import java.time.Instant
import java.time.LocalDateTime

class IrcMessage(val tags: Map<MessageTag, String>, val prefix: ByteArray?, val command: String, val params: List<ByteArray>) {

    val time: LocalDateTime = if (MessageTag.ServerTime in tags) {
        LocalDateTime.ofInstant(Instant.parse(tags[MessageTag.ServerTime]), currentTimeZoneProvider())
    } else {
        currentTimeProvider()
    }

    val sourceUser by lazy {
        prefix?.asUser()?.apply {
            tags[MessageTag.AccountName]?.let { account = it }
        }
    }

}

sealed class MessageTag(val name: String) {
    object AccountName : MessageTag("account")
    object ServerTime : MessageTag("time")
}

internal val messageTags: Map<String, MessageTag> by lazy {
    MessageTag::class.nestedClasses.map { it.objectInstance as MessageTag }.associateBy { it.name }
}
