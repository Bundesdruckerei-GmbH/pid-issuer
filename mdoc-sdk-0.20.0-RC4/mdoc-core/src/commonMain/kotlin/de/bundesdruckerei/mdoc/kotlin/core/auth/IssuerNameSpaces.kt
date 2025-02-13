/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace

typealias IssuerNameSpaces = Map<NameSpace, IssuerSignedItems>

fun IssuerNameSpaces.asCBOR(): CBORObject {
    return CBORObject.NewMap().apply {
        forEach { (nameSpace, items) ->
            Set(nameSpace, items.asCBOR())
        }
    }
}
