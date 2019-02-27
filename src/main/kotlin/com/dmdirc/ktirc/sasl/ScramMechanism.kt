package com.dmdirc.ktirc.sasl

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.SaslConfig
import com.dmdirc.ktirc.messages.sendAuthenticationMessage
import com.dmdirc.ktirc.util.logger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.random.asKotlinRandom

internal class ScramMechanism(private val algorithm: String, override val priority: Int, private val saslConfig: SaslConfig) : SaslMechanism {

    private val log by logger()

    override val ircName = "SCRAM-${algorithm.toUpperCase()}"

    override fun handleAuthenticationEvent(client: IrcClient, data: ByteArray?) {
        val state = client.scramState
        try {
            when (state.scramStage) {
                ScramStage.SendingFirstMessage -> client.sendFirstMessage(state)
                ScramStage.SendingSecondMessage -> client.sendSecondMessage(state, data.parse())
                ScramStage.Finishing -> client.validateAndFinish(state, data.parse())
            }
        } catch (ex: ScramException) {
            client.abortScram(ex.localizedMessage)
        }
    }

    private fun IrcClient.sendFirstMessage(state: ScramState) {
        state.scramStage = ScramStage.SendingSecondMessage
        sendScramMessage(
                "n,,", // No channel binding, no impersonation
                ScramMessageType.AuthName to saslConfig.username.escape(),
                ScramMessageType.Nonce to state.clientNonce)
    }

    private fun IrcClient.sendSecondMessage(state: ScramState, data: Map<ScramMessageType, String>) {
        if (ScramMessageType.FutureExtensions in data)
            throw ScramException("Unsupported extension received: ${data[ScramMessageType.FutureExtensions]}")
        if (ScramMessageType.Error in data)
            throw ScramException("Error received from server: ${data[ScramMessageType.Error]}")

        state.iterCount = data[ScramMessageType.IterationCount]?.toIntOrNull()
                ?: throw ScramException("No iteration count provided")
        state.salt = data[ScramMessageType.Salt]?.fromBase64() ?: throw ScramException("No salt provided")
        state.serverNonce = data[ScramMessageType.Nonce] ?: throw ScramException("No server salt provided")

        state.saltedPassword = pbkdf2(saslConfig.password.toByteArray(), state.salt, state.iterCount)
        val clientKey = hmac(state.saltedPassword, "Client Key".toByteArray())
        val storedKey = hash(clientKey)
        state.authMessage = buildScramMessage(
                ScramMessageType.AuthName to saslConfig.username.escape(),
                ScramMessageType.Nonce to state.clientNonce,
                ScramMessageType.Nonce to state.serverNonce,
                ScramMessageType.Salt to state.salt.toBase64(),
                ScramMessageType.IterationCount to state.iterCount.toString(),
                ScramMessageType.ChannelBinding to "n,,".toByteArray().toBase64(),
                ScramMessageType.Nonce to state.serverNonce).toByteArray()
        val clientSignature = hmac(storedKey, state.authMessage)
        val clientProof = clientKey.xor(clientSignature)

        state.scramStage = ScramStage.Finishing
        sendScramMessage(
                "",
                ScramMessageType.ChannelBinding to "n,,".toByteArray().toBase64(),
                ScramMessageType.Nonce to state.serverNonce,
                ScramMessageType.ClientProof to clientProof.toBase64()
        )
    }

    private fun IrcClient.validateAndFinish(state: ScramState, data: Map<ScramMessageType, String>) {
        if (ScramMessageType.FutureExtensions in data)
            throw ScramException("Unsupported extension received: ${data[ScramMessageType.FutureExtensions]}")
        if (ScramMessageType.Error in data)
            throw ScramException("Error received from server: ${data[ScramMessageType.Error]}")

        val serverKey = hmac(state.saltedPassword, "Server Key".toByteArray())
        val expectedServerSignature = hmac(serverKey, state.authMessage).toBase64()
        val receivedServerSignature = data[ScramMessageType.ServerVerifier]
                ?: throw ScramException("No server verifier received")

        if (expectedServerSignature != receivedServerSignature) {
            throw ScramException("Server signature does not match")
        }
        sendAuthenticationMessage("+")
    }

    private fun IrcClient.abortScram(reason: String) {
        log.warning { "Aborting SCRAM authentication: $reason" }
        sendAuthenticationMessage("*")
    }

    private fun IrcClient.sendScramMessage(prefix: String = "", vararg entries: Pair<ScramMessageType, String>) =
            sendAuthenticationData("$prefix${buildScramMessage(*entries)}")

    private fun buildScramMessage(vararg entries: Pair<ScramMessageType, String>) = entries.joinToString(",") { (k, v) -> "${k.prefix}=$v" }

    private fun ByteArray?.parse(): Map<ScramMessageType, String> {
        return if (this == null || this.isEmpty())
            emptyMap()
        else
            String(this).split(',').map {
                getMessageType(it[0]) to it.substring(2).unescape()
            }.toMap()
    }

    private fun String.escape() = replace("=", "=3D").replace(",", "=2C")
    private fun String.unescape() = replace("=2C", ",").replace("=3D", "=")

    private fun hmac(keyMaterial: ByteArray, input: ByteArray): ByteArray {
        return with(Mac.getInstance("hmac${algorithm.replace("-", "")}")) {
            init(SecretKeySpec(keyMaterial, algorithm))
            doFinal(input)
        }
    }

    private fun hash(input: ByteArray): ByteArray {
        return with(MessageDigest.getInstance(algorithm.replace("-", ""))) {
            digest(input)
        }
    }

    private fun pbkdf2(keyMaterial: ByteArray, initialSalt: ByteArray, iterations: Int): ByteArray {
        var salt = initialSalt + 0x00 + 0x00 + 0x00 + 0x01
        var result: ByteArray? = null
        repeat(iterations) {
            salt = hmac(keyMaterial, salt)
            result = result?.xor(salt) ?: salt
        }
        return result ?: ByteArray(0)
    }

    private val IrcClient.scramState: ScramState
        get() = with(serverState.sasl) {
            (mechanismState as? ScramState ?: com.dmdirc.ktirc.sasl.ScramState()).apply {
                mechanismState = this
            }
        }

    private fun ByteArray.xor(other: ByteArray): ByteArray = zip(other) { a, b -> a.xor(b) }.toByteArray()

}

private class ScramException(message: String) : RuntimeException(message)

private fun newNonce(): String {
    val charPool: List<Char> = (' '..'~') - ',' - '='
    val random = SecureRandom.getInstanceStrong().asKotlinRandom()
    return (0..31).map { charPool.random(random) }.joinToString("")
}

internal class ScramState(
        var scramStage: ScramStage = ScramStage.SendingFirstMessage,
        val clientNonce: String = newNonce(),
        var serverNonce: String = "",
        var iterCount: Int = 1,
        var salt: ByteArray = ByteArray(0),
        var saltedPassword: ByteArray = ByteArray(0),
        var authMessage: ByteArray = ByteArray(0))

internal enum class ScramStage {
    SendingFirstMessage,
    SendingSecondMessage,
    Finishing
}

internal enum class ScramMessageType(val prefix: Char) {
    AuthName('n'),
    FutureExtensions('m'),
    Nonce('r'),
    ChannelBinding('c'),
    Salt('s'),
    IterationCount('i'),
    ClientProof('p'),
    ServerVerifier('v'),
    Error('e'),
}

private fun getMessageType(prefix: Char) =
        ScramMessageType.values().firstOrNull { it.prefix == prefix } ?: ScramMessageType.Error
