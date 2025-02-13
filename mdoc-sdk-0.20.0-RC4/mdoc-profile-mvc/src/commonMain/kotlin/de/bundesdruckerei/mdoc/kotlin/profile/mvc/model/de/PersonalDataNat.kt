package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalDataNat(
    @SerialName("association_name")
    val associationName: tstr? = null
)
