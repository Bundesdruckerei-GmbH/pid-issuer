/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs.cwt

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.currentTimeMillis
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.common.toJwk
import java.lang.IllegalStateException
import java.util.Base64
import java.util.Date
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tests.encodeAndDecodeFromString
import tests.withFixedTime

class CwtProofTest {

    @Test
    fun `given a CwtProof when decodedCwt is retrieved then the claims are as expected`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val proof =
            CwtProof.create(
                clientId = "clientId",
                audience = "audience",
                nonce = "nonce",
                signer = signer,
                issueTimestamp = 5000,
            )

        val cwt = proof.decodedCwt

        assertThat(cwt.aud).isEqualTo("audience")
        assertThat(cwt.nonce).isEqualTo("nonce")
        assertThat(cwt.iat).isEqualTo(Date(5000))
        assertThat(cwt.iss).isEqualTo("clientId")
        assertThat(cwt.coseKey?.toJwk()?.toECKey()?.toPublicKey()).isEqualTo(jwk.toPublicKey())
    }

    @Test
    fun `given a CwtProof when serialized and deserialized and decodedCwt is retrieved then the claims are as expected`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val proof =
            CwtProof.create(
                clientId = "clientId",
                audience = "audience",
                nonce = "nonce",
                signer = signer,
                issueTimestamp = 5000,
            )

        val decoded = Json.encodeAndDecodeFromString(proof)

        val cwt = decoded.decodedCwt

        assertThat(cwt.aud).isEqualTo("audience")
        assertThat(cwt.nonce).isEqualTo("nonce")
        assertThat(cwt.iat).isEqualTo(Date(5000))
        assertThat(cwt.iss).isEqualTo("clientId")
        assertThat(cwt.coseKey?.toJwk()?.toECKey()?.toPublicKey()).isEqualTo(jwk.toPublicKey())
    }

    @Test
    fun `given a CwtProof when validated then it succeeds`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val proof =
            CwtProof.create(
                clientId = "clientId",
                audience = "audience",
                nonce = "nonce",
                signer = signer
            )

        val key = proof.validate("clientId", "audience", "nonce")

        assertThat(key.AsPublicKey()).isEqualTo(jwk.toECPublicKey())
    }

    @Test
    fun `given a CwtProof when validated with invalid clientId then it fails`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val proof =
            CwtProof.create(
                clientId = "clientId",
                audience = "audience",
                nonce = "nonce",
                signer = signer
            )

        assertThrows<IllegalStateException> { proof.validate("invalid", "audience", "nonce") }
    }

    @Test
    fun `given a CwtProof when validated with invalid audience then it fails`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val proof =
            CwtProof.create(
                clientId = "clientId",
                audience = "audience",
                nonce = "nonce",
                signer = signer
            )

        assertThrows<IllegalStateException> { proof.validate("clientId", "invalid", "nonce") }
    }

    @Test
    fun `given a CwtProof when validated with invalid nonce then it fails`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val proof =
            CwtProof.create(
                clientId = "clientId",
                audience = "audience",
                nonce = "nonce",
                signer = signer
            )

        assertThrows<IllegalStateException> { proof.validate("clientId", "audience", "invalid") }
    }

    @Test
    fun `given a CwtProof with invalid alg when validated then it fails`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val cwt =
            Cwt.fromBytes(
                Base64.getUrlDecoder()
                    .decode(
                        CwtProof.create(
                                clientId = "clientId",
                                audience = "audience",
                                nonce = "nonce",
                                signer = signer
                            )
                            .cwt
                    )
            )
        cwt.alg = CBORObject.FromObject("invalid")
        val cwtEncoded =
            Base64.getUrlEncoder().withoutPadding().encodeToString(cwt.sign(signer).EncodeToBytes())
        val proof = CwtProof(cwtEncoded)

        assertThrows<IllegalStateException> { proof.validate("clientId", "audience", "invalid") }
    }

    @Test
    fun `given a CwtProof with x5c header when validated then it fails`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(jwk)
        val cwt =
            Cwt.fromBytes(
                Base64.getUrlDecoder()
                    .decode(
                        CwtProof.create(
                                clientId = "clientId",
                                audience = "audience",
                                nonce = "nonce",
                                signer = signer
                            )
                            .cwt
                    )
            )
        cwt.protectedHeader[CBORObject.FromObject(33)] = CBORObject.FromObject("dummy")
        val cwtEncoded =
            Base64.getUrlEncoder().withoutPadding().encodeToString(cwt.sign(signer).EncodeToBytes())
        val proof = CwtProof(cwtEncoded)

        assertThrows<IllegalStateException> { proof.validate("clientId", "audience", "invalid") }
    }

    @Test
    fun `given an old CwtProof when validated then it fails`() {
        withFixedTime {
            val jwk = ECKeyGenerator(Curve.P_256).generate()
            val signer = JwkSigner(jwk)
            val proof =
                CwtProof.create(
                    clientId = "clientId",
                    audience = "audience",
                    nonce = "nonce",
                    signer = signer
                )

            fixedTime = currentTimeMillis() - 86_000_000

            assertThrows<IllegalStateException> { proof.validate("clientId", "audience", "nonce") }
        }
    }

    @Test
    fun `given an CwtProof from the future when validated then it fails`() {
        withFixedTime {
            val jwk = ECKeyGenerator(Curve.P_256).generate()
            val signer = JwkSigner(jwk)
            val proof =
                CwtProof.create(
                    clientId = "clientId",
                    audience = "audience",
                    nonce = "nonce",
                    signer = signer
                )

            fixedTime = currentTimeMillis() + 86_000_000

            assertThrows<IllegalStateException> { proof.validate("clientId", "audience", "nonce") }
        }
    }

    @Test
    fun `given a currentTimeMillis implementation when a CwtProof is created then the iat is created by invoking currentTimeMills`() {
        withFixedTime {
            val jwk = ECKeyGenerator(Curve.P_256).generate()
            val signer = JwkSigner(jwk)
            val proof =
                CwtProof.create(
                    clientId = "clientId",
                    audience = "audience",
                    nonce = "nonce",
                    signer = signer
                )

            val cwt = proof.decodedCwt
            assertThat(cwt.iat).isEqualTo(Date(fixedTime - fixedTime % 1000))
        }
    }

    @Test
    fun `given an invalid encoded proof when CwtProof is created then SpecificIllegalArgumentException is thrown`() {
        val e = assertThrows<SpecificIllegalArgumentException> { CwtProof(cwt = "invalid") }

        assertThat(e.reason).isEqualTo(INVALID_PROOF)
    }
}
