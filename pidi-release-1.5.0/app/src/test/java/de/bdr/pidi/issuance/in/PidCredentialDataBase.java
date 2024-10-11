/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import de.bdr.pidi.authorization.out.identification.Address;
import de.bdr.pidi.authorization.out.identification.BirthPlace;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;

import java.time.LocalDate;

abstract class PidCredentialDataBase {

    static PidCredentialData getPidCredentialData() {
        return getPidCredentialDataOfNationality("DE");
    }

    static PidCredentialData getPidCredentialDataOfNationality(String countryCode) {
        return new PidCredentialData("familyName", "givenName", LocalDate.of(2000, 1, 1),
                new BirthPlace("placeOfBirth", null, null), "birthFamilyName",
                new Address("formatted", "DE", "region", "locality", "12345", "streetAddress"),
                countryCode);
    }

    static PidCredentialData getMinimalPidCredentialData() {
        return new PidCredentialData("familyName", "givenName", LocalDate.of(2000, 1, 1),
                null, null, null, null);
    }
}
