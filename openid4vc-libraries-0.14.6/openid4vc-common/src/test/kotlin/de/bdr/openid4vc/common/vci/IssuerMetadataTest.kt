/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.bdr.openid4vc.common.UnsupportedCredentialDescription
import de.bdr.openid4vc.common.UnsupportedCredentialFormat
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialDescription
import de.bdr.openid4vc.common.formats.msomdoc.Policy
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialDescription
import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProofType
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.assertThrows
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class IssuerMetadataTest {

    @Test
    fun `parse mdoc issuer meta data `() {
        val mdlExample = loadTestdata("vci/issuerMetadata_mdoc.json")

        val json: JsonElement = Json.decodeFromString(mdlExample)
        val issuerMetadata: IssuerMetadata = Json.decodeFromJsonElement(json)
    }

    @Test
    fun `parse minimal mdoc vc issuer meta data`() {
        val sdjwtExample = loadTestdata("vci/issuerMetadata_mdoc_min.json")

        val json: JsonElement = Json.decodeFromString(sdjwtExample)
        val issuerMetadata: IssuerMetadata = Json.decodeFromJsonElement(json)

        issuerMetadata.credentialConfigurationsSupported.forEach { (_, u) ->
            assertTrue(u is MsoMdocCredentialDescription)
        }
    }

    @Test
    fun `parse minimal mdoc vc issuer meta data missing doctype`() {
        val sdjwtExample = loadTestdata("vci/issuerMetadata_mdoc_missing_doctype.json")
        assertThrows<IllegalArgumentException> { Json.decodeFromString(sdjwtExample) }
    }

    @Test
    fun `deserialize unknown format credential description`() {
        assertThrows<IllegalArgumentException> {
            Json.decodeFromString<CredentialDescription>("""{"format":"unknown"}""")
        }
    }

    @Test
    fun `minimal mdoc vc issuer metadata encode decode`() {
        val metadata =
            IssuerMetadata(
                credentialIssuer = "https://example.com",
                credentialEndpoint = "https.//example.com/credential",
                credentialConfigurationsSupported =
                    mapOf("id" to MsoMdocCredentialDescription(doctype = "doctype"))
            )

        val deserialized = Json.encodeAndDecodeFromString(metadata)

        assertThat(deserialized).isEqualTo(metadata)
    }

    @Test
    fun `mdoc vc issuer metadata encode decode`() {
        val metadata =
            IssuerMetadata(
                credentialIssuer = "https://example.com",
                credentialEndpoint = "https.//example.com/credential",
                credentialConfigurationsSupported =
                    mapOf(
                        "id" to
                            MsoMdocCredentialDescription(
                                proofTypesSupported =
                                    mapOf(
                                        JwtProofType to
                                            ProofTypeSupported(
                                                signingAlgValuesSupported = listOf("ES256")
                                            ),
                                        CwtProofType to
                                            ProofTypeSupported(
                                                signingAlgValuesSupported = listOf("ES256"),
                                                cwtAlgValuesSupported = listOf(3),
                                                cwtCrvValuesSupported = listOf(5)
                                            )
                                    ),
                                doctype = "doctype",
                                policy = Policy(batchSize = 10, oneTimeUse = true),
                                claims =
                                    mapOf(
                                        "test" to
                                            Json.encodeToJsonElement(
                                                MsoMdocCredentialDescription.Claim(
                                                    display =
                                                        listOf(
                                                            MsoMdocCredentialDescription.Claim
                                                                .Display(name = "a", locale = "DE"),
                                                            MsoMdocCredentialDescription.Claim
                                                                .Display()
                                                        )
                                                )
                                            ) as JsonObject
                                    ),
                                display =
                                    listOf(
                                        Json.encodeToJsonElement(
                                                CredentialDescription.Display(
                                                    name = "name",
                                                    locale = "DE",
                                                    logo =
                                                        CredentialDescription.Display.Logo(
                                                            uri = "https://example.com/logo.png",
                                                            altText = "alt-text"
                                                        ),
                                                    description = "description",
                                                    backgroundColor = "ffffff",
                                                    backgroundImage =
                                                        CredentialDescription.Display
                                                            .BackgroundImage(
                                                                uri =
                                                                    "https://example.com/background.png",
                                                                textColor = "ff00ff"
                                                            ),
                                                    textColor = "ff00ff"
                                                )
                                            )
                                            .jsonObject,
                                        Json.encodeToJsonElement(
                                                CredentialDescription.Display(
                                                    name = "bar",
                                                    logo =
                                                        CredentialDescription.Display.Logo(
                                                            uri = "https://example.com/logo2"
                                                        ),
                                                    backgroundImage =
                                                        CredentialDescription.Display
                                                            .BackgroundImage(
                                                                uri =
                                                                    "https://example.com/background2.png"
                                                            )
                                                )
                                            )
                                            .jsonObject,
                                        Json.encodeToJsonElement(
                                                CredentialDescription.Display<Nothing, Nothing>(
                                                    name = "foo"
                                                )
                                            )
                                            .jsonObject
                                    )
                            )
                    ),
                display =
                    listOf(
                        Json.encodeToJsonElement(
                                IssuerMetadata.Display(
                                    name = "name",
                                    locale = "DE",
                                    logo =
                                        IssuerMetadata.Display.Logo(
                                            uri = "http://example.com/logo.png",
                                            altText = "alt-text"
                                        )
                                )
                            )
                            .jsonObject,
                        Json.encodeToJsonElement(
                                IssuerMetadata.Display(
                                    logo =
                                        IssuerMetadata.Display.Logo(
                                            uri = "https://example.com/logo2.png"
                                        )
                                )
                            )
                            .jsonObject,
                        Json.encodeToJsonElement(
                                IssuerMetadata.Display<IssuerMetadata.Display.Logo>()
                            )
                            .jsonObject
                    ),
                credentialResponseEncryption =
                    CredentialResponseEncryption(
                        algValuesSupported = listOf("ALG01"),
                        encValuesSupported = listOf("ENC01"),
                        encryptionRequired = true
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString(metadata)

        assertThat(deserialized).isEqualTo(metadata)
    }

    @Test
    fun `parse sdjwt vc issuer meta data `() {
        val sdjwtExample = loadTestdata("vci/issuerMetadata_sdjwt.json")

        val json: JsonElement = Json.decodeFromString(sdjwtExample)
        val issuerMetadata: IssuerMetadata = Json.decodeFromJsonElement(json)

        issuerMetadata.credentialConfigurationsSupported.forEach { (_, u) ->
            assertTrue(u is SdJwtVcCredentialDescription)
        }
    }

    @Test
    fun `parse minimal sdjwt vc issuer meta data`() {
        val sdjwtExample = loadTestdata("vci/issuerMetadata_sdjwt_min.json")

        val json: JsonElement = Json.decodeFromString(sdjwtExample)
        val issuerMetadata: IssuerMetadata = Json.decodeFromJsonElement(json)

        issuerMetadata.credentialConfigurationsSupported.forEach { (_, u) ->
            assertTrue(u is SdJwtVcCredentialDescription)
        }
    }

    @Test
    fun `parse minimal sdjwt vc issuer meta data missing vct`() {
        val sdjwtExample = loadTestdata("vci/issuerMetadata_sdjwt_missing_vct.json")
        assertThrows<IllegalArgumentException> { Json.decodeFromString(sdjwtExample) }
    }

    @Test
    fun `sdjwt vc issuer metadata encode decode`() {
        val metadata =
            IssuerMetadata(
                credentialIssuer = "https://example.com",
                credentialEndpoint = "https.//example.com/credential",
                credentialConfigurationsSupported =
                    mapOf(
                        "id" to
                            SdJwtVcCredentialDescription(
                                vct = "vct",
                                claims =
                                    JsonObject(
                                        mapOf(
                                            "test" to
                                                Json.encodeToJsonElement(
                                                    SdJwtVcCredentialDescription.Claim(
                                                        display =
                                                            listOf(
                                                                SdJwtVcCredentialDescription.Claim
                                                                    .Display(
                                                                        name = "a",
                                                                        locale = "DE"
                                                                    )
                                                            )
                                                    )
                                                )
                                        )
                                    )
                            )
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString(metadata)

        assertThat(deserialized).isEqualTo(metadata)
    }

    @Test
    fun `parse vc issuer meta data with unknown types`() {
        val sdjwtExample = loadTestdata("vci/issuerMetadata_unknowntypes.json")

        val json: JsonElement = Json.decodeFromString(sdjwtExample)
        val issuerMetadata: IssuerMetadata =
            Json { ignoreUnknownKeys = true }.decodeFromJsonElement(json)

        val credDefinition = issuerMetadata.credentialConfigurationsSupported["gx:LegalParticipant"]

        assert(credDefinition is UnsupportedCredentialDescription)
        assert(credDefinition!!.format is UnsupportedCredentialFormat)
        assertEquals("ldp_vc", credDefinition.format.format)
        assertContains(credDefinition.cryptographicBindingMethodsSupported!!, "DID")
        assertNull(credDefinition.cryptographicSigningAlgValuesSupported)
        assertNull(credDefinition.proofTypesSupported)

        assertFailsWith<NotImplementedError> { credDefinition.format.register() }
        assertFailsWith<NotImplementedError> { credDefinition.format.formatDescriptionClass }
        assertFailsWith<NotImplementedError> { credDefinition.format.credentialRequestClass }
        assertFailsWith<NotImplementedError> { credDefinition.format.credentialDescriptionClass }

        // without ignore unknown keys
        assertFailsWith<java.lang.IllegalArgumentException> {
            Json.decodeFromJsonElement<IssuerMetadata>(json)
        }

        println(issuerMetadata)

        println(Json.encodeToString(issuerMetadata))
    }
}
