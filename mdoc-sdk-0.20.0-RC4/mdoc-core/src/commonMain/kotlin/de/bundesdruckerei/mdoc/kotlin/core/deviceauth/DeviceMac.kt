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
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript
import de.bundesdruckerei.mdoc.kotlin.core.common.UnsupportedCurveException
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import de.bundesdruckerei.mdoc.kotlin.core.common.toHex
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.COSEMac0
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil
import java.nio.charset.StandardCharsets

/**
 * version 1.0
 * 9.1.3.5 mdoc MAC Authentication
 *
 * RFC 8152 describes the algorithm identifiers that shall be used in the alg element. “HMAC 256/256”
 * (HMAC with SHA-256) shall be used.
 */
class DeviceMac(message: COSEMac0 = COSEMac0()) : DevAuthBase<COSEMac0, EMacKey>(message) {

    constructor(
        deviceAuthentication: DeviceAuthentication,
        eMacKey: EMacKey,
        curveName: String
    ) : this() {
        message.apply {
            SetContent(deviceAuthentication.asBytes())
            addAttribute(
                HeaderKeys.Algorithm,
                calculateAlgorithmID(curveName).AsCBOR(),
                Attribute.PROTECTED
            )
            Create(eMacKey)
        }
    }

    constructor(obj: CBORObject) : this(COSEMac0.fromCBOR(obj))

    override fun validate(key: EMacKey): Boolean = message.Validate(key)
    override val contextTag = ContextTag.Mac

    companion object {
        fun getInfo() = "EMacKey".toByteArray(StandardCharsets.UTF_8)

        fun from(
            deviceAuthentication: DeviceAuthentication,
            curveName: String,
            sharedSecret: ByteArray,
            sessionTranscriptBytes: ByteArray,
        ): DeviceMac {
            val sessionKey = CryptoUtils.deriveSessionKey(
                sharedSecret = sharedSecret,
                salt = saltFrom(sessionTranscriptBytes),
                info = getInfo()
            )

            log.d("MAC Key: ${sessionKey.toHex()}")
            val deviceMac = DeviceMac(deviceAuthentication, sessionKey, curveName)
            log.d("MAC devAuth: ${deviceMac.asCBOR()}")
            return deviceMac
        }

        fun calculateMacKey(
            sessionTranscript: SessionTranscript,
            devicePublicKey: COSEKey,
            readerPrivateKey: AsymmetricKeyParameter
        ): ByteArray {
            val pubParam = ECUtil.generatePublicKeyParameter(devicePublicKey.AsPublicKey())

            log.d("Device public key: ${devicePublicKey.AsPublicKey().encoded.toHex()}")
            log.d("Reader private key: $readerPrivateKey")

            return CryptoUtils.deriveSessionKeyFromSharedSecret(
                CryptoUtils.calculateSharedSecret(
                    pubParam,
                    readerPrivateKey
                ),
                saltFrom(sessionTranscript.asBytes()),
                getInfo()
            )
        }

        private fun saltFrom(sessionTranscriptBytes: ByteArray) =
            CryptoUtils.digestSHA256(sessionTranscriptBytes)

        fun calculateAlgorithmID(curveName: String): AlgorithmID = when (curveName) {
            CryptoUtils.CURVE_P_256 -> AlgorithmID.HMAC_SHA_256
            CryptoUtils.CURVE_P_384 -> AlgorithmID.HMAC_SHA_384
            CryptoUtils.CURVE_P_521 -> AlgorithmID.HMAC_SHA_512
            "" -> throw UnsupportedCurveException("curve name must not be null or empty string")
            else -> throw UnsupportedCurveException("Unsupported or unknown curve name , curve = $curveName")
        }
    }
}
