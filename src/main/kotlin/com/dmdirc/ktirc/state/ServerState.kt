package com.dmdirc.ktirc.state

import com.dmdirc.ktirc.io.CaseMapping
import kotlin.reflect.KClass

interface ServerState {

    var localNickname: String

    fun <T : Any> getFeature(feature: ServerFeature<T>): T?
    fun setFeature(feature: ServerFeature<*>, value: Any)
    fun resetFeature(feature: ServerFeature<*>): Any?

}

class IrcServerState(initialNickname: String) : ServerState {

    override var localNickname: String = initialNickname

    private val features = HashMap<ServerFeature<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getFeature(feature: ServerFeature<T>) = features.getOrDefault(feature, feature.default) as? T?

    override fun setFeature(feature: ServerFeature<*>, value: Any) {
        require(feature.type.isInstance(value))
        features[feature] = value
    }

    override fun resetFeature(feature: ServerFeature<*>) = features.remove(feature)

}


sealed class ServerFeature<T : Any>(val name: String, val type: KClass<T>, val default: T? = null) {
    object ServerCaseMapping : ServerFeature<CaseMapping>("CASEMAPPING", CaseMapping::class, CaseMapping.Rfc)
    object MaximumChannels : ServerFeature<Int>("CHANLIMIT", Int::class)
    object ChannelModes : ServerFeature<String>("CHANMODES", String::class)
    object MaximumChannelNameLength : ServerFeature<Int>("CHANNELLEN", Int::class, 200)
}

val serverFeatures: Map<String, ServerFeature<*>> by lazy {
    ServerFeature::class.nestedClasses.map { it.objectInstance as ServerFeature<*> }.associateBy { it.name }
}