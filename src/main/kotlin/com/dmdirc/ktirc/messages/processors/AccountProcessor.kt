package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.UserAccountChanged
import com.dmdirc.ktirc.model.IrcMessage

internal class AccountProcessor : MessageProcessor {

    override val commands = arrayOf("ACCOUNT")

    override fun process(message: IrcMessage) = message.sourceUser?.let { user ->
        listOf(UserAccountChanged(message.metadata, user, message.accountName))
    } ?: emptyList()

    private val IrcMessage.accountName
        get() = with(String(params[0])) { if (this == "*") null else this }

}
