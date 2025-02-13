/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.ssi.oid4vc.service.issuer.dpop

import de.bdr.openid4vc.common.vci.EncryptedNonceService
import de.bdr.openid4vc.common.vci.NonceService.StandardNoncePurpose.C_NONCE
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tests.withFixedTime

class EncryptedNonceServiceTest {

    val inTest = EncryptedNonceService(ByteArray(16), Duration.ofSeconds(10))

    @Test
    fun `analyze valid nonce`() {
        withFixedTime {
            val nonce = inTest.generate(C_NONCE).nonce
            inTest.validate(nonce, C_NONCE)
        }
    }

    @Test
    fun `analyze expired nonce`() {
        val nonce = inTest.generate(C_NONCE).nonce

        withFixedTime(Instant.now().plusSeconds(100)) {
            assertThrows<IllegalArgumentException> { inTest.validate(nonce, C_NONCE) }
        }
    }

    @Test
    fun `analyze invalid nonce`() {
        assertThrows<IllegalArgumentException> { inTest.validate("invalid", C_NONCE) }
    }
}
