/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import COSE.OneKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jose.jwk.JWK
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoseExtensionsTest {

    @Test
    fun oneKeyToJwk() {
        val ecSpec = ECGenParameterSpec("secp256r1")
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ecSpec)
        val keys = generator.genKeyPair()
        val oneKey = OneKey(keys.public, keys.private)

        val jwk = JWK.parse(oneKey.toJwk().toJSONString())

        assertThat(jwk.toECKey().toECPublicKey()).isEqualTo(keys.public)
        assertThat(jwk.toECKey().toECPrivateKey()).isEqualTo(keys.private)
    }

    @Test
    fun oneKeyToJwkWithUnsupportedKeyType() {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keys = generator.genKeyPair()
        val oneKey = OneKey(keys.public, keys.private)

        assertThrows<IllegalArgumentException> { oneKey.toJwk() }
    }
}
