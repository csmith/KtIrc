package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.asUser
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import java.time.Instant
import java.time.LocalDateTime

internal class TopicProcessor : MessageProcessor {

    override val commands = arrayOf(RPL_TOPIC, RPL_TOPICWHOTIME)

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            RPL_TOPIC -> yield(ChannelTopicDiscovered(message.time, message.channel, String(message.params[2])))
            RPL_TOPICWHOTIME -> yield(ChannelTopicMetadataDiscovered(
                    message.time, message.channel, message.params[2].asUser(), message.topicSetTime))
        }
    }.toList()

    private val IrcMessage.channel
        get() = String(params[1])

    private val IrcMessage.topicSetTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochSecond(String(params[3]).toLong()), currentTimeZoneProvider())

}
