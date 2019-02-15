package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.BatchFinished
import com.dmdirc.ktirc.events.BatchStarted
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.model.IrcMessage

internal class BatchProcessor : MessageProcessor {

    override val commands = arrayOf("BATCH")

    override fun process(message: IrcMessage): List<IrcEvent> {
        val args = message.params.map { String(it) }
        val id = args[0]
        return when (id[0]) {
            '+' -> listOf(BatchStarted(message.metadata, id.substring(1), args[1], args.subList(2, args.size).toTypedArray()))
            '-' -> listOf(BatchFinished(message.metadata, id.substring(1)))
            else -> emptyList()
        }
    }

}
