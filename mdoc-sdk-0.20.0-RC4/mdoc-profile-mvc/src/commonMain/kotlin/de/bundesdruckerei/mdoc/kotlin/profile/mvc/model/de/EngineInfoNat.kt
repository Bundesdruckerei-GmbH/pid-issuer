package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EngineInfoNat(
    @SerialName("rated_speed_rotation")
    val ratedSpeedRotation: uint? = null,
    @SerialName("power_to_weight_ratio")
    val powerToWeightRatio: Float? = null,
    @SerialName("max_speed")
    val maxSpeed: uint? = null,
    @SerialName("fuel_capacity")
    val fuelCapacity: Float? = null,
    @SerialName("engine_fuel_type_text")
    val engineFuelTypeText: tstr? = null,
    @SerialName("energy_source")
    val energySource: tstr? = null
)
