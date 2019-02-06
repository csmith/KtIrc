package com.dmdirc.ktirc.sasl

import java.util.*

internal fun ByteArray.toBase64() = String(Base64.getEncoder().encode(this))
internal fun String.fromBase64() = Base64.getDecoder().decode(this)
