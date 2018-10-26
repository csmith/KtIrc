package com.dmdirc.irctest.cases.connection.capabilities.v32

import com.dmdirc.irctest.testCase

val serverTime = testCase("connection.capabilities.302.server-time") {
    steps {
        expect("CAP LS 302")
        send("CAP * LS :server-time")
        expect("CAP REQ :server-time")
        send("CAP * ACK :server-time")
        expect("CAP END")
    }
}
