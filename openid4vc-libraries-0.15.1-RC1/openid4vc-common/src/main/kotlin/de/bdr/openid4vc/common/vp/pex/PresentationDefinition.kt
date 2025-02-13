/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A presentation definition implementation supporting some basic properties. Matches the features
 * demanded by HAIP and ISO 18013-7 except the submission requirements.
 */
@Serializable
data class PresentationDefinition(
    @SerialName("id") val id: String,
    @SerialName("input_descriptors") val inputDescriptors: List<InputDescriptor>,
) {
    init {
        require(inputDescriptors.map { it.id }.distinct().count() == inputDescriptors.size) {
            "Input descriptor ids must be unique"
        }
        require(
            inputDescriptors
                .flatMap { it.constraints.fields?.mapNotNull { it.id } ?: emptyList() }
                .distinct()
                .count() ==
                inputDescriptors.sumOf { it.constraints.fields?.count { it.id != null } ?: 0 }
        ) {
            "Field ids must be unique"
        }
    }
}
