/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.service.credential.response.encryption

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.RSAEncrypter
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.vci.logging.Oid4VcLog.log
import de.bdr.openid4vc.vci.service.endpoints.IllegalCredentialRequestException
import de.bdr.openid4vc.vci.service.endpoints.IllegalCredentialRequestException.Reason
import java.text.ParseException

class DefaultCredentialResponseEncryptionService(
    override val algValuesSupported: List<String> = ALG_VALUES_SUPPORTED,
    override val encValuesSupported: List<String> = ENC_VALUES_SUPPORTED,
    override val required: Boolean = false,
) : CredentialResponseEncryptionService {

    init {
        algValuesSupported.forEach { alg ->
            require(ALG_VALUES_SUPPORTED.contains(alg)) {
                "${this::class.simpleName} does not support configured alg: $alg"
            }
        }
        encValuesSupported.forEach { enc ->
            require(ENC_VALUES_SUPPORTED.contains(enc)) {
                "${this::class.simpleName} does not support configured enc: $enc"
            }
        }
    }

    override fun encrypt(data: String, credentialEncryption: CredentialEncryption): String {
        val algString = credentialEncryption.alg
        if (!algValuesSupported.contains(algString)) {
            fail("Unsupported alg: $algString")
        }
        val alg = JWEAlgorithm.parse(credentialEncryption.alg)

        val encString = credentialEncryption.enc
        val enc = EncryptionMethod.parse(encString)
        if (!encValuesSupported.contains(encString)) {
            fail("Unsupported enc: $encString")
        }

        val jwe = JWEObject(JWEHeader(alg, enc), Payload(data))
        val jwk =
            try {
                JWK.parse(credentialEncryption.jwk.toString())
            } catch (e: ParseException) {
                log.warn("JWK parsing failed.", e)
                fail("Could not parse provided jwk.")
            }

        val encrypter =
            when (jwk.keyType) {
                KeyType.EC -> {
                    val ecKey = jwk.toECKey()
                    if (!ECDHEncrypter.SUPPORTED_ELLIPTIC_CURVES.contains(ecKey.curve)) {
                        fail(
                            "Unsupported JWK curve '${ecKey.curve.name}', only the" +
                                " following curves are supported: $SUPPORTED_CURVES_ERROR_MSG.",
                        )
                    }
                    ECDHEncrypter(ecKey)
                }
                KeyType.RSA -> {
                    RSAEncrypter(jwk.toRSAKey())
                }
                else -> {
                    fail("Unsupported JWK key type: ${jwk.keyType.value}")
                }
            }
        jwe.encrypt(encrypter)
        return jwe.serialize()
    }

    private fun fail(errorDescription: String? = null): Nothing {
        log.warn(
            "Illegal credential request: ${Reason.INVALID_CREDENTIAL_ENCRYPTION_PARAMETERS}, errorDescription: $errorDescription",
        )
        throw IllegalCredentialRequestException(
            reason = Reason.INVALID_CREDENTIAL_ENCRYPTION_PARAMETERS,
            errorDescription = errorDescription,
        )
    }

    companion object {

        private val SUPPORTED_CURVES_ERROR_MSG =
            ECDHEncrypter.SUPPORTED_ELLIPTIC_CURVES.joinToString(", ") { c -> c.name }

        val ALG_VALUES_SUPPORTED: List<String> =
            listOf(
                JWEAlgorithm.ECDH_ES.name,
                JWEAlgorithm.ECDH_ES_A128KW.name,
                JWEAlgorithm.ECDH_ES_A192KW.name,
                JWEAlgorithm.ECDH_ES_A256KW.name,
                JWEAlgorithm.RSA_OAEP_256.name,
                JWEAlgorithm.RSA_OAEP_384.name,
                JWEAlgorithm.RSA_OAEP_512.name,
            )

        val ENC_VALUES_SUPPORTED: List<String> =
            listOf(
                EncryptionMethod.A128GCM.name,
                EncryptionMethod.A192GCM.name,
                EncryptionMethod.A256GCM.name,
            )
    }
}
