/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import COSE.Sign1Message
import COSE.sign
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import de.bdr.openid4vc.common.signing.Signer
import java.time.Duration
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class StatusListPoolConfigurationTest {
    private fun statusListPoolConfiguration(keystore: String, password: String) =
        StatusListPoolConfiguration(
            apiKey = UUID.randomUUID().toString(),
            apiKeys = null,
            size = 128,
            bits = 2,
            issuer = "http://example.com",
            precreation = PrecreationConfiguration(Duration.ofSeconds(10), 1),
            prefetch = PrefetchConfiguration(16, 32),
            updateInterval = Duration.ofSeconds(10),
            listLifetime = Duration.ofSeconds(15),
            aggregationId = null,
            signer = SignerConfiguration(keystore, password),
        )

    @Test
    fun `Should modify CWT header with multiple certificates in chain without exception`() {
        // Given
        val subject =
            statusListPoolConfiguration("classpath:/keys/pid_issuer_multi_chain.p12", "test")
        val sign1Message = sign1Message(subject.signerInstance)

        // When, then
        assertDoesNotThrow { subject.modifyCwtHeader(sign1Message) }
    }

    @Test
    fun `Should modify CWT header with one certificate in chain without exception`() {
        // Given
        val subject =
            statusListPoolConfiguration("classpath:/keys/pid_issuer_single_chain.p12", "test")
        val sign1Message = sign1Message(subject.signerInstance)

        // When, then
        assertDoesNotThrow { subject.modifyCwtHeader(sign1Message) }
    }

    @Test
    fun `Should modify JWT header with multiple certificates in chain without exception`() {
        // Given
        val subject =
            statusListPoolConfiguration("classpath:/keys/pid_issuer_multi_chain.p12", "test")
        val builder = JWSHeader.Builder(JWSAlgorithm.ES512)

        // When, then
        assertDoesNotThrow { subject.modifyJwsHeader(builder) }
    }

    @Test
    fun `Should modify JWT header with one certificate in chain without exception`() {
        // Given
        val subject =
            statusListPoolConfiguration("classpath:/keys/pid_issuer_single_chain.p12", "test")
        val builder = JWSHeader.Builder(JWSAlgorithm.ES512)

        // When, then
        assertDoesNotThrow { subject.modifyJwsHeader(builder) }
    }

    private fun sign1Message(signer: Signer): Sign1Message {
        val sign1Message = Sign1Message()
        sign1Message.SetContent("test")
        sign1Message.sign(signer)

        return sign1Message
    }
}
