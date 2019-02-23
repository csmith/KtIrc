package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ChannelTopicChanged
import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.messages.RPL_NOTOPIC
import com.dmdirc.ktirc.messages.RPL_TOPIC
import com.dmdirc.ktirc.messages.RPL_TOPICWHOTIME
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.asUser
import com.dmdirc.ktirc.util.currentTimeZoneProvider
import java.time.Instant
import java.time.LocalDateTime

internal class TopicProcessor : MessageProcessor {

    override val commands = arrayOf(RPL_TOPIC, RPL_TOPICWHOTIME, RPL_NOTOPIC, "TOPIC")

    override fun process(message: IrcMessage) = sequence {
        when (message.command) {
            RPL_TOPIC -> yield(ChannelTopicDiscovered(message.metadata, message.channel, String(message.params[2])))
            RPL_NOTOPIC -> yield(ChannelTopicDiscovered(message.metadata, message.channel, null))
            RPL_TOPICWHOTIME -> yield(ChannelTopicMetadataDiscovered(
                    message.metadata, message.channel, message.params[2].asUser(), message.topicSetTime))
            "TOPIC" -> message.sourceUser?.let { yield(ChannelTopicChanged(message.metadata, it, String(message.params[0]), String(message.params[1]))) }
        }
    }.toList()

    private val IrcMessage.channel
        get() = String(params[1])

    private val IrcMessage.topicSetTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochSecond(String(params[3]).toLong()), currentTimeZoneProvider())

}
