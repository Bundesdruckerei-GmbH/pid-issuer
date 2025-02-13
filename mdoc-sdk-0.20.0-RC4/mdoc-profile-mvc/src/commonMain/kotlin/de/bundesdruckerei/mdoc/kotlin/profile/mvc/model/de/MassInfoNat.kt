package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MassInfoNat(
    @SerialName("mass_of_vehicle_empty_min")
    val massOfVehicleEmptyMin: uint? = null,
    @SerialName("mass_of_vehicle_empty_max")
    val massOfVehicleEmptyMax: uint? = null,
    @SerialName("bearing_load")
    val bearingLoad: uint? = null,
)
