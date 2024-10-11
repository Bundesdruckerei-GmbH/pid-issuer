/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.ICBORToFromConverter
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.CBORtoClassConversionException
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.dataItem

data class DeviceAuthentication(
    val sessionTranscript: SessionTranscript,
    val docType: DocType,
    val deviceNameSpacesBytes: DeviceNameSpacesBytes
) : ICBORable {

    override fun asCBOR(): CBORObject = CborConverter.ToCBORObject(this)
    fun asTaggedCBOR(): CBORObject =
        CBORObject.FromObjectAndTag(asCBOR().EncodeToBytes(), CBOR_TAG_BYTE_STRING)

    fun asBytes(): ByteArray = asTaggedCBOR().EncodeToBytes()

    object CborConverter : ICBORToFromConverter<DeviceAuthentication> {
        override fun ToCBORObject(obj: DeviceAuthentication?): CBORObject = CBORObject.NewArray()
            .Add(DEVICE_AUTHENTICATION)
            .Add(obj?.sessionTranscript?.asCBOR())
            .Add(obj?.docType)
            .Add(obj?.deviceNameSpacesBytes)

        override fun FromCBORObject(obj: CBORObject?): DeviceAuthentication =
            obj?.let { fromCBOR(it) }
                ?: throw CBORtoClassConversionException(DeviceAuthentication::class)
    }

    companion object {
        internal const val DEVICE_AUTHENTICATION = "DeviceAuthentication"

        fun fromTaggedCBOR(obj: CBORObject): DeviceAuthentication =
            fromCBOR(obj.dataItem())

        fun fromCBOR(obj: CBORObject): DeviceAuthentication {
            require(obj[0]?.AsString() == DEVICE_AUTHENTICATION) { "Missing $DEVICE_AUTHENTICATION header" }
            val sessionTranscript: SessionTranscript = SessionTranscript.fromCBOR(obj[1])
            val docType: DocType = obj[2].AsString()
            val dnBytes: DeviceNameSpacesBytes = obj[3]

            return DeviceAuthentication(sessionTranscript, docType, dnBytes)
        }
    }
}
