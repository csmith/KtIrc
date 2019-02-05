package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ModeChanged
import com.dmdirc.ktirc.model.IrcMessage

internal class ModeProcessor : MessageProcessor {

    override val commands = arrayOf(RPL_CHANNELMODEIS, RPL_UMODEIS, "MODE")

    override fun process(message: IrcMessage): List<ModeChanged> {
        val isDiscovery = message.command == RPL_CHANNELMODEIS || message.command == RPL_UMODEIS
        val paramOffset = if (message.command == RPL_CHANNELMODEIS) 1 else 0
        return listOf(ModeChanged(
                message.time,
                target = String(message.params[paramOffset]),
                modes = String(message.params[paramOffset + 1]),
                arguments = message.params.takeLast(message.params.size - paramOffset - 2).map { String(it) }.toTypedArray(),
                discovered = isDiscovery))
    }

}