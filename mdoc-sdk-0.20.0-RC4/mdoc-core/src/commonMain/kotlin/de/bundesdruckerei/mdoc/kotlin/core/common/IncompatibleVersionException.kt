/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

class IncompatibleVersionException(msg: String = "Document version is not supported.") :
    IllegalStateException(msg)
