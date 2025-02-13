package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EngineInfo(
    @SerialName("engine_number")
    val engineNumber: uint? = null,
    @SerialName("engine_capacity")
    val engineCapacity: uint? = null,
    @SerialName("engine_power")
    val enginePower: uint? = null,
    @SerialName("class_off_hybrid_vehicle_code")
    val classOffHybridVehicleCode: tstr? = null,
    @SerialName("energy_sources")
    val energySources: List<EnergySource>? = null,
)
