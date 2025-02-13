package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MassInfo(
    @SerialName("unit")
    val unit: tstr,
    @SerialName("techn_perm_max_laden_mass")
    val technPermMaxLadenMass: Float? = null,
    @SerialName("vehicle_max_mass")
    val vehicleMaxMass: Float? = null,
    @SerialName("whole_vehicle_max_mass")
    val wholeVehicleMaxMass: Float? = null,
    @SerialName("mass_of_vehicle_in_running_order")
    val massOfVehicleInRunningOrder: Float? = null,
)
