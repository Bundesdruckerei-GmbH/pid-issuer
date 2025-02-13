/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.ssi.statuslist

import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.statuslist.StatusList
import de.bdr.openid4vc.statuslist.StatusListRegistry
import de.bdr.openid4vc.statuslist.StatusListToken
import java.time.Instant
import org.junit.jupiter.api.Test

class StatusListForVerifiedEmail {

    private val issuer = "https://issuer-openid4vc.ssi.tir.budru.de"
    private val subject = "https://issuer-openid4vc.ssi.tir.budru.de/statuslists/1"
    private val issuerKeyJson =
        """
           {
               "kty":"EC",
               "nbf":1689684815,
               "d":"hpg4oxiMu0yGZH5QYsprm5S-Puogv8Hpyl6teTUqO2M",
               "use":"sig",
               "crv":"P-256",
               "kid":"MGwwZ6RlMGMxCzAJBgNVBAYTAkRFMQ8wDQYDVQQHDAZCZXJsaW4xHTAbBgNVBAoMFEJ1bmRlc2RydWNrZXJlaSBHbWJIMQowCAYDVQQLDAFJMRgwFgYDVQQDDA9JRHVuaW9uIFRlc3QgQ0ECAQM=",
               "x":"8j18K2e4cdddv4shdEPO8gnu12g3n6D1mD_JSOSpGCc",
               "y":"1vWhoYjr5czqzW6XbC3nmeZq1MCM5JKZNRzlb6SN6VI",
               "exp":1847364815
           }
        """
            .trimIndent()

    @Test
    fun generateInitialStatusListToken() {

        val statusList = StatusList(16, 1, StatusListRegistry.StatusTypes.VALID.v)

        println("Status List: $statusList")

        val signer = JwkSigner(JWK.parse(issuerKeyJson))

        val statusListJwt =
            StatusListToken(subject, issuer, Instant.now(), statusList = statusList).asJwt(signer)

        println("Status List Token: $statusListJwt")
    }
}
