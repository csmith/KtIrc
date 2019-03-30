package com.dmdirc.ktirc

import java.time.Duration

/**
 * Dsl marker for [IrcClient] dsl.
 */
@DslMarker
annotation class IrcClientDsl

/**
 * Dsl for configuring an IRC Client.
 *
 * [server] and [profile] blocks are required. The full range of configuration options are:
 *
 * ```
 * server {
 *     host = "irc.example.com"     // Required
 *     port = 6667
 *     useTls = true
 *     password = "H4ckTh3Pl4n3t"
 * }
 *
 * profile {
 *     nickname = "MyBot"           // Required
 *     username = "bot"
 *     realName = "Botomatic v1.2"
 * }
 *
 * behaviour {
 *     requestModesOnJoin = true
 *     alwaysEchoMessages = true
 *     preferIPv6 = false
 * }
 *
 * sasl {
 *     mechanisms += "PLAIN" // or to set the list from scratch:
 *     mechanisms("PLAIN")
 *
 *     username = "botaccount"
 *     password = "s3cur3"
 * }
 * ```
 */
@IrcClientDsl
class IrcClientConfigBuilder {

    private var server: ServerConfig? = null
        set(value) {
            check(field == null) { "server may only be specified once" }
            check(!value?.host.isNullOrEmpty()) { "server.host must be specified" }
            field = value
        }

    private var profile: ProfileConfig? = null
        set(value) {
            check(field == null) { "profile may only be specified once" }
            check(!value?.nickname.isNullOrEmpty()) { "profile.nickname must be specified" }
            field = value
        }

    private var behaviour: BehaviourConfig? = null
        set(value) {
            check(field == null) { "behaviour may only be specified once" }
            field = value
        }

    private var sasl: SaslConfig? = null
        set(value) {
            check(field == null) { "sasl may only be specified once" }
            field = value
        }

    /**
     * Configures the server that the IrcClient will connect to.
     *
     * See [ServerConfig] for details of each parameter.
     *
     * @param block Optional additional configuration to apply to the [ServerConfig]
     */
    @IrcClientDsl
    fun server(host: String? = null, port: Int? = null, useTls: Boolean? = null, password: String? = null, block: (ServerConfig.() -> Unit)? = null) {
        server = ServerConfig().apply {
            host?.let { this.host = it }
            port?.let { this.port = it }
            useTls?.let { this.useTls = it }
            password?.let { this.password = it }
            block?.let { apply(it) }
        }
    }

    /**
     * Configures the profile of the IrcClient user.
     *
     * See [ProfileConfig] for details of each parameter.
     *
     * @param block Optional additional configuration to apply to the [ProfileConfig]
     */
    @IrcClientDsl
    fun profile(nickname: String? = null, username: String? = null, realName: String? = null, block: (ProfileConfig.() -> Unit)? = null) {
        profile = ProfileConfig().apply {
            nickname?.let { this.nickname = it }
            username?.let { this.username = it }
            realName?.let { this.realName = it }
            block?.let { apply(it) }
        }
    }

    /**
     * Configures the behaviour of the client (optional).
     */
    @IrcClientDsl
    fun behaviour(block: BehaviourConfig.() -> Unit) {
        behaviour = BehaviourConfig().apply(block)
    }

    /**
     * Configures SASL authentication (optional).
     */
    @IrcClientDsl
    fun sasl(block: SaslConfig.() -> Unit) {
        sasl = SaslConfig().apply(block)
    }

    internal fun build() =
            IrcClientConfig(
                    checkNotNull(server) { "Server must be specified " },
                    checkNotNull(profile) { "Profile must be specified" },
                    behaviour ?: BehaviourConfig(),
                    sasl)

}

/**
 * Dsl for configuring a server.
 */
@IrcClientDsl
class ServerConfig {
    /** The hostname (or IP address) of the server to connect to. */
    var host: String = ""
    /** The port to connect on. Defaults to 6697. */
    var port: Int = 6697
    /** Whether or not to use TLS (an encrypted connection). */
    var useTls: Boolean = true
    /** The password required to connect to the server, if any. */
    var password: String? = null
}

/**
 * Dsl for configuring a profile.
 */
@IrcClientDsl
class ProfileConfig {
    /** The initial nickname to use when connecting. */
    var nickname: String = ""
    /** The username (used in place of an ident response) to provide to the server. */
    var username: String = "KtIrc"
    /** The "real name" to provide to the server. */
    var realName: String = "KtIrc User"
}

/**
 * Dsl for configuring SASL authentication.
 *
 * By default the `PLAIN`, `SCRAM-SHA-1`, and `SCRAM-SHA-256` methods will be enabled if SASL is configured.
 *
 * You can modify the mechanisms either by editing the [mechanisms] collection:
 *
 * ```
 * mechanisms += "EXTERNAL"
 * mechanisms.remove("PLAIN")
 * ```
 *
 * or by calling the [mechanisms] function with all the mechanisms you wish
 * to enable:
 *
 * ```
 * mechanisms("PLAIN", "EXTERNAL")
 * ```
 *
 * Priority of mechanisms is determined by KtIrc, regardless of the order
 * they are specified in here.
 */
@IrcClientDsl
class SaslConfig {
    /** The SASL mechanisms to enable. */
    val mechanisms: MutableCollection<String> = mutableSetOf("PLAIN", "SCRAM-SHA-1", "SCRAM-SHA-256")
    /** The username to provide when authenticating using SASL. */
    var username: String = ""
    /** The username to provide when authenticating using SASL. */
    var password: String = ""

    /**
     * Replaces all enabled SASL mechanisms with the given ones.
     */
    @IrcClientDsl
    fun mechanisms(vararg methods: String) {
        with(this.mechanisms) {
            clear()
            addAll(methods)
        }
    }
}

@IrcClientDsl
class PingConfig {

    /**
     * The period of time we will wait between sending pings to the server.
     *
     * If [incomingLinesResetTimer] is enabled, then a ping will be sent this period of time after the last line
     * is received from the server. Otherwise, it will be sent this period of time after the last PONG response.
     */
    var sendPeriod: Duration? = null

    /**
     * The period of time to wait for a reply.
     *
     * If the server does not respond to a PING in this period, we consider it stoned and disconnect.
     */
    var responseGracePeriod: Duration? = null

    /**
     * Whether to treat incoming lines from the server as an indication that it is still active.
     *
     * This reduces the amount of pings that KtIrc will send, but can result in KtIrc staying connected even if the
     * server is severely lagged or ignores all lines sent to it (for example).
     */
    var incomingLinesResetTimer: Boolean = false

}

/**
 * Dsl for configuring the behaviour of an [IrcClient].
 */
@IrcClientDsl
class BehaviourConfig : ClientBehaviour {

    override var requestModesOnJoin = false
    override var alwaysEchoMessages = false
    override var preferIPv6 = true

    override var pingTimeouts: PingTimeouts? = null
        internal set(value) {
            check(field == null) { "ping timeouts may only be specified once" }
            field = value
        }

    /**
     * Configures how frequently KtIrc will send pings, and how it will deal with non-responsive servers.
     *
     * If not specified, KtIrc will not send pings.
     *
     * See [PingConfig].
     */
    @IrcClientDsl
    fun sendPings(block: PingConfig.() -> Unit) {
        val config = PingConfig().apply(block)
        requireNotNull(config.sendPeriod) { "send period must be specified" }
        requireNotNull(config.responseGracePeriod) { "response grace period must be specified" }
        pingTimeouts = IrcPingTimeouts(config.sendPeriod!!, config.responseGracePeriod!!, config.incomingLinesResetTimer)
    }

}
