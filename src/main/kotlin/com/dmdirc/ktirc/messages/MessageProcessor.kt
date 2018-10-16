package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.IrcMessage

interface MessageProcessor {

    /**
     * The messages which this handler can process.
     */
    val commands: Array<String>

    /**
     * Processes the given message.
     */
    fun process(message: IrcMessage): List<IrcEvent>

}

val messageProcessors = setOf(
        ISupportProcessor(),
        JoinProcessor(),
        NamesProcessor(),
        PingProcessor(),
        WelcomeProcessor()
)