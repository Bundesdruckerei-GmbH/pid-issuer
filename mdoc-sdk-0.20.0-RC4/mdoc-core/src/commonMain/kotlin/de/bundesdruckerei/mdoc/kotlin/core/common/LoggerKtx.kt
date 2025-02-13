/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

import co.touchlab.kermit.Logger
import co.touchlab.kermit.mutableLoggerConfigInit
import co.touchlab.kermit.platformLogWriter

val mdocLogger = Logger(mutableLoggerConfigInit(listOf(platformLogWriter())))

inline val <reified T : Any> T.log: Logger get() = log()

inline fun <reified T : Any> T.log(tag: String? = this::class.simpleName) = when (tag) {
    null -> mdocLogger
    else -> mdocLogger.withTag(tag)
}
