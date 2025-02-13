package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrailerMassInfo(
    @SerialName("unit")
    val unit: tstr,
    @SerialName("tech_perm_max_tow_mass_braked_trail")
    val techPermMaxTowMassBrakedTrailer: Float? = null,
    @SerialName("tech_perm_max_tow_mass_unbr_trailer")
    val techPermMaxTowMassUnbrakedTrailer: Float? = null,
)
