/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

import com.upokecenter.cbor.CBORObject

interface ICBORable {
    fun asCBOR(): CBORObject
}
