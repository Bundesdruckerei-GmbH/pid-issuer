/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import com.nimbusds.jose.jwk.Curve.P_256
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_CREDENTIAL_FORMAT
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest
import de.bdr.openid4vc.common.mapStructureToJson
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tests.TestData
import tests.encodeAndDecodeFromString

class CredentialRequestTest {

    @Test
    fun `given a minimal SdJwtVcCredentialRequest when serialized and deserialized then the result equals the input`() {
        val request = SdJwtVcCredentialRequest(vct = "vct", proof = TestData.jwtProof())

        val deserialized = Json.encodeAndDecodeFromString(request)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `given a serialized credential request with non string credential format when deserialized an SerializationException is thrown`() {
        assertThrows<SerializationException> {
            Json.decodeFromString<CredentialRequest>("""{"format":{}}""")
        }
    }

    @Test
    fun `given a serialized credential request with unknown credential format when deserialized a SpecificIllegalArgumentException is thrown`() {
        val e =
            assertThrows<SpecificIllegalArgumentException> {
                Json.decodeFromString<CredentialRequest>("""{"format":"unknown"}""")
            }

        assertThat(e.reason).isEqualTo(INVALID_CREDENTIAL_FORMAT)
    }

    @Test
    fun `given an invalid format when passed into SdJwtVcCredentialRequest constructor then an IllegalArgumentException is thrown`() {
        assertThrows<IllegalArgumentException> {
            SdJwtVcCredentialRequest(
                vct = "vct",
                proof = TestData.jwtProof(),
                format = MsoMdocCredentialFormat
            )
        }
    }

    @Test
    fun `given a minimal MsoMdocCredentialRequest when serialized and deserialized then the result equals the input`() {
        val request = MsoMdocCredentialRequest(doctype = "doctype", proof = TestData.jwtProof())

        val deserialized = Json.encodeAndDecodeFromString(request)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `given an authenticated channel MsoMdocCredentialRequest when serialized and deserialized then the result equals the input`() {
        val request =
            MsoMdocCredentialRequest(
                doctype = "doctype",
            )

        val deserialized = Json.encodeAndDecodeFromString(request)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `given two equal instances of MsoMdocCredentialRequest when hash codes are calculated then they are equal as well`() {
        val request1 =
            MsoMdocCredentialRequest(
                format = MsoMdocCredentialFormat,
                doctype = "doctype",
                proof = TestData.jwtProof(),
            )

        val request2 = Json.encodeAndDecodeFromString(request1)

        assertThat(request1).isEqualTo(request2)
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
    }

    @Test
    fun `given to different instances of MsoMdocCredentialRequest when hash codes are calculated then they are different as well`() {
        val request1 =
            MsoMdocCredentialRequest(
                format = MsoMdocCredentialFormat,
                doctype = "doctype1",
                proof = TestData.jwtProof(),
            )

        val request2 =
            MsoMdocCredentialRequest(
                format = MsoMdocCredentialFormat,
                doctype = "doctype2",
                proof = TestData.jwtProof(),
            )

        assertThat(request1).isNotEqualTo(request2)
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode())
    }

    @Test
    fun `given an instance of MsoMdocCredentialRequest and a String when they are compared then they are different`() {
        val request =
            MsoMdocCredentialRequest(
                doctype = "doctype",
            )

        val s = "Test"

        assertThat(request).isNotEqualTo(s)
    }

    @Test
    fun `given a CredentialIdentifierBasedCredentialRequest when serialized and deserialized then the result equals the input`() {
        val request =
            CredentialIdentifierBasedCredentialRequest(
                proof = TestData.jwtProof(),
                credentialIdentifier = "identifier",
                credentialEncryption =
                    CredentialEncryption(
                        jwk = ECKeyGenerator(P_256).generate().toJSONObject().mapStructureToJson(),
                        alg = "alg",
                        enc = "enc"
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString<CredentialRequest>(request)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `given proof and proofs when construcing a CredentialIdentifierBasedCredentialRequest then creation fails`() {
        assertThrows<IllegalArgumentException> {
            CredentialIdentifierBasedCredentialRequest(
                proof = TestData.jwtProof(),
                proofs = listOf(TestData.jwtProof()),
                credentialIdentifier = "identifier"
            )
        }
    }

    @Test
    fun `given a FormatSpecificCredentialRequest when retrieving credential identifier then null is returned`() {
        assertThat(
                SdJwtVcCredentialRequest(vct = "vct", proof = TestData.jwtProof())
                    .credentialIdentifier
            )
            .isNull()
    }

    @Test
    fun `given a batch credential request when serialized and deserialized then the result equals the input`() {
        val batchRequest =
            BatchCredentialRequest(
                listOf(
                    MsoMdocCredentialRequest(doctype = "doctype", proof = TestData.jwtProof()),
                    SdJwtVcCredentialRequest(vct = "vct", proof = TestData.jwtProof())
                )
            )

        val deserialized = Json.encodeAndDecodeFromString(batchRequest)

        assertThat(deserialized).isEqualTo(batchRequest)
    }

    @Test
    fun `given an invalid format when passed into MsoMdocCredentialRequest constructor then an IllegalArgumentException is thrown`() {
        assertThrows<IllegalArgumentException> {
            MsoMdocCredentialRequest(
                doctype = "doctype",
                proof = TestData.jwtProof(),
                format = SdJwtVcCredentialFormat
            )
        }
    }

    @Test
    fun decodeMultiProofRequest() {
        val testRequest: String =
            """{"format":"vc+sd-jwt","vct":"urn:eu.europa.ec.eudi:pid:1",
        |"proofs":{"jwt":["eyJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwidXNlIjoic2lnIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJmZWQ3OTg2Mi1hZjM2LTRmZWUtOGU2NC04OWUzYzkxMDkxZWQiLCJ4IjoiUElhVndtalF5YVl5V040X3IwRUM2R3RFYk5QbkhwS1NQUGRvOFhUVVoyWSIsInkiOiJ6Sk5CbjA2ckI1U2xodHZ0UUk5UGhRYjY3V3g5VC1kOVNUa2NNUXUyVnNBIiwiYWxnIjoiRVMyNTYifX0.eyJpc3MiOiJmZWQ3OTg2Mi1hZjM2LTRmZWUtOGU2NC04OWUzYzkxMDkxZWQiLCJhdWQiOiJodHRwOi8vcGlkaTo4MDgwL2MxIiwiaWF0IjoxNzE4NzEwNDAxLCJub25jZSI6IlQ4eWhFa2hoZlBQT05zaHlhUlNaanoifQ.A2lkEIWAE8Rtv5zTVGmEHF2Xp7rwVKJDjZjqUfpAK6_5XLI36UGYcbbm1jcbwEuhEXTUDuRo73ZEw6EiM7jagQ",
        |"eyJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwidXNlIjoic2lnIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJmZWQ3OTg2Mi1hZjM2LTRmZWUtOGU2NC04OWUzYzkxMDkxZWQiLCJ4IjoiUElhVndtalF5YVl5V040X3IwRUM2R3RFYk5QbkhwS1NQUGRvOFhUVVoyWSIsInkiOiJ6Sk5CbjA2ckI1U2xodHZ0UUk5UGhRYjY3V3g5VC1kOVNUa2NNUXUyVnNBIiwiYWxnIjoiRVMyNTYifX0.eyJpc3MiOiJmZWQ3OTg2Mi1hZjM2LTRmZWUtOGU2NC04OWUzYzkxMDkxZWQiLCJhdWQiOiJodHRwOi8vcGlkaTo4MDgwL2MxIiwiaWF0IjoxNzE4NzEwNDAxLCJub25jZSI6IlQ4eWhFa2hoZlBQT05zaHlhUlNaanoifQ.60p2ShSwHB5CaYjQ2My8ggo_tXwldIDwutGKLFDD2KBemuTEtAR9Bki36KYQk5L8A_KKc1SZPDZODSeV7jVSFA"]}}"""
                .trimMargin("|")

        val cr: CredentialRequest = Json.decodeFromString(testRequest)
        assertTrue(cr.proofs.isNotEmpty())
    }
}
