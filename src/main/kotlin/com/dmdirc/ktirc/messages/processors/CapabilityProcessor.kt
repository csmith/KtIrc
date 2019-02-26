package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.events.ServerCapabilitiesAcknowledged
import com.dmdirc.ktirc.events.ServerCapabilitiesFinished
import com.dmdirc.ktirc.events.ServerCapabilitiesReceived
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.capabilities
import com.dmdirc.ktirc.util.logger

internal class CapabilityProcessor : MessageProcessor {

    private val log by logger()

    override val commands = arrayOf("CAP")

    override fun process(message: IrcMessage) = when {
        message.params.size < 2 -> {
            log.warning { "Discarding CAP with insufficient args: $message" }
            emptyList()
        }
        message.subCommand == "LS" -> handleList(message.metadata, message.subCommandArguments)
        message.subCommand == "ACK" -> listOf(ServerCapabilitiesAcknowledged(message.metadata, message.params.capabilities))
        else -> {
            log.warning { "Discarding CAP with unknown subcommand: $message" }
            emptyList()
        }
    }

    private fun handleList(metadata: EventMetadata, lsParams: List<ByteArray>) = sequence {
        yield(ServerCapabilitiesReceived(metadata, lsParams.capabilities))
        if (lsParams.size < 2 || String(lsParams[0]) != "*") {
            yield(ServerCapabilitiesFinished(metadata))
        }
    }.toList()

    private val IrcMessage.subCommand
        get() = String(params[1])

    private val IrcMessage.subCommandArguments
        get() = params.slice(1 until params.size)

    private val List<ByteArray>.capabilities
        get() = with (String(last())) { if (isEmpty()) emptyMap() else split(' ').toCapabilities() }

    private fun List<String>.toCapabilities() = sequence {
        forEach { cap ->
            val index = cap.indexOf('=')
            val name = if (index == -1) cap else cap.substring(0 until index)
            val value = if (index == -1) "" else cap.substring(index + 1)
            capabilities[name]?.let { yield(name to value) } ?: log.info { "Unknown capability: $name (value: $value)" }
        }
    }.toMap()

}
