package com.dmdirc.ktirc

import com.dmdirc.irctest.IrcLibraryTests
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestFactory

class KtIrcIntegrationTest {

    @TestFactory
    fun dynamicTests() = IrcLibraryTests().getTests(object : IrcLibraryTests.IrcLibrary {

        private lateinit var ircClient : IrcClient

        override fun connect(nick: String, ident: String, realName: String, password: String?) {
            ircClient = IrcClient {
                server {
                    host = "localhost"
                    port = 12321
                    this.password = password
                }
                profile {
                    nickname = nick
                    username = ident
                    this.realName = realName
                }
            }
            ircClient.connect()
        }

        override fun terminate() {
            runBlocking {
                ircClient.disconnect()
            }
        }

    })

}