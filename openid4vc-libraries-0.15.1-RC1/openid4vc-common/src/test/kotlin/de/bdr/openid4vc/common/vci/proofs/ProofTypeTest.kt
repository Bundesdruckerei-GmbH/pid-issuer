/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci.proofs

import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProofTypeTest {

    @Test
    fun `given list with unknown proof type`() {

        val jsonWithUnsupportedProofType =
            """{
        "cwt": {
          "proof_signing_alg_values_supported": [
            "-7",
            "-35",
            "-36"
          ],
          "proof_alg_values_supported": [-7, -35, -36],
          "proof_crv_values_supported": [1, 2, 3]
        },
        "jwt": {
          "proof_signing_alg_values_supported": [
            "ES256",
            "ES384",
            "ES512"
          ]
        }
      }"""

        // should fail when deserialize with strict mode
        val jsonFormatStrict = Json { ignoreUnknownKeys = false }
        assertThrows<Exception> {
            jsonFormatStrict.decodeFromString<Map<ProofType, ProofTypeConfiguration>>(
                jsonWithUnsupportedProofType
            )
        }

        // should be ignored in ignore mode
        val jsonFormat = Json { ignoreUnknownKeys = true }
        val proofTypesSupported: Map<ProofType, ProofTypeConfiguration> =
            jsonFormat.decodeFromString(SupportedProofTypesSerializer, jsonWithUnsupportedProofType)
        assertEquals(1, proofTypesSupported.size)
        assertNotNull(proofTypesSupported[JwtProofType])
    }
}
