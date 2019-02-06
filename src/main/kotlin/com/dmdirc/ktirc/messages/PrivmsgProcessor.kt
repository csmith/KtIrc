package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ActionReceived
import com.dmdirc.ktirc.events.CtcpReceived
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.MessageTag
import com.dmdirc.ktirc.model.User

internal class PrivmsgProcessor : MessageProcessor {

    override val commands = arrayOf("PRIVMSG")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        listOf(when {
            message.isCtcp() -> handleCtcp(message, user)
            else -> MessageReceived(message.time, user, String(message.params[0]), String(message.params[1]), message.messageId)
        })
    } ?: emptyList()

    private fun handleCtcp(message: IrcMessage, user: User): IrcEvent {
        val content = String(message.params[1]).substring(1 until message.params[1].size - 1)
        val parts = content.split(' ', limit=2)
        val body = if (parts.size == 2) parts[1] else ""
        return when (parts[0].toUpperCase()) {
            "ACTION" -> ActionReceived(message.time, user, String(message.params[0]), body, message.messageId)
            else -> CtcpReceived(message.time, user, String(message.params[0]), parts[0], body)
        }
    }

    private fun IrcMessage.isCtcp() = params[1].size > 2 && params[1][0] == CTCP_BYTE && params[1][params[1].size - 1] == CTCP_BYTE

    private val IrcMessage.messageId
        get() = tags[MessageTag.MessageId]

}
