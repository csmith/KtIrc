package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.state.ServerState
import com.dmdirc.ktirc.state.serverFeatures
import com.dmdirc.ktirc.util.logger
import kotlin.reflect.KClass

class ISupportProcessor(val serverState: ServerState) : MessageProcessor {

    private val log by logger()

    override val commands = arrayOf("005")

    override fun process(message: IrcMessage): List<IrcEvent> {
        // Ignore the first (nickname) and last ("are supported by this server") params
        for (i in 1 until message.params.size - 1) {
            parseParam(message.params[i])
        }
        return emptyList()
    }

    private fun parseParam(param: ByteArray) = when (param[0]) {
        '-'.toByte() -> resetFeature(param.sliceArray(1 until param.size))
        else -> when (val equals = param.indexOf('='.toByte())) {
            -1 -> enableFeatureWithDefault(param)
            else -> enableFeature(param.sliceArray(0 until equals), param.sliceArray(equals + 1 until param.size))
        }
    }

    private fun resetFeature(name: ByteArray) = name.asFeature()?.let {
        serverState.resetFeature(it)
        log.finer { "Reset feature ${it::class}" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun enableFeature(name: ByteArray, value: ByteArray) {
        name.asFeature()?.let { feature ->
            serverState.setFeature(feature, value.cast(feature.type))
            log.finer { "Set feature ${feature::class} to ${String(value)}" }
        }
    }

    private fun enableFeatureWithDefault(name: ByteArray) {
        name.asFeature()?.let { feature ->
            when (feature.type) {
                Boolean::class -> serverState.setFeature(feature, true)
                else -> TODO("not implemented")
            }
        }
    }

    private fun ByteArray.asFeature() = serverFeatures[String(this)]
            ?: run {
                log.warning { "Unknown feature in 005: ${String(this)}" }
                null
            }

    private fun ByteArray.cast(to: KClass<out Any>): Any = when (to) {
        Int::class -> String(this).toInt()
        String::class -> String(this)
        CaseMapping::class -> CaseMapping.fromName(String(this))
        else -> TODO("not implemented")
    }

}