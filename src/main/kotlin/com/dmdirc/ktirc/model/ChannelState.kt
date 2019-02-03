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
     * A map of all users in the channel to their current modes.
     */
    val users = ChannelUserMap(caseMappingProvider)

}

/**
 * Describes a user in a channel, and their modes.
 */
data class ChannelUser(var nickname: String, var modes: String = "")
