package com.dmdirc.ktirc.util

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.sasl.toBase64
import java.time.ZoneOffset

internal var generateLabel = { ircClient: IrcClient ->
    val time = currentTimeProvider().toEpochSecond(ZoneOffset.UTC)
    val counter = ircClient.serverState.labelCounter.incrementAndGet()
    ByteArray(6) {
        when {
            it < 3 -> (time shr it and 0xff).toByte()
            else -> (counter shr (it - 3) and 0xff).toByte()
        }
    }.toBase64()
}
