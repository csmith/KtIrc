package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping

class ChannelState(val name: String, caseMappingProvider: () -> CaseMapping) {

    var receivingUserList = false
    val users = ChannelUserMap(caseMappingProvider)

}

data class ChannelUser(var nickname: String, var modes: String = "")
