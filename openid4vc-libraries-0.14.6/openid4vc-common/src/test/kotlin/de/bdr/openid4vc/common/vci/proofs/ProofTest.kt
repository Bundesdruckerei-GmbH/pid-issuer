/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jose.jwk.Curve.P_256
import com.nimbusds.jose.jwk.Curve.P_384
import com.nimbusds.jose.jwk.Curve.P_521
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.currentTimeMillis
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProof
import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProofType
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tests.TestData

class ProofTest {

    @Test
    fun testProofs() {
        val key = ECKeyGenerator(P_256).generate()
        val signer = JwkSigner(key)

        val key384 = ECKeyGenerator(P_384).generate()
        val signer384 = JwkSigner(key384)

        val key512 = ECKeyGenerator(P_521).generate()
        val signer512 = JwkSigner(key512)

        val proofs =
            listOf(
                CwtProof.create(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonce = "abc",
                    signer = signer
                ),
                JwtProof.create(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonce = "def",
                    signer = signer
                ),
                CwtProof.create(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonce = "ghi",
                    signer = signer384
                ),
                CwtProof.create(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonce = "jkl",
                    signer = signer512
                )
            )

        val json = Json.encodeToString(proofs)

        val decodedProofs: List<Proof> = Json.decodeFromString(json)

        assertThat(decodedProofs.size).isEqualTo(4)
        val first = decodedProofs[0]
        val second = decodedProofs[1]
        val cwt384 = decodedProofs[2]
        val cwt512 = decodedProofs[3]

        assertTrue(first is CwtProof)
        first.validate(
            clientId = "http://example.com/wallet",
            audience = "http://example.com/issuer",
            nonce = "abc"
        )

        assertTrue(second is JwtProof)
        second.validate(
            clientId = "http://example.com/wallet",
            audience = "http://example.com/issuer",
            nonce = "def"
        )

        assertTrue(cwt384 is CwtProof)
        cwt384.validate(
            clientId = "http://example.com/wallet",
            audience = "http://example.com/issuer",
            nonce = "ghi"
        )

        assertTrue(cwt512 is CwtProof)
        cwt512.validate(
            clientId = "http://example.com/wallet",
            audience = "http://example.com/issuer",
            nonce = "jkl"
        )

        assertThrows<IllegalStateException> {
            currentTimeMillis = { 0 }
            try {
                second.validate(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonce = "def"
                )
            } finally {
                currentTimeMillis = System::currentTimeMillis
            }
        }

        assertThrows<IllegalStateException> {
            second.validate(
                clientId = "http://example.com/wallet-invalid",
                audience = "http://example.com/issuer",
                nonce = "def"
            )
        }
    }

    @Test
    fun `validate cwt proof without cwt tag`() {
        val data = TestData.loadTestdata("vci/proof/cwt_proof_wo_cwt_tag1")
        val cwt = CwtProof(data, CwtProofType)
        cwt.validate(
            "mobilewallet",
            "https://9cf4-213-142-97-194.ngrok-free.app",
            "TjPVXBne7m2Dp30C0kNKk7J6KvxCcR2kjAEn6pd5Vbg",
            40000000000
        )
    }

    @Test
    fun `validate cwt proof with cwt tag`() {
        val data = TestData.loadTestdata("vci/proof/cwt_proof_with_cwt_tag1")
        val cwt = CwtProof(data, CwtProofType)
        cwt.validate(
            "track1_light",
            "https://trial.authlete.net",
            "v-1b-n82kEJGbHROSekGsmR-xEuamCxY_T0tXtQN-dY",
            40000000000
        )
    }

    @Test
    fun `validate cwt proof with invalid claims`() {
        val data = TestData.loadTestdata("vci/proof/cwt_proof_with_cwt_tag1")
        val cwt = CwtProof(data, CwtProofType)
        assertFailsWith<IllegalStateException> {
            cwt.validate(
                "invalid client id",
                "https://trial.authlete.net",
                "v-1b-n82kEJGbHROSekGsmR-xEuamCxY_T0tXtQN-dY",
                40000000000
            )
        }

        assertFailsWith<IllegalStateException> {
            cwt.validate(
                "track1_light",
                "invalid audience",
                "v-1b-n82kEJGbHROSekGsmR-xEuamCxY_T0tXtQN-dY",
                40000000000
            )
        }

        assertFailsWith<IllegalStateException> {
            cwt.validate("track1_light", "https://trial.authlete.net", "invalid nonce", 40000000000)
        }

        assertFailsWith<IllegalStateException> {
            cwt.validate(
                "track1_light",
                "https://trial.authlete.net",
                "v-1b-n82kEJGbHROSekGsmR-xEuamCxY_T0tXtQN-dY",
                1L
            )
        }
    }
}
