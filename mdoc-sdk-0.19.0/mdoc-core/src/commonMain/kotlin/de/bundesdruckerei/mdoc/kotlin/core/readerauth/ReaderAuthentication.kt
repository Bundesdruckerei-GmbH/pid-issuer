/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.readerauth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.ItemsRequestBytes
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

class ReaderAuthentication(
    val sessionTranscript: SessionTranscript,
    val itemsRequestBytes: CBORObject
) : ICBORable {

    init {
        require(itemsRequestBytes.HasMostOuterTag(CBOR_TAG_BYTE_STRING)) {
            "Missing tag $CBOR_TAG_BYTE_STRING in ItemsRequestBytes"
        }
    }

    override fun asCBOR(): CBORObject = CBORObject.NewArray()
        .Add(READER_AUTHENTICATION)
        .Add(sessionTranscript.asCBOR())
        .Add(itemsRequestBytes)

    fun asTaggedCBOR(): CBORObject =
        CBORObject.FromObjectAndTag(asCBOR().EncodeToBytes(), CBOR_TAG_BYTE_STRING)

    fun asBytes(): ByteArray = asTaggedCBOR().EncodeToBytes()

    companion object {
        internal const val READER_AUTHENTICATION = "ReaderAuthentication"

        fun fromTaggedCBOR(taggedCborObject: CBORObject): ReaderAuthentication {
            val cborObject = CBORObject.DecodeFromBytes(taggedCborObject.GetByteString())
            return fromCBOR(cborObject)
        }

        fun fromCBOR(obj: CBORObject): ReaderAuthentication {
            require(obj[0]?.AsString() == READER_AUTHENTICATION) {
                "Missing $READER_AUTHENTICATION header"
            }
            val sessionTranscript: SessionTranscript = SessionTranscript.fromCBOR(obj[1])
            val itemsRequestBytes: ItemsRequestBytes = obj[2]

            return ReaderAuthentication(sessionTranscript, itemsRequestBytes)
        }
    }
}
