package com.dmdirc.irctest

@DslMarker
annotation class TestDslMarker

fun testCase(name: String, data: TestCaseContext.() -> Unit) = TestCaseContext(name).apply(data)

@TestDslMarker
class TestCaseContext(val name: String) {

    internal val config = ConfigContext()
    internal val steps = StepsContext()

    fun config(data: ConfigContext.() -> Unit) = config.data()
    fun steps(data: StepsContext.() -> Unit) = steps.data()

}

@TestDslMarker
class ConfigContext {

    var nick = "nick"
    var user = "ident"
    var realName = "some name"
    var password: String? = null

}

@TestDslMarker
class StepsContext {

    private val steps = mutableListOf<Step>()

    fun send(line: String) { steps += SendStep(line) }
    fun expect(line: String) { steps += SimpleExpectStep(line) }

    fun expect(data: ExpectContext.() -> Unit) {
        ExpectContext().data()
    }

    operator fun iterator() = steps.iterator()

}

open class Step
class SendStep(val line: String) : Step()
class SimpleExpectStep(val line: String): Step()

@TestDslMarker
class ExpectContext {

    private var commandMatcher: ((String) -> Boolean)? = null
    private var paramMatchers = HashMap<Int, (String) -> Boolean>()

    val command: String = "COMMAND"
    val parameters = ParameterProvider()

    infix fun String.toMatch(regex: String) {
        val matcher = { it: String -> it.matches(regex.toRegex()) }
        when (this) {
            command -> commandMatcher = matcher
            else -> paramMatchers[this.toInt()] = matcher
        }
    }

    infix fun String.toEqual(value: String) {
        val matcher = { it: String -> it == value }
        when (this) {
            command -> commandMatcher = matcher
            else -> paramMatchers[this.toInt()] = matcher
        }
    }
}

class ParameterProvider {
    val count = "count"
    operator fun get(index: Int) = "$index"
}

