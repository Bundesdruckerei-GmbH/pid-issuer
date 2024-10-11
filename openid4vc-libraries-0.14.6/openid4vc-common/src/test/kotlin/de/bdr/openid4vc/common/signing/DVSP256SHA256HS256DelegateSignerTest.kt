/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.JWSAlgorithms.DVS_P256_SHA256_HS256
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256HS256MacSigner
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import java.lang.RuntimeException
import kotlin.random.Random
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DVSP256SHA256HS256DelegateSignerTest {

    private val delegate = mockk<DVSP256SHA256HS256MacSigner>()

    @Test
    fun `given a DVSP256SHA256HS256DelegateSigner when algorithm is retrieved then it is DVS_P256_SHA256_HS256`() {
        val inTest = DVSP256SHA256HS256DelegateSigner(delegate)

        assertThat(inTest.algorithm).isEqualTo(Algorithm.DVS_P256_SHA256_HS256)
    }

    @Test
    fun `given a DVSP256SHA256HS256DelegateSigner when keys are retrieved then the key material contains the key from the delegate`() {
        val ecPublicKey = ECKeyGenerator(Curve.P_256).generate().toECPublicKey()
        every { delegate.dvsp256SHA256Key.pkR }.returns(ecPublicKey)
        val inTest = DVSP256SHA256HS256DelegateSigner(delegate)

        val keys = inTest.keys

        assertThat(keys).isInstanceOf(JwkKeyMaterial::class)
        assertThat(keys.jwk.toECKey().toECPublicKey()).isEqualTo(ecPublicKey)
    }

    @Test
    fun `given a DVSP256SHA256HS256DelegateSigner when sign is invoked then it delegates to sign of the delegate`() {
        val inTest = DVSP256SHA256HS256DelegateSigner(delegate)
        val toSign = Random.nextBytes(16)
        val signature = Random.nextBytes(16)
        every { delegate.sign(aJwsHeaderWithOnlyAlgSet(DVS_P256_SHA256_HS256), toSign) }
            .returns(Base64URL.encode(signature))

        val result = inTest.sign(toSign)

        assertThat(result).isEqualTo(signature)
    }

    @Test
    fun `given a DVSP256SHA256HS256DelegateSigner when sign of the delegate throws a JOSEException then it is wrapped in a RuntimeException`() {
        val inTest = DVSP256SHA256HS256DelegateSigner(delegate)
        val toSign = Random.nextBytes(16)
        val joseException = JOSEException("Error")
        every { delegate.sign(aJwsHeaderWithOnlyAlgSet(DVS_P256_SHA256_HS256), toSign) }
            .throws(joseException)

        val e = assertThrows<RuntimeException> { inTest.sign(toSign) }

        assertThat(e.cause).isEqualTo(joseException)
    }

    private fun MockKMatcherScope.aJwsHeaderWithOnlyAlgSet(alg: JWSAlgorithm): JWSHeader = match {
        it.algorithm == alg && it.toJSONObject().size == 1
    }
}
