/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs.cwt

import COSE.AlgorithmID.ECDSA_256
import COSE.HeaderKeys
import COSE.OneKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.common.toJwk
import java.lang.IllegalStateException
import java.util.Date
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CwtTest {

    private val signingKey = ECKeyGenerator(Curve.P_256).generate()
    private val validationOneKey = OneKey(signingKey.toECPublicKey(), null)
    private val signer = JwkSigner(signingKey)

    @Test
    fun `given a cwt with invalid cose_key when cose key is read then IllegalStateException is thrown`() {
        val cwt = Cwt()
        cwt.unprotectedHeader[CBORObject.FromObject("test")] = CBORObject.FromObject("test")
        cwt.protectedHeader[Cwt.Companion.HeaderKeys.COSE_KEY] = CBORObject.FromObject(ByteArray(3))
        assertThrows<IllegalStateException> { cwt.coseKey }
    }

    @Test
    fun `given a cwt with all fields set when serialized and deserialized then all values are the same`() {
        val key = OneKey.generateKey(ECDSA_256)
        val iat = Date(0)
        val test = CBORObject.FromObject("test")

        val cwt = Cwt()
        cwt.unprotectedHeader[test] = test
        cwt.nonce = "nonce"
        cwt.coseKey = key
        cwt.cty = "cty"
        cwt.alg = ECDSA_256.AsCBOR()
        cwt.iss = "iss"
        cwt.iat = iat
        cwt.aud = "aud"

        val parsed = Cwt.fromBytes(cwt.toBytes(signer, useCwtTag = true))

        assertTrue(parsed.validate(validationOneKey))
        assertThat(parsed.unprotectedHeader[test]).isEqualTo(test)
        assertThat(parsed.coseKey!!.toJwk()).isEqualTo(key.toJwk())
        assertThat(parsed.nonce).isEqualTo("nonce")
        assertThat(parsed.cty).isEqualTo("cty")
        assertThat(parsed.alg).isEqualTo(ECDSA_256.AsCBOR())
        assertThat(parsed.iss).isEqualTo("iss")
        assertThat(parsed.iat).isEqualTo(iat)
        assertThat(parsed.aud).isEqualTo("aud")
    }

    @Test
    fun `given a cwt with all fields set and unset when serialized and deserialized then all values are null`() {
        val cwt = Cwt()
        cwt.coseKey = OneKey.generateKey(ECDSA_256)
        cwt.coseKey = null
        cwt.cty = "cty"
        cwt.cty = null
        cwt.nonce = "nonce"
        cwt.nonce = null
        cwt.alg = CBORObject.FromObject(3)
        cwt.alg = null
        cwt.iss = "iss"
        cwt.iss = null
        cwt.iat = Date(0)
        cwt.iat = null
        cwt.aud = "aud"
        cwt.aud = null

        val parsed = Cwt.fromBytes(cwt.toBytes(signer))

        assertThat(parsed.coseKey).isNull()
        assertThat(parsed.cty).isNull()
        assertThat(parsed.nonce).isNull()
        assertThat(parsed.alg).isNull()
        assertThat(parsed.iss).isNull()
        assertThat(parsed.iat).isNull()
        assertThat(parsed.aud).isNull()
    }
}
