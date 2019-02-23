package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.NicknameChangeFailed
import com.dmdirc.ktirc.messages.ERR_ERRONEUSNICKNAME
import com.dmdirc.ktirc.messages.ERR_NICKCOLLISION
import com.dmdirc.ktirc.messages.ERR_NICKNAMEINUSE
import com.dmdirc.ktirc.messages.ERR_NONICKNAMEGIVEN
import com.dmdirc.ktirc.model.IrcMessage

internal class NickChangeErrorProcessor : MessageProcessor {

    override val commands = arrayOf(ERR_ERRONEUSNICKNAME, ERR_NICKCOLLISION, ERR_NICKNAMEINUSE, ERR_NONICKNAMEGIVEN)

    override fun process(message: IrcMessage) = listOf(NicknameChangeFailed(message.metadata, message.command.toNicknameChangeError()))

    private fun String.toNicknameChangeError(): NicknameChangeFailed.NicknameChangeError = when(this) {
        ERR_ERRONEUSNICKNAME -> NicknameChangeFailed.NicknameChangeError.ErroneousNickname
        ERR_NICKCOLLISION -> NicknameChangeFailed.NicknameChangeError.Collision
        ERR_NICKNAMEINUSE -> NicknameChangeFailed.NicknameChangeError.AlreadyInUse
        ERR_NONICKNAMEGIVEN -> NicknameChangeFailed.NicknameChangeError.NoNicknameGiven
        else -> throw IllegalArgumentException("Unknown nick change error")
    }

}
