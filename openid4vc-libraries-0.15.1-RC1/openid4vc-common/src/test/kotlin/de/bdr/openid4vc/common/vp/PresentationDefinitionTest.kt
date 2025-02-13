/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocFormatDescription
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcFormatDescription
import de.bdr.openid4vc.common.vp.pex.Constraints
import de.bdr.openid4vc.common.vp.pex.InputDescriptor
import de.bdr.openid4vc.common.vp.pex.PresentationDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class PresentationDefinitionTest {

    @Test
    fun `decode and encode presentation definition`() {
        val presentationDefinition =
            PresentationDefinition(
                id = "id",
                inputDescriptors =
                    listOf(
                        InputDescriptor(
                            id = "msomdoc",
                            format = MsoMdocFormatDescription(alg = listOf("ES256")),
                            constraints = Constraints(),
                        ),
                        InputDescriptor(
                            id = "sdjwtvc",
                            format = SdJwtVcFormatDescription(),
                            constraints = Constraints(),
                        ),
                    ),
            )

        val result = Json.encodeAndDecodeFromString(presentationDefinition)

        assertThat(result).isEqualTo(presentationDefinition)
    }

    @Test
    fun `decode mdl presentation definition with all mandatory fields`() {
        val mdlExample = loadTestdata("vp/presentation-definition/presentationDefinition_mdl1.json")

        val json: JsonElement = Json.decodeFromString(mdlExample)
        val pDef: PresentationDefinition = Json.decodeFromJsonElement(json)

        assertEquals("mDL-sample-req", pDef.id)

        assertEquals(1, pDef.inputDescriptors.size)

        val inputDes = pDef.inputDescriptors[0]

        assertEquals(inputDes.format.type, MsoMdocCredentialFormat)

        assertEquals("org.iso.18013.5.1.mDL", inputDes.id)

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement(pDef))
    }

    @Test
    fun `decode mdl presentation definition with one document type and multiple namespaces`() {
        val mdlExample = loadTestdata("vp/presentation-definition/presentationDefinition_mdl2.json")

        val json: JsonElement = Json.decodeFromString(mdlExample)
        val pDef: PresentationDefinition = Json.decodeFromJsonElement(json)

        assertEquals("mDL-sample-req-one-document-type-multiple-namespaces", pDef.id)

        assertEquals(1, pDef.inputDescriptors.size)

        val inputDes = pDef.inputDescriptors[0]

        assertEquals(inputDes.format.type, MsoMdocCredentialFormat)

        assertEquals("org.iso.18013.5.1.mDL", inputDes.id)

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement(pDef))
    }

    @Test
    fun `decode mdl presentation definition with two document types and one namespaces`() {
        val mdlExample = loadTestdata("vp/presentation-definition/presentationDefinition_mdl3.json")

        val json: JsonElement = Json.decodeFromString(mdlExample)
        val pDef: PresentationDefinition = Json.decodeFromJsonElement(json)

        assertEquals("mDL-sample-req-two-document-type-one-namespace", pDef.id)

        assertEquals(2, pDef.inputDescriptors.size)

        val inputDes = pDef.inputDescriptors[0]

        assertEquals(inputDes.format.type, MsoMdocCredentialFormat)

        assertEquals("org.iso.18013.5.1.mDL", inputDes.id)

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement(pDef))
    }

    @Test
    fun `decode sdjwt presentation definition`() {
        val sdJwtExample =
            loadTestdata("vp/presentation-definition/presentationDefinition_sdjwt1.json")

        val json: JsonElement = Json.decodeFromString(sdJwtExample)
        val pDef: PresentationDefinition = Json.decodeFromJsonElement(json)

        assertEquals("d76c51b7-ea90-49bb-8368-6b3d194fc131", pDef.id)

        assertEquals(1, pDef.inputDescriptors.size)

        val inputDes = pDef.inputDescriptors[0]

        assertEquals(SdJwtVcCredentialFormat, inputDes.format.type)

        assertEquals("IdentityCredential", inputDes.id)

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement(pDef))
    }
}
