package com.dmdirc.ktirc

/**
 * Dsl marker for [IrcClient] dsl.
 */
@DslMarker
annotation class IrcClientDsl

internal data class IrcClientConfig(val server: ServerConfig, val profile: ProfileConfig, val sasl: SaslConfig?)

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
 *     username = "bot
 *     realName = "Botomatic v1.2"
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
    private var profile: ProfileConfig? = null
    private var sasl: SaslConfig? = null

    /**
     * Configures the server that the IrcClient will connect to.
     *
     * At a minimum, [ServerConfig.host] must be supplied.
     */
    @IrcClientDsl
    fun server(block: ServerConfig.() -> Unit) {
        check(server == null) { "server may only be specified once" }
        server = ServerConfig().apply(block).also { check(it.host.isNotEmpty()) { "server.host must be specified" } }
    }

    /**
     * Configures the profile of the IrcClient user.
     *
     * At a minimum, [ProfileConfig.nickname] must be supplied.
     */
    @IrcClientDsl
    fun profile(block: ProfileConfig.() -> Unit) {
        check(profile == null) { "profile may only be specified once" }
        profile = ProfileConfig().apply(block).also { check(it.nickname.isNotEmpty()) { "profile.nickname must be specified" } }
    }

    /**
     * Configures SASL authentication (optional).
     */
    @IrcClientDsl
    fun sasl(block: SaslConfig.() -> Unit) {
        check(sasl == null) { "sasl may only be specified once" }
        sasl = SaslConfig().apply(block)
    }

    internal fun build() =
            IrcClientConfig(
                    checkNotNull(server) { "Server must be specified " },
                    checkNotNull(profile) { "Profile must be specified" },
                    sasl)

}

/**
 * Dsl for configuring a server.
 */
@IrcClientDsl
class ServerConfig {
    /** The hostname (or IP address) of the server to connect to. */
    var host: String = ""
    /** The port to connect on. Defaults to 6667. */
    var port: Int = 6667
    /** Whether or not to use TLS (an encrypted connection). */
    var useTls: Boolean = false
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

    @IrcClientDsl
    fun mechanisms(vararg methods: String) {
        with (this.mechanisms) {
            clear()
            addAll(methods)
        }
    }
}
