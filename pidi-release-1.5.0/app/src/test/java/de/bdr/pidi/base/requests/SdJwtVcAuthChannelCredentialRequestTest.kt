/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base.requests

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SdJwtVcAuthChannelCredentialRequestTest {

    @DisplayName("Should register the credential format")
    @Test
    fun test001() {
        // Given
        val jwk = ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).algorithm(Algorithm.parse("ES256")).generate()
        val request = SdJwtVcAuthChannelCredentialRequest(format =  SdJwtVcAuthChannelCredentialFormat, vct = "vct", verifierPub = jwk)
        val subject = request.format as SdJwtVcAuthChannelCredentialFormat

        // When

        // Then
        MatcherAssert.assertThat(subject.registered, Matchers.`is`(true))
    }
}