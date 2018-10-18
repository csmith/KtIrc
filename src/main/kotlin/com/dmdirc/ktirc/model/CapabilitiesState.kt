package com.dmdirc.ktirc.model

class CapabilitiesState {

    var negotiationState: CapabilitiesNegotiationState = CapabilitiesNegotiationState.AWAITING_LIST

    val advertisedCapabilities = HashMap<Capability, String>()
    val enabledCapabilities = HashMap<Capability, String>()

}

enum class CapabilitiesNegotiationState {

    AWAITING_LIST,
    AWAITING_ACK,
    FINISHED

}

sealed class Capability(val name: String) {
    // Capabilities that enable more information in message tags:
    object ServerTimeMessageTag : Capability("server-time") // TODO: Parse this and expose time in events
    object UserAccountMessageTag : Capability("account-tag") // TODO: Add accounts to user info

    // Capabilities that extend existing commands to supply extra information:
    object HostsInNamesReply : Capability("userhost-in-names") // TODO: Parse these hosts
    object MultipleUserModePrefixes : Capability("multi-prefix")
    object AccountAndRealNameInJoinMessages : Capability("extended-join") // TODO: Parse this

    // Capabilities that affect how messages are sent/received:
    object EchoMessages : Capability("echo-message")

    // Capabilities that notify us of changes to other clients:
    object AccountChangeMessages : Capability("account-notify")
    object AwayStateMessages : Capability("away-notify")
    object HostChangeMessages : Capability("chghost")
}

val capabilities: Map<String, Capability> by lazy {
    Capability::class.nestedClasses.map { it.objectInstance as Capability }.associateBy { it.name }
}
