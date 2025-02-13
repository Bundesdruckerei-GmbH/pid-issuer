/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
class ClaimsQuery(
    @SerialName("id") val id: String? = null,
    @SerialName("path") val path: ClaimsPathPointer? = null,
    @SerialName("namespace") val namespace: String? = null,
    @SerialName("claim_name") val claimName: String? = null,
    @SerialName("values") val values: List<JsonPrimitive>? = null,
)
