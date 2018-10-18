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

@Suppress("unused")
sealed class Capability(val name: String) {
    // Capabilities that enable more information in message tags:
    object ServerTimeMessageTag : Capability("server-time")
    object UserAccountMessageTag : Capability("account-tag")

    // Capabilities that extend existing commands to supply extra information:
    object HostsInNamesReply : Capability("userhost-in-names")
    object MultipleUserModePrefixes : Capability("multi-prefix")
    object AccountAndRealNameInJoinMessages : Capability("extended-join")

    // Capabilities that affect how messages are sent/received:
    object EchoMessages : Capability("echo-message")

    // Capabilities that notify us of changes to other clients:
    object AccountChangeMessages : Capability("account-notify") // TODO: Add processor
    object AwayStateMessages : Capability("away-notify") // TODO: Add processor
    object HostChangeMessages : Capability("chghost") // TODO: Add processor
}

val capabilities: Map<String, Capability> by lazy {
    Capability::class.nestedClasses.map { it.objectInstance as Capability }.associateBy { it.name }
}
