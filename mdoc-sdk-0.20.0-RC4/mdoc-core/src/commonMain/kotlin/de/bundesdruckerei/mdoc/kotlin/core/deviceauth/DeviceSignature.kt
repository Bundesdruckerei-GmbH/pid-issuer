/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import COSE.AlgorithmID
import COSE.Attribute
import COSE.HeaderKeys
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.common.UnsupportedCurveException
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.COSESign1
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.SigStructure
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.Sign1
import java.security.Signature

/**
 * version 1.0
 * 9.1.3.6 mdoc ECDSA / EdDSA Authentication
 *
 * The alg element (RFC 8152) shall be included as an element in the protected header. Other elements
 * should not be present in the protected header. An mdoc shall use one of the following signature
 * algorithms: “ES256” (ECDSA with SHA-256), “ES384” (ECDSA with SHA-384), “ES512” (ECDSA with
 * SHA-512) or “EdDSA” (EdDSA). ”ES256” shall be used with curves P-256 and brainpoolP256r1. “ES384”
 * shall be used with curves P-384, brainpoolP320r1 and brainpoolP384r1. “ES512” shall be used with
 * curves P-521 and brainpoolP512r1. “EdDSA” shall be used with curves Ed25519 and Ed448
 */
class DeviceSignature(message: COSESign1 = COSESign1()) : DevAuthBase<COSESign1, COSEKey>(message) {
    constructor(
        deviceAuthentication: DeviceAuthentication,
        authenticationKey: COSEKey,
        curveName: String
    ) : this() {
        message.apply {
            SetContent(deviceAuthentication.asBytes())
            addAttribute(
                HeaderKeys.Algorithm,
                calculateAlgorithmID(curveName).AsCBOR(),
                Attribute.PROTECTED
            )
            sign(authenticationKey)
        }
    }

    constructor(obj: CBORObject) : this(COSESign1.fromCBOR(obj))

    override fun validate(key: COSEKey): Boolean {
        return message.validate(key)
    }

    override val contextTag = ContextTag.Sig

    companion object {
        fun calculateAlgorithmID(curveName: String): AlgorithmID {
            return when (curveName) {
                CryptoUtils.CURVE_P_256 -> AlgorithmID.ECDSA_256
                CryptoUtils.CURVE_P_384 -> AlgorithmID.ECDSA_384
                CryptoUtils.CURVE_P_521 -> AlgorithmID.ECDSA_512
                "" -> throw UnsupportedCurveException("curve name must not be null or empty string")
                else -> throw UnsupportedCurveException("Unsupported or unknown curve name , curve = $curveName")
            }
        }

        fun from(
            signature: Signature,
            deviceAuthentication: DeviceAuthentication
        ): DeviceSignature {
            val toBeSigned = SigStructure(deviceAuthentication.asBytes())
            val signed = CryptoUtils.generateRawSignature(signature, toBeSigned.asBytes())
            return DeviceSignature(Sign1(signed).asCBOR())
        }
    }
}
