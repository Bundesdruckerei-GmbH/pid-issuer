/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci.proofs

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jose.jwk.Curve.P_256
import com.nimbusds.jose.jwk.Curve.P_384
import com.nimbusds.jose.jwk.Curve.P_521
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.clock
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.common.vci.EncryptedNonceService
import de.bdr.openid4vc.common.vci.NonceService.StandardNoncePurpose.C_NONCE
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof
import java.time.Duration
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tests.withFixedTime

class ProofTest {

    @Test
    fun testProofs() {
        val nonceService = EncryptedNonceService(ByteArray(16), Duration.ofMinutes(100))

        val key = ECKeyGenerator(P_256).generate()
        val signer = JwkSigner(key)

        val key384 = ECKeyGenerator(P_384).generate()
        val signer384 = JwkSigner(key384)

        val key512 = ECKeyGenerator(P_521).generate()
        val signer512 = JwkSigner(key512)

        val proofs =
            listOf(
                JwtProof.create(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonce = nonceService.generate(C_NONCE).nonce,
                    signer = signer,
                )
            )

        val json = Json.encodeToString(proofs)

        val decodedProofs: List<Proof> = Json.decodeFromString(json)

        assertThat(decodedProofs.size).isEqualTo(1)
        val first = decodedProofs[0]
        assertTrue(first is JwtProof)
        first.validate(
            clientId = "http://example.com/wallet",
            audience = "http://example.com/issuer",
            nonceService = nonceService,
        )

        assertThrows<IllegalStateException> {
            withFixedTime(clock.instant().plusSeconds(60)) {
                first.validate(
                    clientId = "http://example.com/wallet",
                    audience = "http://example.com/issuer",
                    nonceService = nonceService,
                )
            }
        }

        assertThrows<IllegalStateException> {
            first.validate(
                clientId = "http://example.com/wallet-invalid",
                audience = "http://example.com/issuer",
                nonceService = nonceService,
            )
        }
    }
}
