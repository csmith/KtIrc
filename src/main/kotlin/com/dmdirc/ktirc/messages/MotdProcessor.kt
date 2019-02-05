package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.MotdFinished
import com.dmdirc.ktirc.model.IrcMessage

internal class MotdProcessor : MessageProcessor {

    override val commands = arrayOf(ERR_NOMOTD, RPL_ENDOFMOTD)

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            ERR_NOMOTD -> yield(MotdFinished(message.time, missing = true))
            RPL_ENDOFMOTD -> yield(MotdFinished(message.time))
        }
    }.toList()

}
