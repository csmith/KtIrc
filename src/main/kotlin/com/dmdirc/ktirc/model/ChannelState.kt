package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

/**
 * Describes the state of a channel that the client has joined.
 */
class ChannelState(val name: String, caseMappingProvider: () -> CaseMapping) {

    /**
     * Whether or not we are in the process of receiving a user list (which may span many messages).
     */
    var receivingUserList = false
        internal set

    /**
     * Whether or not we have discovered the full set of modes for the channel.
     */
    var modesDiscovered = false
        internal set

    /**
     * A map of all users in the channel to their current modes.
     */
    val users = ChannelUserMap(caseMappingProvider)

    /**
     * A map of modes set on the channel, and their values (if any).
     *
     * If [modesDiscovered] is false, this map may be missing modes that the server hasn't told us about.
     */
    var modes = HashMap<Char, String>()

    internal fun reset() {
        receivingUserList = false
        modesDiscovered = false
        users.clear()
        modes.clear()
    }
}

/**
 * Describes a user in a channel, and their modes.
 */
data class ChannelUser(var nickname: String, var modes: String = "")

/**
 * The types of supported channel modes, and what parameters they require.
 *
 * These must be sorted according to the order they are sent in the CHANMODES server feature.
 */
enum class ChannelModeType {
    /** The mode adds or removes an entry for a list. It must have a param to add or remove. */
    List,
    /** The mode has a parameter that must be present to set it or unset it. */
    SetUnsetParameter,
    /** The mode has a parameter that must be present to set it, but is unset without one. */
    SetParameter,
    /** The mode does not take parameters in any case. */
    NoParameter;

    /**
     * Whether this mode requires a parameter to be set or not.
     */
    val needsParameterToSet: Boolean
        get() = this != NoParameter

    /**
     * Whether this mode requires a parameter to be unset or not.
     */
    val needsParameterToUnset: Boolean
        get() = this != NoParameter && this != SetParameter

}
