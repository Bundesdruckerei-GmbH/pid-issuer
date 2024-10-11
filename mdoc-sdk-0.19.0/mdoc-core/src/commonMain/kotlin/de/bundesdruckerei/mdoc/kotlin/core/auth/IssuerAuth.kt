/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import COSE.AlgorithmID
import COSE.Attribute
import COSE.CoseException
import COSE.HeaderKeys
import COSE.Sign1Message
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.DigestAlgorithm
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.ValidityRange
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest
import java.time.Instant

class IssuerAuth : Sign1Message, ICBORable {
    val mso: MobileSecurityObject
    var signature: ByteArray = ByteArray(0)
        private set

    private constructor(cborObject: CBORObject) : super(false) {
        try {
            DecodeFromCBORObject(cborObject)
        } catch (ce: CoseException) {
            log.e("Invalid COSE Sign1 structure", ce)
        }

        try {
            signature = cborObject.get(3).GetByteString()
        } catch (ex: Exception) {
            when (ex) {
                is IllegalArgumentException,
                is IllegalStateException,
                is NullPointerException -> log.e("Unable to retrieve signature.", ex)

                else -> log.e("Unknown exception occurred when retrieving the signature.", ex)
            }
        }

        val msoBytes = GetContent()
        log.d("MSO: ${Hex.toHexString(msoBytes)}")
        val msoCBOR = CBORObject.DecodeFromBytes(msoBytes)
        mso = MobileSecurityObject.fromTaggedCBOR(msoCBOR)
    }

    private constructor(mso: MobileSecurityObject) : super(false) {
        this.mso = mso
        SetContent(mso.asTaggedCBOR().EncodeToBytes())
    }


    fun obtainX5Chain() = obtainX5Chain(asCBOR())

    override fun asCBOR(): CBORObject = EncodeToCBORObject()

    companion object {
        private const val x5chainLabel = 33
        private val x5chainCbor = CBORObject.FromObject(x5chainLabel)

        private val algCbor = HeaderKeys.Algorithm.AsCBOR()
        fun fromCBOR(cborObject: CBORObject) = IssuerAuth(cborObject)

        fun obtainX5Chain(issuerAuthCbor: CBORObject) = X5Chain.fromCBOR(issuerAuthCbor[1][x5chainLabel])

        internal fun create(
            docType: DocType,
            nameSpaces: IssuerNameSpaces,
            valueDigests: ValueDigests?,
            dsCertificates: X5Chain,
            deviceKeyInfo: DeviceKeyInfo,
            validityRange: ValidityRange,
            signatureAlgorithm: AlgorithmID,
            digestAlgorithm: DigestAlgorithm
        ) = create(
            docType = docType,
            nameSpaces = nameSpaces,
            valueDigests = valueDigests,
            dsCertificates = dsCertificates,
            deviceKeyInfo = deviceKeyInfo,
            validityRange = validityRange,
            signatureAlgorithm = signatureAlgorithm,
            digestAlgorithm = digestAlgorithm,
            getNow = Instant::now
        )

        internal fun create(
            docType: DocType,
            nameSpaces: IssuerNameSpaces,
            valueDigests: ValueDigests?,
            dsCertificates: X5Chain,
            deviceKeyInfo: DeviceKeyInfo,
            validityRange: ValidityRange,
            signatureAlgorithm: AlgorithmID,
            digestAlgorithm: DigestAlgorithm,
            getNow: () -> Instant
        ): IssuerAuth {

            val localValueDigests = valueDigests ?: ValueDigests(
                nameSpaces.mapValues { (_, issuerSignedItems) ->
                    issuerSignedItems.associateTo(LinkedHashMap(issuerSignedItems.size)) { issuerSignedItem ->
                        issuerSignedItem.digestID to MessageDigest
                            .getInstance(digestAlgorithm.value)
                            .digest(issuerSignedItem.asTaggedCBORBytes().EncodeToBytes())
                    }
                }
            )

            val now = getNow()

            require(now <= validityRange.validUntil) {
                "time of signing must be before `${validityRange.validUntil}` inclusive."
            }

            if (now < validityRange.validFrom) {
                log.i("time of signing is before document's `validFrom` property.")
            }

            val validityInfo = ValidityInfo(signed = now, validityRange = validityRange)

            val issuerAuth = IssuerAuth(
                mso = MobileSecurityObject(
                    version = "1.0", // As specified in ISO/IEC 18013-5:2021
                    digestAlgorithm = digestAlgorithm,
                    valueDigests = localValueDigests,
                    deviceKeyInfo = deviceKeyInfo,
                    docType = docType,
                    validityInfo = validityInfo,
                )
            )

            issuerAuth.addAttribute(
                /* label = */ algCbor,
                /* value = */ signatureAlgorithm.AsCBOR(),
                /* where = */ Attribute.PROTECTED
            )
            issuerAuth.addAttribute(
                /* label = */ x5chainCbor,
                /* value = */ dsCertificates.asCBOR(),
                /* where = */ Attribute.UNPROTECTED
            )

            return issuerAuth
        }
    }
}
