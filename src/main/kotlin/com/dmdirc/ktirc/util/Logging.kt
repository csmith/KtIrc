package com.dmdirc.ktirc.util

import java.util.logging.Logger
import kotlin.reflect.KClass

private fun <T: Any> logger(forClass: KClass<T>): Logger {
    return Logger.getLogger(forClass.qualifiedName)
}

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { logger(this::class) }
}