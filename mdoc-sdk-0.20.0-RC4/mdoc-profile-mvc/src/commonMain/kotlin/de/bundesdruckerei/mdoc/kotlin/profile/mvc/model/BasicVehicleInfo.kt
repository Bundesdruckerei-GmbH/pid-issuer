package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BasicVehicleInfo(
    @SerialName("vehicle_category_code")
    val vehicleCategoryCode: tstr? = null,
    @SerialName("vehicle_category_nat")
    val vehicleCategoryNat: tstr? = null,
    @SerialName("type_approval_number")
    val typeApprovalNumber: tstr? = null,
    @SerialName("make")
    val make: tstr,
    @SerialName("commercial_name")
    val commercialName: tstr? = null,
    @SerialName("colours")
    val colours: List<Colour>? = null,
    @SerialName("automation_level")
    val automationLevel: tstr? = null,
    @SerialName("status_vehicle")
    val statusVehicle: tstr? = null,
)
