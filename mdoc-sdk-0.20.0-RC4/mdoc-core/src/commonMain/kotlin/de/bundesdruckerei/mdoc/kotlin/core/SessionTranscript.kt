/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core

import com.upokecenter.cbor.CBORException
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.dataItem
import de.bundesdruckerei.mdoc.kotlin.core.common.ifNotNull
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import de.bundesdruckerei.mdoc.kotlin.core.common.toHex

data class SessionTranscript(
    val deviceEngagementBytes: DeviceEngagementBytes? = null,
    val eReaderKeyBytes: EReaderKeyBytes? = null,
    val handoverBytes: ByteArray? = null
) : ICBORable {

    private val defaultIAExceptionMsg = "Invalid state of SessionTranscript arguments detected."

    init {
        require(
            !(deviceEngagementBytes == null && eReaderKeyBytes == null && handoverBytes == null)
        ) { defaultIAExceptionMsg }

        handoverBytes?.apply {
            require(this.isNotEmpty()) {
                "$defaultIAExceptionMsg HandoverBytes should not be empty."
            }
        }

        eReaderKeyBytes?.requireValidCOSEKeyStructure()

        log.d("deviceEngagementBytes = " + deviceEngagementBytes?.toHex())
        log.d("eReaderKeyBytes = " + eReaderKeyBytes?.toHex())
        log.d("handover = " + handoverBytes?.toHex())

        log.d("sessionTranscript = " + this.asTaggedCBOR())
        log.d("sessionTranscript as Bytes= " + this.asBytes().toHex())
    }

    private fun EReaderKeyBytes.requireValidCOSEKeyStructure() =
        try {
            COSEKey(CBORObject.DecodeFromBytes(this))
        } catch (ex: CBORException) {
            throw IllegalArgumentException(
                "$defaultIAExceptionMsg eReaderKeyBytes with invalid structure.",
                ex
            )
        }

    override fun asCBOR(): CBORObject = CBORObject.NewArray().apply {
        deviceEngagementBytes?.apply {
            Add(CBORObject.FromObjectAndTag(this, CBOR_TAG_BYTE_STRING))
        } ?: Add(CBORObject.Null)

        eReaderKeyBytes?.apply {
            Add(CBORObject.FromObjectAndTag(this, CBOR_TAG_BYTE_STRING))
        } ?: Add(CBORObject.Null)

        handoverBytes?.apply {
            Add(CBORObject.DecodeFromBytes(this))
        } ?: Add(CBORObject.Null)
    }

    fun asTaggedCBOR(): CBORObject =
        CBORObject.FromObjectAndTag(asCBOR().EncodeToBytes(), CBOR_TAG_BYTE_STRING)

    fun asBytes(): ByteArray = asTaggedCBOR().EncodeToBytes()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionTranscript

        if (!deviceEngagementBytes.contentEquals(other.deviceEngagementBytes)) return false
        if (!eReaderKeyBytes.contentEquals(other.eReaderKeyBytes)) return false
        if (!handoverBytes.contentEquals(other.handoverBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceEngagementBytes.contentHashCode()
        result = 31 * result + eReaderKeyBytes.contentHashCode()
        result = 31 * result + handoverBytes.contentHashCode()
        return result
    }

    companion object {
        fun fromCBOR(cborObject: CBORObject): SessionTranscript {
            val deviceEngagementBytes: DeviceEngagementBytes? =
                cborObject[0].ifNotNull()?.GetByteString()
            val eReaderKeyBytes: EReaderKeyBytes? = cborObject[1].ifNotNull()?.GetByteString()
            val handover = cborObject[2].ifNotNull()?.EncodeToBytes()
            return SessionTranscript(deviceEngagementBytes, eReaderKeyBytes, handover)
        }

        fun fromTaggedCBOR(taggedCborObject: CBORObject): SessionTranscript =
            fromCBOR(taggedCborObject.dataItem())
    }
}
