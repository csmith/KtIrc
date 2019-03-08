package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.NicknameChangeError
import com.dmdirc.ktirc.events.NicknameChangeFailed
import com.dmdirc.ktirc.messages.ERR_ERRONEUSNICKNAME
import com.dmdirc.ktirc.messages.ERR_NICKCOLLISION
import com.dmdirc.ktirc.messages.ERR_NICKNAMEINUSE
import com.dmdirc.ktirc.messages.ERR_NONICKNAMEGIVEN
import com.dmdirc.ktirc.model.IrcMessage

internal class NickChangeErrorProcessor : MessageProcessor {

    override val commands = arrayOf(ERR_ERRONEUSNICKNAME, ERR_NICKCOLLISION, ERR_NICKNAMEINUSE, ERR_NONICKNAMEGIVEN)

    override fun process(message: IrcMessage) = listOf(NicknameChangeFailed(message.metadata, message.command.toNicknameChangeError()))

    private fun String.toNicknameChangeError(): NicknameChangeError = when(this) {
        ERR_ERRONEUSNICKNAME -> NicknameChangeError.ErroneousNickname
        ERR_NICKCOLLISION -> NicknameChangeError.Collision
        ERR_NICKNAMEINUSE -> NicknameChangeError.AlreadyInUse
        ERR_NONICKNAMEGIVEN -> NicknameChangeError.NoNicknameGiven
        else -> throw IllegalArgumentException("Unknown nick change error")
    }

}
