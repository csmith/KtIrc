package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.util.logger
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

/**
 * Contains the current state of a single IRC server.
 */
class ServerState internal constructor(
        private val initialNickname: String,
        private val initialServerName: String,
        saslConfig: SaslConfig? = null) {

    private val log by logger()

    /** Whether we've received the 'Welcome to IRC' (001) message. */
    internal var receivedWelcome = false

    /** The current status of the server. */
    var status = ServerStatus.Disconnected
        internal set

    /**
     * What we believe our current nickname to be on the server.
     *
     * Initially this will be the nickname provided in the config. It will be updated to the actual nickname
     * in use when connecting. Once you have received a [com.dmdirc.ktirc.events.ServerWelcome] event you can
     * rely on this value being current.
     * */
    var localNickname: String = initialNickname
        internal set

    /**
     * The name of the server we are connected to.
     *
     * Initially this will be the hostname or IP address provided in the config. It will be updated to the server's
     * self-reported hostname when connecting. Once you have received a [com.dmdirc.ktirc.events.ServerWelcome] event
     * you can rely on this value being current.
     */
    var serverName: String = initialServerName
        internal set

    /** The features that the server has declared it supports (from the 005 header). */
    val features = ServerFeatureMap()

    /** The capabilities we have negotiated with the server (from IRCv3). */
    val capabilities = CapabilitiesState()

    /** The current state of SASL authentication. */
    internal val sasl = SaslState(saslConfig)

    /**
     * Convenience accessor for the [ServerFeature.ModePrefixes] feature, which will always have a value.
     */
    val channelModePrefixes
        get() = features[ServerFeature.ModePrefixes] ?: throw IllegalStateException("lost mode prefixes")

    /**
     * Convenience accessor for the [ServerFeature.ChannelTypes] feature, which will always have a value.
     */
    val channelTypes
        get() = features[ServerFeature.ChannelTypes] ?: throw IllegalStateException("lost channel types")

    /**
     * Batches that are currently in progress.
     */
    internal val batches = mutableMapOf<String, Batch>()

    /**
     * Counter for ensuring sent labels are unique.
     */
    internal val labelCounter = AtomicLong(0)

    /**
     * Whether or not the server supports labeled responses.
     */
    internal val supportsLabeledResponses: Boolean
        get() = Capability.LabeledResponse in capabilities.enabledCapabilities

    /**
     * Channels waiting for a label to be received.
     */
    internal val labelChannels = mutableMapOf<String, SendChannel<IrcEvent>>()

    /**
     * Determines if the given mode is one applied to a user of a channel, such as 'o' for operator.
     */
    fun isChannelUserMode(mode: Char) = channelModePrefixes.isMode(mode)

    /**
     * Determines what type of channel mode the given character is, based on the server features.
     *
     * If the mode isn't found, or the server hasn't provided modes, it is presumed to be [ChannelModeType.NoParameter].
     */
    fun channelModeType(mode: Char): ChannelModeType {
        features[ServerFeature.ChannelModes]?.forEachIndexed { index, modes ->
            if (mode in modes) {
                return ChannelModeType.values()[index]
            }
        }

        log.warning { "Unknown channel mode $mode, assuming it's boolean" }
        return ChannelModeType.NoParameter
    }

    internal fun reset() {
        receivedWelcome = false
        status = ServerStatus.Disconnected
        localNickname = initialNickname
        serverName = initialServerName
        features.clear()
        capabilities.reset()
        sasl.reset()
        batches.clear()
        labelCounter.set(0)
        labelChannels.clear()
    }

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
    operator fun <T : Any> get(feature: ServerFeature<T>) = features.getOrDefault(feature, feature.default) as? T?
            ?: feature.default

    internal operator fun set(feature: ServerFeature<*>, value: Any) {
        require(feature.type.isInstance(value)) {
            "Value given for feature ${feature::class} must be type ${feature.type} but was ${value::class}"
        }

        features[feature] = value
    }

    internal fun setAll(featureMap: ServerFeatureMap) = featureMap.features.forEach { feature, value -> features[feature] = value }
    internal fun reset(feature: ServerFeature<*>) = features.put(feature, null)
    internal fun clear() = features.clear()
    internal fun isEmpty() = features.isEmpty()

}

/**
 * Stores the mapping of mode prefixes received from the server.
 */
data class ModePrefixMapping(val modes: String, val prefixes: String) {

    /** Determines whether the given character is a mode prefix (e.g. "@", "+"). */
    fun isPrefix(char: Char) = prefixes.contains(char)

    /** Determines whether the given character is a channel user mode (e.g. "o", "v"). */
    fun isMode(char: Char) = modes.contains(char)

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
    object ChannelModes : ServerFeature<Array<String>>("CHANMODES", Array<String>::class)

    /** The types of channels supported. */
    object ChannelTypes : ServerFeature<String>("CHANTYPES", String::class, "#&")

    /** The maximum length of a channel name, defaulting to 200. */
    object MaximumChannelNameLength : ServerFeature<Int>("CHANNELLEN", Int::class, 200)

    /** Whether or not the server supports extended who. */
    object WhoxSupport : ServerFeature<Boolean>("WHOX", Boolean::class, false)
}

/**
 * Enumeration of the possible states of a server.
 */
enum class ServerStatus {
    /** The server is not connected. */
    Disconnected,
    /** We are attempting to connect to the server. It is not yet ready for use. */
    Connecting,
    /** We are logging in, dealing with capabilities, etc. The server is not yet ready for use. */
    Negotiating,
    /** We are connected and commands can be sent. */
    Ready,
}

/**
 * Represents an in-progress batch.
 */
internal data class Batch(val type: String, val arguments: List<String>, val metadata: EventMetadata, val events: MutableList<IrcEvent> = mutableListOf())

internal val serverFeatures: Map<String, ServerFeature<*>> by lazy {
    ServerFeature::class.nestedClasses.map { it.objectInstance as ServerFeature<*> }.associateBy { it.name }
}
