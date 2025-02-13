package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.tstr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalData(
    @SerialName("organization_name_unicode")
    val organizationNameUnicode: tstr? = null,
    @SerialName("organization_name_latin1")
    val organizationNameLatin1: tstr? = null,
    @SerialName("family_name_unicode")
    val familyNameUnicode: tstr? = null,
    @SerialName("family_name_latin1")
    val familyNameLatin1: tstr? = null,
    @SerialName("given_name_unicode")
    val givenNameUnicode: tstr? = null,
    @SerialName("given_name_latin1")
    val givenNameLatin: tstr? = null,
    @SerialName("resident_address")
    val residentAddress: tstr,
    @SerialName("resident_city")
    val residentCity: tstr,
    @SerialName("resident_state")
    val residentState: tstr? = null,
    @SerialName("resident_postal_code")
    val residentPostalCode: tstr? = null,
    @SerialName("resident_country")
    val residentCountry: tstr
)
