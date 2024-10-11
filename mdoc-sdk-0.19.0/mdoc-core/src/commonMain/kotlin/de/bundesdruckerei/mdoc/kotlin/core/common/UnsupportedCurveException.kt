/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

class UnsupportedCurveException : IllegalArgumentException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}
