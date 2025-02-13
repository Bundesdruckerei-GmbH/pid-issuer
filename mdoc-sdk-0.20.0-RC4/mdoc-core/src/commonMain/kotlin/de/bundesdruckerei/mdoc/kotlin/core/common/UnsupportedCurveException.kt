/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

class UnsupportedCurveException : IllegalArgumentException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}
