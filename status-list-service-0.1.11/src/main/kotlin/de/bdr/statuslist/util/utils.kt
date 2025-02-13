/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.util

import java.lang.System.nanoTime
import java.security.MessageDigest
import java.time.Duration
import org.slf4j.LoggerFactory

@OptIn(ExperimentalStdlibApi::class)
fun sha256(data: ByteArray): String {
    return MessageDigest.getInstance("SHA-256").digest(data).toHexString()
}

val Any.log
    get() = LoggerFactory.getLogger(this::class.java)

fun <T> measureRuntime(block: () -> T): Pair<Duration, T> {
    val start = nanoTime()
    val result = block()
    return Pair(Duration.ofNanos(nanoTime() - start), result)
}
