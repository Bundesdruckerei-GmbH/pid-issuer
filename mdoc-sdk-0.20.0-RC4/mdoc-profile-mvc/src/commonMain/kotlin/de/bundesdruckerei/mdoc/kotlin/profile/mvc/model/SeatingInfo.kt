package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeatingInfo(
    @SerialName("nr_of_seating_positions")
    val nrOfSeatingPositions: uint? = null,
    @SerialName("number_of_standing_places")
    val numberOfStandingPlaces: uint? = null,
)
