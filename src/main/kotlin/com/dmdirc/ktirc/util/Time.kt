package com.dmdirc.ktirc.util

import java.time.LocalDateTime
import java.time.ZoneId

internal var currentTimeZoneProvider = { ZoneId.systemDefault() }
internal var currentTimeProvider = { LocalDateTime.now(currentTimeZoneProvider()) }