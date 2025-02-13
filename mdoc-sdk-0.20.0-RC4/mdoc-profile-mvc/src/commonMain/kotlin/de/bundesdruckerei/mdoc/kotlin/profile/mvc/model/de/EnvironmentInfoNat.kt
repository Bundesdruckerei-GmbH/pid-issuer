package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentInfoNat(
    @SerialName("stationary_noise")
    val stationaryNoise: tstr? = null,
    @SerialName("stationary_noise_rotation")
    val stationaryNoiseRotation: uint? = null,
    @SerialName("road_noise")
    val roadNoise: tstr? = null,
    @SerialName("co2_combined_wltp")
    val co2CombinedWltp: uint? = null,
    @SerialName("co2_combined")
    val co2Combined: uint? = null,
    @SerialName("type_approval_emission_class")
    val typeApprovalEmissionClass: tstr? = null,
    @SerialName("emission_class_key")
    val emissionClassKey: tstr? = null,
    @SerialName("emission_class")
    val emissionClass: tstr? = null,
)
