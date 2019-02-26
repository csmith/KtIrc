package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.CtcpReplyReceived
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.NoticeReceived
import com.dmdirc.ktirc.messages.CTCP_BYTE
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User

internal class NoticeProcessor : MessageProcessor {

    override val commands = arrayOf("NOTICE")

    override fun process(message: IrcMessage) = when {
            message.isCtcp() -> handleCtcp(message,  message.sourceUser)
            else -> listOf(NoticeReceived(message.metadata,  message.sourceUser ?: User("*"), String(message.params[0]), String(message.params[1])))
        }

    private fun handleCtcp(message: IrcMessage, user: User?): List<IrcEvent> {
        user ?: return emptyList()
        val content = String(message.params[1].sliceArray(1 until message.params[1].size - 1))
        val parts = content.split(' ', limit=2)
        val body = if (parts.size == 2) parts[1] else ""
        return listOf(CtcpReplyReceived(message.metadata, user, String(message.params[0]), parts[0], body))
    }

    private fun IrcMessage.isCtcp() = params[1].size > 2 && params[1][0] == CTCP_BYTE && params[1][params[1].size - 1] == CTCP_BYTE

}
