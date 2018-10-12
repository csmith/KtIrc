package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.io.IrcMessage

interface MessageProcessor {

    /**
     * The messages which this handler can process.
     */
    val commands: Array<String>

    /**
     * Processes the given message.
     */
    fun process(message: IrcMessage)

}