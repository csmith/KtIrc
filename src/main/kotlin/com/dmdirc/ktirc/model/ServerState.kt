package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import kotlin.reflect.KClass

class ServerState(initialNickname: String) {

    var localNickname: String = initialNickname
    val features = ServerFeatureMap()

}

class ServerFeatureMap {

    private val features = HashMap<ServerFeature<*>, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(feature: ServerFeature<T>) = features.getOrDefault(feature, feature.default) as? T? ?: feature.default

    operator fun set(feature: ServerFeature<*>, value: Any) {
        require(feature.type.isInstance(value))
        features[feature] = value
    }

    fun setAll(featureMap: ServerFeatureMap) = featureMap.features.forEach { feature, value -> features[feature] = value }
    fun reset(feature: ServerFeature<*>) = features.put(feature, null)

}

data class ModePrefixMapping(val modes: String, val prefixes: String) {

    fun isPrefix(char: Char) = prefixes.contains(char)
    fun getMode(prefix: Char) = modes[prefixes.indexOf(prefix)]
    fun getModes(prefixes: String) = String(prefixes.map(this::getMode).toCharArray())

}

sealed class ServerFeature<T : Any>(val name: String, val type: KClass<T>, val default: T? = null) {
    object ServerCaseMapping : ServerFeature<CaseMapping>("CASEMAPPING", CaseMapping::class, CaseMapping.Rfc)
    object ModePrefixes : ServerFeature<ModePrefixMapping>("PREFIX", ModePrefixMapping::class, ModePrefixMapping("ov", "@+"))
    object MaximumChannels : ServerFeature<Int>("CHANLIMIT", Int::class)
    object ChannelModes : ServerFeature<String>("CHANMODES", String::class)
    object MaximumChannelNameLength : ServerFeature<Int>("CHANNELLEN", Int::class, 200)
    object WhoxSupport : ServerFeature<Boolean>("WHOX", Boolean::class, false)
}

val serverFeatures: Map<String, ServerFeature<*>> by lazy {
    ServerFeature::class.nestedClasses.map { it.objectInstance as ServerFeature<*> }.associateBy { it.name }
}