/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuthorizationResponse(
    /* REQUIRED: Defined in OI4VP */
    @SerialName("vp_token") val vpToken: JsonElement,
    /* REQUIRED: Defined in OI4VP */
    @SerialName("presentation_submission") val presentationSubmission: PresentationSubmission?,
    /* The following parameters MAY be included in the response as defined in the respective specifications.*/
    /* OPTIONAL: Defined in RFC6749 */
    @SerialName("state") val state: String? = null,
    /* OPTIONAL: Defined in OpenID.Core */
    @SerialName("id_token") val idToken: String? = null,
    /* OPTIONAL: Defined in RFC9207 */
    @SerialName("iss") val iss: String? = null,
)

@Serializable
data class PresentationSubmission(
    /* REQUIRED : Defined in DIF Presentation-Exchange */
    @SerialName("id") val id: String,
    /* REQUIRED : Defined in DIF Presentation-Exchange */
    @SerialName("definition_id") val definitionId: String,
    /* REQUIRED : Defined in DIF Presentation-Exchange */
    @SerialName("descriptor_map") val descriptorMap: List<DescriptorMapElement>,
)

@Serializable
data class DescriptorMapElement(
    /* REQUIRED : Defined in DIF Presentation-Exchange */
    @SerialName("id") val id: String,
    /* REQUIRED : Defined in DIF Presentation-Exchange */
    @SerialName("format") val format: String,
    /* REQUIRED : Defined in DIF Presentation-Exchange */
    @SerialName("path") val path: String,
    /* OPTIONAL : Defined in DIF Presentation-Exchange */
    @SerialName("path_nested") val pathNested: DescriptorMapElement? = null,
)
