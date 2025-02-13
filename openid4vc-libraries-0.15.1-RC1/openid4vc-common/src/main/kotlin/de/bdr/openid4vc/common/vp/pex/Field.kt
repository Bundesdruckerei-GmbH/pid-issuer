/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import jsonStringContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class Field(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("purpose") val purpose: String? = null,
    @SerialName("path") val path: List<String>,
    @SerialName("filter") val filter: JsonObject? = null,
    @SerialName("optional") val optional: Boolean? = null,
    @SerialName("intent_to_retain") val intentToRetain: Boolean? = null,
) {

    init {
        if (filter != null) {
            require(setOf("type", "const").union(filter.keys).size <= 2) {
                "Only type and const supported for filter"
            }

            require(!filter.containsKey("type") || filter["type"] == JsonPrimitive("string")) {
                "Only type == string supported for filter"
            }

            require(!filter.containsKey("const") || filter["const"]?.jsonStringContent() != null) {
                "const must be a string supported for filter"
            }
        }
    }
}
