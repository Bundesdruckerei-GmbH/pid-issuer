/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputDescriptor(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("purpose") val purpose: String? = null,
    @SerialName("group") val group: String? = null,
    @Serializable(with = FormatDescriptionSerializer::class)
    @SerialName("format")
    val format: FormatDescription,
    @SerialName("constraints") val constraints: Constraints,
)
