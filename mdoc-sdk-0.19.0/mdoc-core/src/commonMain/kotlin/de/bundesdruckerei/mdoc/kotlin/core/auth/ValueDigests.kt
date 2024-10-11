/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORTypeMapper
import com.upokecenter.cbor.ICBORToFromConverter
import de.bundesdruckerei.mdoc.kotlin.core.NameSpacesDigests
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

data class ValueDigests(val digests: NameSpacesDigests) : ICBORable {

    override fun asCBOR(): CBORObject = CBORObject.FromObject(this, cborMapper)

    object CborConverter : ICBORToFromConverter<ValueDigests> {

        override fun ToCBORObject(valueDigests: ValueDigests) =
            CBORObject.NewMap().also { nameSpaces ->
                valueDigests.digests.forEach { (nameSpace, digestIds) ->
                    nameSpaces[nameSpace] = CBORObject.NewMap().also {
                        digestIds.forEach { (digestId, digest) ->
                            it[CBORObject.FromObject(digestId)] = CBORObject.FromObject(digest)
                        }
                    }
                }
            }

        override fun FromCBORObject(cborObject: CBORObject) = ValueDigests(
            cborObject.entries.associate { (nameSpace, digestIDs) ->
                nameSpace.AsString() to digestIDs.entries.associateTo(
                    destination = LinkedHashMap(digestIDs.size()),
                    transform = { (digestId, digest) ->
                        digestId.AsInt64Value() to digest.GetByteString()
                    }
                )
            }
        )
    }

    companion object {
        private val cborMapper: CBORTypeMapper =
            CBORTypeMapper().AddConverter(ValueDigests::class.java, CborConverter)

        fun fromCBOR(cborObject: CBORObject) = CborConverter.FromCBORObject(cborObject)
    }
}
