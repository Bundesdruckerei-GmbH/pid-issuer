/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.ssi.statuslist

import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.statuslist.StatusList
import de.bdr.openid4vc.statuslist.StatusListToken
import java.time.Instant
import org.junit.jupiter.api.Test

internal class StatusListTokenTest {

    private val issuer = "https://example.com"
    private val subject = "https://example.com/statuslists/1"

    private val issuerKeyJson =
        """
        {
            "kty": "EC",
            "d": "xzUEdsyLosZF0acZGRAjTKImb0lQvAvssDK5XIZELd0",
            "use": "sig",
            "crv": "P-256",
            "x": "I3HWm_0Ds1dPMI-IWmf4mBmH-YaeAVbPVu7vB27CxXo",
            "y": "6N_d5Elj9bs1htgV3okJKIdbHEpkgTmAluYKJemzn1M",
            "kid": "12",
            "alg": "ES256"
        }
        """
            .trimIndent()

    @Test
    fun `create status list token`() {
        val statusList = StatusList(16, 1)
        statusList.set(0, 1)
        statusList.set(3, 1)
        statusList.set(4, 1)
        statusList.set(5, 1)
        statusList.set(7, 1)
        statusList.set(8, 1)
        statusList.set(9, 1)
        statusList.set(13, 1)
        statusList.set(15, 1)

        println("Status List: $statusList")

        val signer = JwkSigner(JWK.parse(issuerKeyJson))

        val statusListJwt =
            StatusListToken(subject, issuer, Instant.now(), statusList).asJwt(signer)

        println("Status List Token: $statusListJwt")
    }

    @Test
    fun `create status list token2`() {
        val statusList = StatusList(12, 2)
        statusList.set(0, 1)
        statusList.set(1, 2)
        statusList.set(2, 0)
        statusList.set(3, 3)
        statusList.set(4, 0)
        statusList.set(5, 1)
        statusList.set(6, 0)
        statusList.set(7, 1)
        statusList.set(8, 1)
        statusList.set(9, 2)
        statusList.set(10, 3)
        statusList.set(11, 3)

        println("Status List: $statusList")

        val issuerKey = JWK.parse(issuerKeyJson)

        val signer = JwkSigner(JWK.parse(issuerKeyJson))

        val statusListJwt =
            StatusListToken(subject, issuer, Instant.now(), statusList).asJwt(signer)

        println("Status List Token: $statusListJwt")
    }
}
