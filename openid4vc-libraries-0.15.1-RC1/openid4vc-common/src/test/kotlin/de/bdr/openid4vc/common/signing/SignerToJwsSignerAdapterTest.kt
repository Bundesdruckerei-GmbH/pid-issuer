/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.util.Base64URL
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.JWSAlgorithms
import io.mockk.every
import io.mockk.mockk
import java.security.SecureRandom
import kotlin.random.Random
import org.junit.jupiter.api.Test

class SignerToJwsSignerAdapterTest {

    val signer = mockk<Signer>()

    @Test
    fun `given a signer with ES256 algorithm when supportedJWSAlgorithms or jwsAlgorithm is called then the correct algorithm is returned`() {
        every { signer.algorithm }.returns(Algorithm.ES256)
        val inTest = SignerToJwsSignerAdapter(signer)

        val jwsAlgorithm = inTest.jwsAlgorithm
        val supported = inTest.supportedJWSAlgorithms()

        assertThat(jwsAlgorithm).isEqualTo(JWSAlgorithm.ES256)
        assertThat(supported).isEqualTo(setOf(JWSAlgorithm.ES256))
    }

    @Test
    fun `given a signer with ES384 algorithm when supportedJWSAlgorithms or jwsAlgorithm is called then the correct algorithm is returned`() {
        every { signer.algorithm }.returns(Algorithm.ES384)
        val inTest = SignerToJwsSignerAdapter(signer)

        val jwsAlgorithm = inTest.jwsAlgorithm
        val supported = inTest.supportedJWSAlgorithms()

        assertThat(jwsAlgorithm).isEqualTo(JWSAlgorithm.ES384)
        assertThat(supported).isEqualTo(setOf(JWSAlgorithm.ES384))
    }

    @Test
    fun `given a signer with ES512 algorithm when supportedJWSAlgorithms or jwsAlgorithm is called then the correct algorithm is returned`() {
        every { signer.algorithm }.returns(Algorithm.ES512)
        val inTest = SignerToJwsSignerAdapter(signer)

        val jwsAlgorithm = inTest.jwsAlgorithm
        val supported = inTest.supportedJWSAlgorithms()

        assertThat(jwsAlgorithm).isEqualTo(JWSAlgorithm.ES512)
        assertThat(supported).isEqualTo(setOf(JWSAlgorithm.ES512))
    }

    @Test
    fun `given a signer with DVS_P256_SHA256_HS256 algorithm when supportedJWSAlgorithms or jwsAlgorithm is called then the correct algorithm is returned`() {
        every { signer.algorithm }.returns(Algorithm.DVS_P256_SHA256_HS256)
        val inTest = SignerToJwsSignerAdapter(signer)

        val jwsAlgorithm = inTest.jwsAlgorithm
        val supported = inTest.supportedJWSAlgorithms()

        assertThat(jwsAlgorithm).isEqualTo(JWSAlgorithms.DVS_P256_SHA256_HS256)
        assertThat(supported).isEqualTo(setOf(JWSAlgorithms.DVS_P256_SHA256_HS256))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `given a signer with ES256 algorithm when sign is is called then the delegate signature is returned transcoded`() {
        every { signer.algorithm }.returns(Algorithm.ES256)
        val toSign = ByteArray(10)
        val signature =
            "3045022100d2ddb5f887480655484eb51a933af9e84e683c50e4480d8ad7c803728e8e2cbd0220652ee8c6ced239016e4cc83b3284fed6d31db2048356c2a9eaa0a6a34bdcbb3d"
                .hexToByteArray()
        val transcoded =
            "d2ddb5f887480655484eb51a933af9e84e683c50e4480d8ad7c803728e8e2cbd652ee8c6ced239016e4cc83b3284fed6d31db2048356c2a9eaa0a6a34bdcbb3d"
                .hexToByteArray()
        every { signer.sign(toSign) }.returns(signature)
        val inTest = SignerToJwsSignerAdapter(signer)

        val result = inTest.sign(JWSHeader(JWSAlgorithm.ES256), toSign)

        assertThat(result).isEqualTo(Base64URL.encode(transcoded))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `given a signer with ES384 algorithm when sign is is called then the delegate signature is returned transcoded`() {
        every { signer.algorithm }.returns(Algorithm.ES384)
        val toSign = ByteArray(10)
        val signature =
            "306502306d6b5c80e07a5c6006a90949f2fcff9aef3e62fcea75a2eb2773342b8980ea83ce5c743dc786ca4021e3aa20e97e1a65023100cca4f2490174bcd952ffbb9e23b146be55debcc1a9bca916211199103e621fcb2339753b92ff23ecce93c178a7bbd4bd"
                .hexToByteArray()
        val transcoded =
            "6d6b5c80e07a5c6006a90949f2fcff9aef3e62fcea75a2eb2773342b8980ea83ce5c743dc786ca4021e3aa20e97e1a65cca4f2490174bcd952ffbb9e23b146be55debcc1a9bca916211199103e621fcb2339753b92ff23ecce93c178a7bbd4bd"
                .hexToByteArray()
        every { signer.sign(toSign) }.returns(signature)
        val inTest = SignerToJwsSignerAdapter(signer)

        val result = inTest.sign(JWSHeader(JWSAlgorithm.ES384), toSign)

        assertThat(result).isEqualTo(Base64URL.encode(transcoded))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `given a signer with ES512 algorithm when sign is is called then the delegate signature is returned transcoded`() {
        every { signer.algorithm }.returns(Algorithm.ES512)
        val toSign = ByteArray(10)
        val signature =
            "308188024200b5801608d48991d6ec210acffbe9cd459b15b90d3c32c4adee286f73a9147441d3c97114c26b27f48ed6bd5c06abcb2f515bcd25f3aad8057191e01f60f97550d7024201bd954fdcba19c1d3dddb2bab5a071d6c2b91025d459fc817e297ae38da34489b5001c5b5376697934748af328b379fc4467be893ee0794adf2a0a62e24a190d471"
                .hexToByteArray()
        val transcoded =
            "00b5801608d48991d6ec210acffbe9cd459b15b90d3c32c4adee286f73a9147441d3c97114c26b27f48ed6bd5c06abcb2f515bcd25f3aad8057191e01f60f97550d701bd954fdcba19c1d3dddb2bab5a071d6c2b91025d459fc817e297ae38da34489b5001c5b5376697934748af328b379fc4467be893ee0794adf2a0a62e24a190d471"
                .hexToByteArray()
        every { signer.sign(toSign) }.returns(signature)
        val inTest = SignerToJwsSignerAdapter(signer)

        val result = inTest.sign(JWSHeader(JWSAlgorithm.ES512), toSign)

        assertThat(result).isEqualTo(Base64URL.encode(transcoded))
    }

    @Test
    fun `given a signer with DVS_P256_SHA256_HS256 algorithm when sign is is called then the delegate signature is returned unmodified`() {
        every { signer.algorithm }.returns(Algorithm.DVS_P256_SHA256_HS256)
        val toSign = ByteArray(10)
        val signature = Random.nextBytes(16)
        every { signer.sign(toSign) }.returns(signature)
        val inTest = SignerToJwsSignerAdapter(signer)

        val result = inTest.sign(JWSHeader(JWSAlgorithms.DVS_P256_SHA256_HS256), toSign)

        assertThat(result).isEqualTo(Base64URL.encode(signature))
    }

    @Test
    fun `given a SignerToJwsSignerAdapter when retrieving the jca context then provider and secure random are null`() {
        every { signer.algorithm }.returns(Algorithm.ES256)
        val inTest = SignerToJwsSignerAdapter(signer)

        val jcaContext = inTest.jcaContext

        assertThat(jcaContext.provider).isNull()
        assertThat(jcaContext.secureRandom.toString()).isEqualTo(SecureRandom().toString())
    }
}
