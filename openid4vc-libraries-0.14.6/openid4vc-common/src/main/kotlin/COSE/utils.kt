/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package COSE

import com.nimbusds.jose.crypto.impl.ECDSA
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.Algorithm
import java.lang.IllegalArgumentException

fun Sign1Message.sign(signer: de.bdr.openid4vc.common.signing.Signer) {
    if (rgbContent == null) throw CoseException("No Content Specified")
    if (rgbSignature != null) return

    if (rgbProtected == null) {
        rgbProtected = if (objProtected.size() > 0) objProtected.EncodeToBytes() else ByteArray(0)
    }

    val obj = CBORObject.NewArray()
    obj.Add(contextString)
    obj.Add(rgbProtected)
    obj.Add(externalData)
    obj.Add(rgbContent)

    val rawSignature = signer.sign(obj.EncodeToBytes())

    rgbSignature =
        when (signer.algorithm) {
            Algorithm.ES256 -> ECDSA.transcodeSignatureToConcat(rawSignature, 64)
            Algorithm.ES384 -> ECDSA.transcodeSignatureToConcat(rawSignature, 96)
            Algorithm.ES512 -> ECDSA.transcodeSignatureToConcat(rawSignature, 132)
            else ->
                throw IllegalArgumentException(
                    "Signer algorithm ${signer.algorithm} is not supported to sign a Sign1Message"
                )
        }

    ProcessCounterSignatures()
}
