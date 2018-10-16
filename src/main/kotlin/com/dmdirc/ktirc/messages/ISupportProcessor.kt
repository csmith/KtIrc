package com.dmdirc.ktirc.messages

import com.dmdirc.ktirc.events.ServerFeaturesUpdated
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.io.IrcMessage
import com.dmdirc.ktirc.model.ModePrefixMapping
import com.dmdirc.ktirc.model.ServerFeatureMap
import com.dmdirc.ktirc.model.serverFeatures
import com.dmdirc.ktirc.util.logger
import kotlin.reflect.KClass

class ISupportProcessor : MessageProcessor {

    private val log by logger()

    override val commands = arrayOf("005")

    override fun process(message: IrcMessage) = listOf(ServerFeaturesUpdated(ServerFeatureMap().apply {
        // Ignore the first (nickname) and last ("are supported by this server") params
        for (i in 1 until message.params.size - 1) {
            parseParam(message.params[i])
        }
    }))

    private fun ServerFeatureMap.parseParam(param: ByteArray) = when (param[0]) {
        '-'.toByte() -> resetFeature(param.sliceArray(1 until param.size))
        else -> when (val equals = param.indexOf('='.toByte())) {
            -1 -> enableFeatureWithDefault(param)
            else -> enableFeature(param.sliceArray(0 until equals), param.sliceArray(equals + 1 until param.size))
        }
    }

    private fun ServerFeatureMap.resetFeature(name: ByteArray) = name.asFeature()?.let {
        reset(it)
        log.finer { "Reset feature ${it::class}" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ServerFeatureMap.enableFeature(name: ByteArray, value: ByteArray) {
        name.asFeature()?.let { feature ->
            set(feature, value.cast(feature.type))
            log.finer { "Set feature ${feature::class} to ${String(value)}" }
        }
    }

    private fun ServerFeatureMap.enableFeatureWithDefault(name: ByteArray) {
        name.asFeature()?.let { feature ->
            when (feature.type) {
                Boolean::class -> set(feature, true)
                else -> TODO("not implemented")
            }
        }
    }

    private fun ByteArray.asFeature() = serverFeatures[String(this)]
            ?: run {
                log.warning { "Unknown feature in 005: ${String(this)}" }
                null
            }

    private fun ByteArray.cast(to: KClass<out Any>): Any = with (String(this)) {
        when (to) {
            Int::class -> toInt()
            String::class -> this
            CaseMapping::class -> CaseMapping.fromName(this)
            ModePrefixMapping::class -> indexOf(')').let { ModePrefixMapping(substring(1 until it), substring(it + 1)) }
            else -> TODO("not implemented")
        }
    }

}