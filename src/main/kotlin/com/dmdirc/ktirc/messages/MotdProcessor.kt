package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.MotdFinished
import com.dmdirc.ktirc.events.MotdLineReceived
import com.dmdirc.ktirc.model.IrcMessage

internal class MotdProcessor : MessageProcessor {

    override val commands = arrayOf(ERR_NOMOTD, RPL_MOTDSTART, RPL_MOTD, RPL_ENDOFMOTD)

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            RPL_MOTDSTART -> yield(MotdLineReceived(message.time, String(message.params[1]), true))
            RPL_MOTD -> yield(MotdLineReceived(message.time, String(message.params[1])))
            ERR_NOMOTD -> yield(MotdFinished(message.time, missing = true))
            RPL_ENDOFMOTD -> yield(MotdFinished(message.time))
        }
    }.toList()

}
