/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.errors

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.ICBORConverter
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.log

typealias ErrorsEntry = Pair<NameSpace, ErrorItems>

class Errors(val entries: Array<ErrorsEntry>) : ICBORable {

    init {
        if (entries.isEmpty()) log.w("ErrorEntry array should not be empty.")
    }

    override fun asCBOR(): CBORObject = cborConverter.ToCBORObject(this)

    companion object {
        @Suppress("ObjectLiteralToLambda")
        private val cborConverter = object : ICBORConverter<Errors> {
            override fun ToCBORObject(obj: Errors): CBORObject {
                return CBORObject.NewMap().apply {
                    obj.entries.forEach { (nameSpace, items) ->
                        Set(nameSpace, items.asCBOR())
                    }
                }
            }
        }

        fun fromCBOR(cborObject: CBORObject): Errors {
            val errorsEntries: ArrayList<ErrorsEntry> = ArrayList()

            cborObject.entries.forEach { error ->
                val namespace: NameSpace = error.key.AsString()
                val errorItems: ErrorItems = ErrorItems.fromCBOR(error.value)

                errorsEntries.add(Pair(namespace, errorItems))
            }

            return Errors(errorsEntries.toTypedArray())
        }
    }
}
