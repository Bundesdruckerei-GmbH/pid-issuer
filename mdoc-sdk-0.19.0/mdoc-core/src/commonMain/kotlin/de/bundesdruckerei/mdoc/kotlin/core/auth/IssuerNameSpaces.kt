/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
