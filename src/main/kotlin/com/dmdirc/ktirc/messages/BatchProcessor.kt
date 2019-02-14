package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.model.IrcMessage

internal class BatchProcessor : MessageProcessor {

    override val commands = arrayOf("BATCH")

    override fun process(message: IrcMessage): List<IrcEvent> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
