package com.dmdirc.ktirc.model

/**
 * Describes the state of capability negotiation with the server.
 */
class CapabilitiesState {

    /** The current negotiation state. */
    var negotiationState: CapabilitiesNegotiationState = CapabilitiesNegotiationState.AWAITING_LIST
        internal set

    // TODO: These should only be mutable internally
    /** The capabilities that were advertised by the server. */
    val advertisedCapabilities = HashMap<Capability, String>()
    /** The capabilities that we have agreed to enable. */
    val enabledCapabilities = HashMap<Capability, String>()

    internal fun reset() {
        negotiationState = CapabilitiesNegotiationState.AWAITING_LIST
        advertisedCapabilities.clear()
        enabledCapabilities.clear()
    }

}

/**
 * The state of negotiations with the server.
 */
enum class CapabilitiesNegotiationState {

    /**
     * We have requested a list of capabilities and are awaiting a reply.
     */
    AWAITING_LIST,

    /**
     * We have sent a list of capabilities to enable, and are awaiting an acknowledgement.
     */
    AWAITING_ACK,

    /**
     * We are attempting to authenticate with SASL.
     */
    AUTHENTICATING,

    /**
     * Negotiation has completed.
     */
    FINISHED

}

/**
 * IRCv3 capabilities supported by the client.
 */
@Suppress("unused")
sealed class Capability(val name: String) {
    // Capabilities that introduce extra commands:
    /** Allows authentication using SASL via the AUTHENTICATE command. */
    object SaslAuthentication : Capability("sasl")

    // Capabilities that enable more information in message tags:
    /** Draft version of message tags, enables client-only tags. */
    object DraftMessageTags33 : Capability("draft/message-tags-0.2") // TODO: Add processor for TAGMSG

    /** Messages are tagged with the server time they originated at. */
    object ServerTimeMessageTag : Capability("server-time")

    /** Messages are tagged with the sender's account name. */
    object UserAccountMessageTag : Capability("account-tag")

    // Capabilities that extend existing commands to supply extra information:
    /** Hosts are included for users in NAMES messages. */
    object HostsInNamesReply : Capability("userhost-in-names")

    /** Multiple mode prefixes are returned per-user in NAMES messages. */
    object MultipleUserModePrefixes : Capability("multi-prefix")

    /** The user's account and real name are provided when they join a channel. */
    object AccountAndRealNameInJoinMessages : Capability("extended-join")

    // Capabilities that affect how messages are sent/received:
    /** Messages sent by the client are echo'd back on successful delivery. */
    object EchoMessages : Capability("echo-message")

    /** Messages can be sent in batches, and potentially handled differently by the client. */
    object Batch : Capability("batch")

    // Capabilities that notify us of changes to other clients:
    /** Receive a notification when a user's account changes. */
    object AccountChangeMessages : Capability("account-notify")

    /** Receive a notification when a user's away state changes. */
    object AwayStateMessages : Capability("away-notify") // TODO: Add processor

    /** Receive a notification when a user's host changes, instead of a quit/join. */
    object HostChangeMessages : Capability("chghost") // TODO: Add processor

}

internal val capabilities: Map<String, Capability> by lazy {
    Capability::class.nestedClasses.map { it.objectInstance as Capability }.associateBy { it.name }
}
