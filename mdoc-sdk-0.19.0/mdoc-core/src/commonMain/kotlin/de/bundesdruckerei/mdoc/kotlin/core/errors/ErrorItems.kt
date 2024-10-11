/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.errors

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.ICBORConverter
import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import de.bundesdruckerei.mdoc.kotlin.core.int

typealias ErrorCode = int

typealias ErrorItem = MutableMap<DataElementIdentifier, ErrorCode>

data class ErrorItems(val errors: ErrorItem) : ICBORable {

    init {
        if (errors.isEmpty()) log.w("ErrorItem map should not be empty.")
    }

    override fun asCBOR(): CBORObject = cborConverter.ToCBORObject(this)

    companion object {
        @Suppress("ObjectLiteralToLambda")
        private val cborConverter = object : ICBORConverter<ErrorItems> {
            override fun ToCBORObject(obj: ErrorItems): CBORObject {
                return CBORObject.NewMap().apply {
                    obj.errors.entries.forEach { errorItem ->
                        Set(errorItem.key, errorItem.value)
                    }
                }
            }
        }

        fun fromCBOR(cborObject: CBORObject): ErrorItems {
            val errorItems: ErrorItem = mutableMapOf()
            cborObject.entries.forEach { (key, value) ->
                errorItems.set(key.AsString(), value.AsInt32Value())
            }
            return ErrorItems(errorItems)
        }
    }
}
