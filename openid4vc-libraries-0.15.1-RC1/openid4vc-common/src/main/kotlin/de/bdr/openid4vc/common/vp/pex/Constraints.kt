/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Constraints(
    @SerialName("fields") val fields: List<Field>? = emptyList(),
    @SerialName("limit_disclosure") val limitDisclosure: String? = null,
) {
    @Transient
    val limitDisclosureSetting =
        LimitDisclosureSetting.entries.firstOrNull { it.value == limitDisclosure }
            ?: throw IllegalArgumentException("Invalid limit disclosure value $limitDisclosure")
}

enum class LimitDisclosureSetting(val value: String?) {
    NONE(null),
    REQUIRED("required"),
    PREFERRED("preferred"),
}
