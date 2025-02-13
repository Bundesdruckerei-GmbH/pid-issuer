/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.logging

import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal object Oid4VcLog {
    val log = LoggerFactory.getLogger(Oid4VcLog::class.java)

    fun <T> mdc(vararg entries: Pair<String, String>, block: () -> T): T {
        val previousContext = MDC.getCopyOfContextMap()
        entries.forEach { MDC.put(it.first, it.second) }
        try {
            return block()
        } finally {
            MDC.setContextMap(previousContext)
        }
    }
}
