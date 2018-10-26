package com.dmdirc.irctest.cases.connection

import com.dmdirc.irctest.testCase

val password = testCase("connection.password") {
    config {
        password = "This is a test"
        nick = "test"
    }

    steps {
        expect("PASS :This is a test")
    }
}
