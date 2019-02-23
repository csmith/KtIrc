package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.MotdFinished
import com.dmdirc.ktirc.events.MotdLineReceived
import com.dmdirc.ktirc.messages.ERR_NOMOTD
import com.dmdirc.ktirc.messages.RPL_ENDOFMOTD
import com.dmdirc.ktirc.messages.RPL_MOTD
import com.dmdirc.ktirc.messages.RPL_MOTDSTART
import com.dmdirc.ktirc.model.IrcMessage

internal class MotdProcessor : MessageProcessor {

    override val commands = arrayOf(ERR_NOMOTD, RPL_MOTDSTART, RPL_MOTD, RPL_ENDOFMOTD)

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            RPL_MOTDSTART -> yield(MotdLineReceived(message.metadata, String(message.params[1]), true))
            RPL_MOTD -> yield(MotdLineReceived(message.metadata, String(message.params[1])))
            ERR_NOMOTD -> yield(MotdFinished(message.metadata, missing = true))
            RPL_ENDOFMOTD -> yield(MotdFinished(message.metadata))
        }
    }.toList()

}
