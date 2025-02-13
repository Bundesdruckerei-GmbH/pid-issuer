/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package COSE

import com.nimbusds.jose.jwk.Curve.P_256
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.signing.DVSP256SHA256HS256DelegateSigner
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256HS256MacSigner
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256Key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UtilsTest {

    @Test
    fun testInvalidSignerAlgorithm() {
        val private = ECKeyGenerator(P_256).generate().toECPrivateKey()
        val public = ECKeyGenerator(P_256).generate().toECPublicKey()
        val signer =
            DVSP256SHA256HS256DelegateSigner(
                DVSP256SHA256HS256MacSigner(DVSP256SHA256Key(private, public))
            )

        val message = Sign1Message()
        message.SetContent(ByteArray(0))

        assertThrows<IllegalArgumentException> { message.sign(signer) }
    }
}
