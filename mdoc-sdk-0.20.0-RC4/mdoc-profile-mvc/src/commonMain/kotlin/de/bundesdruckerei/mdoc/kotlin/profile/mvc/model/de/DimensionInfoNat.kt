package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DimensionInfoNat(
    @SerialName("length_min")
    val lengthMin: uint? = null,
    @SerialName("length_max")
    val lengthMax: uint? = null,
    @SerialName("width_min")
    val widthMin: uint? = null,
    @SerialName("width_max")
    val widthMax: uint? = null,
    @SerialName("height_min")
    val heightMin: uint? = null,
    @SerialName("height_max")
    val heightMax: uint? = null,
)
