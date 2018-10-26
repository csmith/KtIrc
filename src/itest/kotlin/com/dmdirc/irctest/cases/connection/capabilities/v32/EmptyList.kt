package com.dmdirc.irctest.cases.connection.capabilities.v32

import com.dmdirc.irctest.testCase

val emptyList = testCase("connection.capabilities.302.empty-list") {
    steps {
        expect("CAP LS 302")
        send("CAP * LS :")
        expect("CAP END")
    }
}
