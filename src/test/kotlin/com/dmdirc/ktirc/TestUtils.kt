package com.dmdirc.ktirc

internal fun params(vararg args: String) = args.map { it.toByteArray() }
