package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AxisInfoNat(
    @SerialName("quantity_axes")
    val quantityAxes: uint? = null,
    @SerialName("tech_axial_load_axis_1")
    val techAxialLoadAxis1: uint? = null,
    @SerialName("tech_axial_load_axis_2")
    val techAxialLoadAxis2: uint? = null,
    @SerialName("tech_axial_load_axis_3")
    val techAxialLoadAxis3: uint? = null,
    @SerialName("axial_load_axis_1")
    val axialLoadAxis1: uint? = null,
    @SerialName("axial_load_axis_2")
    val axialLoadAxis2: uint? = null,
    @SerialName("axial_load_axis_3")
    val axialLoadAxis3: uint? = null,
    @SerialName("quantity_drive_axis")
    val quantityDriveAxis: uint? = null,
    @SerialName("tires_axis_1")
    val tireAxis1: tstr? = null,
    @SerialName("tires_axis_2")
    val tireAxis2: tstr? = null,
    @SerialName("tires_axis_3")
    val tireAxis3: tstr? = null,
)
