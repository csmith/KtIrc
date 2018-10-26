package com.dmdirc.ktirc

import com.dmdirc.irctest.IrcLibraryTests
import com.dmdirc.ktirc.model.Profile
import com.dmdirc.ktirc.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.TestFactory

class KtIrcIntegrationTest {

    @TestFactory
    fun dynamicTests() = IrcLibraryTests().getTests(object : IrcLibraryTests.IrcLibrary {

        private lateinit var ircClient : IrcClientImpl

        override fun connect(nick: String, ident: String, realName: String, password: String?) {
            ircClient = IrcClientImpl(Server("localhost", 12321, password = password), Profile(nick, ident, realName))
            GlobalScope.launch(Dispatchers.IO) {
                ircClient.connect()
            }
        }

        override fun terminate() {
            ircClient.disconnect()
        }

    })

}