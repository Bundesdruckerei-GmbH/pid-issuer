/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

import com.upokecenter.cbor.CBORObject

interface ICBORable {
    fun asCBOR(): CBORObject
}
