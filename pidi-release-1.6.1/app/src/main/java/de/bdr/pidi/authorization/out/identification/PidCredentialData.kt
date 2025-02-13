/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.identification

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

data class PidCredentialData(
    val pseudonym: String,
    val familyName: String,
    val givenName: String,
    val birthdate: LocalDate,
    val placeOfBirth: BirthPlace? = null,
    val birthFamilyName: String? = null,
    val address: Address? = null,
    val nationality: String? = null,
) {

    companion object {
        val TEST_DATA_SET =
            PidCredentialData(
                pseudonym = "pseudonym",
                familyName = "MUSTERMANN",
                givenName = "ERIKA",
                birthdate = LocalDate.of(1964, 8, 12),
                placeOfBirth = BirthPlace(locality = "BERLIN"),
                birthFamilyName = "GABLER",
                address =
                Address(
                    streetAddress = "HEIDESTRASSE 17",
                    country = "DE",
                    locality = "KÖLN",
                    postalCode = "51147"
                ),
                nationality = "DE"
            )
    }
}

data class BirthPlace @Default constructor(
    val locality: String? = null,
    val country: String? = null,
    val region: String? = null
) {
    @JsonIgnore
    fun isEmpty() = locality == null && country == null && region == null
}

data class Address @Default constructor(
    val formatted: String? = null,
    val country: String? = null,
    val region: String? = null,
    val locality: String? = null,
    val postalCode: String? = null,
    val streetAddress: String? = null,
) {
    @JsonIgnore
    fun isEmpty() =
        formatted == null &&
                country == null &&
                region == null &&
                locality == null &&
                postalCode == null &&
                streetAddress == null
}

annotation class Default

