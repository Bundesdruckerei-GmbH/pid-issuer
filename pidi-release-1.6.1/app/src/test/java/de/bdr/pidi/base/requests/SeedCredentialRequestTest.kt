/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.base.requests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.common.vci.CredentialRequest.Companion.serializer
import de.bdr.pidi.testdata.TestUtils
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SeedCredentialRequestTest {
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        SeedCredentialFormat
    }

    @DisplayName("Verify valid SeedCredentialRequest with proof can be parsed")
    @Test
    fun test001() {
        val body = objectMapper.createObjectNode()
            .put("format", "seed_credential")
            .put("pin_derived_eph_key_pop", getPin())

        val proofNode: ObjectNode = body.putObject("proof")
        proofNode.put("jwt", getProof()).put("proof_type", "jwt")

        val credentialRequest = Json.decodeFromString(serializer(), body.toString())

        assertThat(credentialRequest)
            .isInstanceOf(CredentialRequest::class.java)
            .asInstanceOf(InstanceOfAssertFactories.type(SeedCredentialRequest::class.java))
            .extracting(SeedCredentialRequest::pinDerivedEphKeyPop)
            .isInstanceOf(SignedJWT::class.java)
    }

    @DisplayName("Verify valid SeedCredentialRequest with single proofs can be parsed")
    @Test
    fun test002() {
        val body = objectMapper.createObjectNode()
            .put("format", "seed_credential")
            .put("pin_derived_eph_key_pop", getPin())

        val proofNode: ObjectNode = body.putObject("proofs")
        proofNode.putArray("jwt").add(getProof())

        val credentialRequest = Json.decodeFromString(serializer(), body.toString())

        assertThat(credentialRequest)
            .isInstanceOf(CredentialRequest::class.java)
            .asInstanceOf(InstanceOfAssertFactories.type(SeedCredentialRequest::class.java))
            .extracting(SeedCredentialRequest::pinDerivedEphKeyPop)
            .isInstanceOf(SignedJWT::class.java)
    }

    private fun getPin(): String? =
        TestUtils.PIN_DERIVED_EPH_KEY_POP.serialize()

    private fun getProof(): String? =
        TestUtils.DEVICE_KEY_PROOF.serialize()

}