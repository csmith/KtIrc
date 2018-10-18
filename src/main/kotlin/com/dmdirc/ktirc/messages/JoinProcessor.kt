package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User

internal class JoinProcessor : MessageProcessor {

    override val commands = arrayOf("JOIN")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        user.addExtendedJoinFields(message.params)
        listOf(ChannelJoined(message.time, user, String(message.params[0])))
    } ?: emptyList()

    private fun User.addExtendedJoinFields(params: List<ByteArray>) {
        if (params.size == 3) {
            String(params[1]).let { account = if (it == "*") null else it }
            realName = String(params[2])
        }
    }

}