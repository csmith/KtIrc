package com.dmdirc.ktirc

import com.dmdirc.irctest.IrcLibraryTests
import com.dmdirc.ktirc.model.Profile
import com.dmdirc.ktirc.model.Server
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestFactory

class KtIrcIntegrationTest {

    @TestFactory
    fun dynamicTests() = IrcLibraryTests().getTests(object : IrcLibraryTests.IrcLibrary {

        private lateinit var ircClient : IrcClientImpl

        override fun connect(nick: String, ident: String, realName: String, password: String?) {
            ircClient = IrcClientImpl(Server("localhost", 12321, password = password), Profile(nick, ident, realName))
            ircClient.connect()
        }

        override fun terminate() {
            runBlocking {
                delay(100)
                ircClient.disconnect()
                ircClient.join()
            }
        }

    })

}