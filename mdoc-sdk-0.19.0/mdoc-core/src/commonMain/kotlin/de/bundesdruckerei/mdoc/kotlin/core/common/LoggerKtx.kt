/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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