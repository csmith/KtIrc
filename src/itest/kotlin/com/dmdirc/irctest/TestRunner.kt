package com.dmdirc.irctest

import com.dmdirc.irctest.cases.testCases
import org.junit.jupiter.api.DynamicTest
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket

class IrcLibraryTests {

    interface IrcLibrary {
        fun connect(nick: String, ident: String, realName: String, password: String?)
        fun terminate()
    }

    fun getTests(library: IrcLibrary, names: List<TestCaseContext> = testCases) = names.map { getTest(library, it) }

    fun getTest(library: IrcLibrary, test: TestCaseContext): DynamicTest = DynamicTest.dynamicTest(test.name) {
        ServerSocket(12321).use { serverSocket ->
            library.connect(
                    test.config.nick,
                    test.config.user,
                    test.config.realName,
                    test.config.password
            )

            val clientSocket = serverSocket.accept()
            val clientInput = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val clientOutput = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
            for (step in test.steps) {
                when (step) {
                    is SimpleExpectStep -> {
                        while (true) {
                            val read = clientInput.readLine()
                            if (read == step.line) {
                                println("     MATCH: $read")
                                break
                            } else {
                                println("UNEXPECTED: $read")
                            }
                        }
                    }
                    is SendStep -> {
                        println("      SENT: ${step.line}")
                        clientOutput.write("${step.line}\r\n")
                        clientOutput.flush()
                    }
                }
            }
        }
    }

}