/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.ICBORToFromConverter
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSigned
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceAuthValidationResult
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceSigned
import de.bundesdruckerei.mdoc.kotlin.core.errors.Errors
import de.bundesdruckerei.mdoc.kotlin.core.requests.DeviceRequest
import org.bouncycastle.crypto.EphemeralKeyPair
import java.security.cert.X509Certificate
import java.time.OffsetDateTime

data class Document @JvmOverloads constructor(
    val docType: DocType,
    val issuerSigned: IssuerSigned,
    val deviceSigned: DeviceSigned? = null,
    val deviceRequest: DeviceRequest? = null,
    val errors: Errors? = null
) : ICBORable {
    override fun asCBOR(): CBORObject = CborConverter.ToCBORObject(this)

    @JvmOverloads
    fun validate(
        rootCertificates: Collection<X509Certificate>,
        certificateValidator: (certificateChain: List<X509Certificate>) -> Boolean = { true },
        readerMacKey: EphemeralKeyPair?,
        sessionTranscript: SessionTranscript,
        currentTimestamp: OffsetDateTime = OffsetDateTime.now()
    ): DocumentValidationResult {
        val issuedSignedValidationResult = issuerSigned.validate(
            docType = docType,
            rootCertificates = rootCertificates,
            certificateValidator = certificateValidator,
            currentTimestamp = currentTimestamp
        )

        val deviceSignedValidationResult = deviceSigned?.validate(
            docType = docType,
            deviceKeyInfo = issuerSigned.issuerAuth.mso.deviceKeyInfo,
            readerMacKey = readerMacKey,
            sessionTranscript = sessionTranscript,
        ) ?: DeviceAuthValidationResult()

        return DocumentValidationResult(
            issuerAuthValidationResult = issuedSignedValidationResult,
            deviceAuthValidationResult = deviceSignedValidationResult
        )
    }

    object CborConverter : ICBORToFromConverter<Document> {
        override fun ToCBORObject(obj: Document): CBORObject {
            val cbor = CBORObject.NewMap()
            cbor["docType"] = CBORObject.FromObject(obj.docType)
            cbor["issuerSigned"] = obj.issuerSigned.asCBOR()
            obj.deviceSigned?.let { cbor["deviceSigned"] = it.asCBOR() }
            obj.errors?.let { cbor["errors"] = it.asCBOR() }
            return cbor
        }

        override fun FromCBORObject(cborObject: CBORObject): Document {
            val docType = cborObject["docType"].AsString()
            val issuerSigned = IssuerSigned.fromCBOR(cborObject["issuerSigned"])
            val deviceSigned = cborObject["deviceSigned"]?.let { DeviceSigned.fromCBOR(it) }
            val errors: Errors? = cborObject["errors"]?.let { Errors.fromCBOR(it) }
            return Document(docType, issuerSigned, deviceSigned, null, errors)
        }
    }

    companion object {
        fun fromCBOR(cborObject: CBORObject): Document = CborConverter.FromCBORObject(cborObject)
        fun fromBytes(data: ByteArray): Document = fromCBOR(CBORObject.DecodeFromBytes(data))
    }
}
