/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORTypeMapper
import com.upokecenter.cbor.ICBORToFromConverter
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.DigestAlgorithm
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.dataItem
import de.bundesdruckerei.mdoc.kotlin.core.tstr

data class MobileSecurityObject(
    val version: tstr,
    val digestAlgorithm: DigestAlgorithm,
    val valueDigests: ValueDigests,
    val deviceKeyInfo: DeviceKeyInfo,
    val docType: DocType,
    val validityInfo: ValidityInfo,
    val status: StatusClaim? = null
) : ICBORable {

    override fun asCBOR(): CBORObject = CBORObject.FromObject(this, cborMapper)

    fun asTaggedCBOR(): CBORObject = CBORObject
        .FromObject(asCBOR().EncodeToBytes())
        .WithTag(CBOR_TAG_BYTE_STRING)

    private object CborConverter : ICBORToFromConverter<MobileSecurityObject> {

        override fun ToCBORObject(mso: MobileSecurityObject): CBORObject {
            return CBORObject.NewMap().also {
                it[VERSION] = CBORObject.FromObject(mso.version)
                it[DIGEST_ALGORITHM] = mso.digestAlgorithm.asCBOR()
                it[VALUE_DIGESTS] = mso.valueDigests.asCBOR()
                it[DEVICE_KEY_INFO] = mso.deviceKeyInfo.asCBOR()
                it[DOCUMENT_TYPE] = CBORObject.FromObject(mso.docType)
                it[VALIDITY_INFO] = mso.validityInfo.asCBOR()
                mso.status?.apply { it[StatusClaim.KEY] = asCBOR() }
            }
        }

        override fun FromCBORObject(cborObject: CBORObject) = MobileSecurityObject(
            version = cborObject[VERSION].AsString(),
            digestAlgorithm = DigestAlgorithm.fromCBOR(cborObject[DIGEST_ALGORITHM]),
            valueDigests = ValueDigests.fromCBOR(cborObject[VALUE_DIGESTS]),
            deviceKeyInfo = DeviceKeyInfo.fromCBOR(cborObject[DEVICE_KEY_INFO]),
            docType = cborObject[DOCUMENT_TYPE].AsString(),
            validityInfo = ValidityInfo(cborObject[VALIDITY_INFO]),
            status = cborObject[StatusClaim.KEY]?.run { StatusClaim(this) }
        )
    }

    companion object {
        private const val VERSION = "version"
        private const val DIGEST_ALGORITHM = "digestAlgorithm"
        private const val VALUE_DIGESTS = "valueDigests"
        private const val DEVICE_KEY_INFO = "deviceKeyInfo"
        private const val DOCUMENT_TYPE = "docType"
        private const val VALIDITY_INFO = "validityInfo"

        private val cborMapper =
            CBORTypeMapper().AddConverter(MobileSecurityObject::class.java, CborConverter)

        fun fromCBOR(cborObject: CBORObject) = CborConverter.FromCBORObject(cborObject)
        fun fromTaggedCBOR(taggedCborObject: CBORObject): MobileSecurityObject =
            fromCBOR(taggedCborObject.dataItem())
    }
}
