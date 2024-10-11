/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.readerauth

import COSE.AlgorithmID
import COSE.Attribute
import COSE.HeaderKeys
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.common.UnsupportedCurveException
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.COSESign1
import java.security.cert.X509Certificate

class ReaderAuth(val message: COSESign1 = COSESign1()) {
    constructor(
        readerAuthentication: ReaderAuthentication,
        readerAuthenticationKey: COSEKey,
        curveName: String,
        readerCertificate: X509Certificate
    ) : this() {
        message.apply {
            SetContent(readerAuthentication.asBytes())
            addAttribute(
                HeaderKeys.Algorithm,
                calculateAlgorithmID(curveName).AsCBOR(),
                Attribute.PROTECTED
            )
            addAttribute(
                CBORObject.FromObject(x5chainLabel),
                CBORObject.FromObject(readerCertificate.encoded),
                Attribute.UNPROTECTED
            )
            sign(readerAuthenticationKey)
        }
    }

    constructor(obj: CBORObject) : this(COSESign1.fromCBOR(obj))

    fun validate(readerAuthentication: ReaderAuthentication, key: COSEKey): Boolean {
        message.SetContent(readerAuthentication.asBytes())
        return message.validate(key)
    }

    fun asBytes(): ByteArray = asCBOR().EncodeToBytes()
    fun asCBOR(): CBORObject = message.EncodeToCBORObject()

    companion object {
        const val x5chainLabel = 33

        // TODO code duplication? see also DeviceMac.calculateAlgorithmID(…) and DeviceSignature.calculateAlgorithmID(…)
        fun calculateAlgorithmID(curveName: String): AlgorithmID = when (curveName) {
            CryptoUtils.CURVE_P_256 -> AlgorithmID.ECDSA_256
            CryptoUtils.CURVE_P_384 -> AlgorithmID.ECDSA_384
            CryptoUtils.CURVE_P_521 -> AlgorithmID.ECDSA_512
            "" -> throw UnsupportedCurveException("curve name must not be null or empty string")
            else -> throw UnsupportedCurveException("Unsupported or unknown curve name , curve = $curveName")
        }

        fun fromCBOR(obj: CBORObject): ReaderAuth = ReaderAuth(COSESign1.fromCBOR(obj))
    }
}
