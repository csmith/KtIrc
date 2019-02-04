package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.MotdFinished
import com.dmdirc.ktirc.model.IrcMessage

internal class MotdProcessor : MessageProcessor {

    override val commands = arrayOf("422", "376")

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            "422" -> yield(MotdFinished(message.time, missing = true))
            "376" -> yield(MotdFinished(message.time))
        }
    }.toList()

}
