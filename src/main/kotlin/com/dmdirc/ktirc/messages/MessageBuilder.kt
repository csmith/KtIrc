package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.model.MessageTag

internal class MessageBuilder {

    fun build(tags: Map<MessageTag, String>, command: String, arguments: Array<out String>) =
            // TODO: Check line length
            buildString {
                append(tags.toPrefix())
                append(command)
                append(arguments.toSuffix())
            }.toByteArray()

    private fun Map<MessageTag, String>.toPrefix() = when {
        isEmpty() -> ""
        // TODO: Check if the server actually understands tags here
        // TODO: Check maximum length of tags
        else ->
            map { "${it.key.name}=${it.value.escapeTagValue()}" }
                    .joinToString(separator = ";", prefix = "@", postfix = " ")
    }

    private fun Array<out String>.toSuffix() = when (size) {
        0 -> ""
        1 -> this[0].asLastParam()
        else -> dropLast(1).joinToString(separator = " ", prefix = " ") + last().asLastParam()
    }

    private fun String.asLastParam() = when {
        contains(' ') || startsWith(':') -> " :$this"
        else -> " $this"
    }

    private fun String.escapeTagValue() = replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace(";", "\\:")
            .replace(" ", "\\s")

}
