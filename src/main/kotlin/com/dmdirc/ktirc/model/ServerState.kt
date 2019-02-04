package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import kotlin.reflect.KClass

/**
 * Contains the current state of a single IRC server.
 */
class ServerState internal constructor(initialNickname: String, initialServerName: String) {

    /** Whether we've received the 'Welcome to IRC' (001) message. */
    internal var receivedWelcome = false

    /** The current status of the server. */
    var status = ServerStatus.Connecting
        internal set

    /** Our present nickname on the server. */
    var localNickname: String = initialNickname
        internal set

    /** The server's name, as reported by it. */
    var serverName: String = initialServerName
        internal set

    /** The features that the server has declared it supports (from the 005 header). */
    val features = ServerFeatureMap()

    /** The capabilities we have negotiated with the server (from IRCv3). */
    val capabilities = CapabilitiesState()

}

/**
 * Maps known features onto their values, enforcing type safety.
 */
class ServerFeatureMap {

    private val features = HashMap<ServerFeature<*>, Any?>()

    /**
     * Gets the value, or the default value, of the given feature.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(feature: ServerFeature<T>) = features.getOrDefault(feature, feature.default) as? T? ?: feature.default

    internal operator fun set(feature: ServerFeature<*>, value: Any) {
        require(feature.type.isInstance(value))
        features[feature] = value
    }

    internal fun setAll(featureMap: ServerFeatureMap) = featureMap.features.forEach { feature, value -> features[feature] = value }
    internal fun reset(feature: ServerFeature<*>) = features.put(feature, null)

}

/**
 * Stores the mapping of mode prefixes received from the server.
 */
data class ModePrefixMapping(val modes: String, val prefixes: String) {

    /** Determines whether the given character is a mode prefix (e.g. "@", "+"). */
    fun isPrefix(char: Char) = prefixes.contains(char)
    /** Gets the mode corresponding to the given prefix (e.g. "@" -> "o"). */
    fun getMode(prefix: Char) = modes[prefixes.indexOf(prefix)]
    /** Gets the modes corresponding to the given prefixes (e.g. "@+" -> "ov"). */
    fun getModes(prefixes: String) = String(prefixes.map(this::getMode).toCharArray())

}

/**
 * Describes a server feature determined from the 005 response.
 */
sealed class ServerFeature<T : Any>(val name: String, val type: KClass<T>, val default: T? = null) {
    /** The network the server says it belongs to. */
    object Network : ServerFeature<String>("NETWORK", String::class)
    /** The case mapping the server uses, defaulting to RFC. */
    object ServerCaseMapping : ServerFeature<CaseMapping>("CASEMAPPING", CaseMapping::class, CaseMapping.Rfc)
    /** The mode prefixes the server uses, defaulting to ov/@+. */
    object ModePrefixes : ServerFeature<ModePrefixMapping>("PREFIX", ModePrefixMapping::class, ModePrefixMapping("ov", "@+"))
    /** The maximum number of channels a client may join. */
    object MaximumChannels : ServerFeature<Int>("MAXCHANNELS", Int::class) // TODO: CHANLIMIT also exists
    /** The modes supported in channels. */
    object ChannelModes : ServerFeature<String>("CHANMODES", String::class)
    /** The maximum length of a channel name, defaulting to 200. */
    object MaximumChannelNameLength : ServerFeature<Int>("CHANNELLEN", Int::class, 200)
    /** Whether or not the server supports extended who. */
    object WhoxSupport : ServerFeature<Boolean>("WHOX", Boolean::class, false)
}

internal val serverFeatures: Map<String, ServerFeature<*>> by lazy {
    ServerFeature::class.nestedClasses.map { it.objectInstance as ServerFeature<*> }.associateBy { it.name }
}

/**
 * Enumeration of the possible states of a server.
 */
enum class ServerStatus {
    /** We are attempting to connect to the server. It is not yet ready for use. */
    Connecting,
    /** We are logging in, dealing with capabilities, etc. The server is not yet ready for use. */
    Negotiating,
    /** We are connected and commands can be sent. */
    Ready,
}
