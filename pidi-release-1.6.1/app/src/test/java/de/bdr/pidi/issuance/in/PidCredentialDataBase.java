/*
 * Copyright 2024-2025 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import de.bdr.pidi.authorization.out.identification.Address;
import de.bdr.pidi.authorization.out.identification.BirthPlace;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;

import static de.bdr.pidi.testdata.ValidTestData.LOCAL_BIRTH_DATE;

abstract class PidCredentialDataBase {

    static PidCredentialData getPidCredentialData() {
        return getPidCredentialDataOfNationality("DE");
    }

    static PidCredentialData getPidCredentialDataOfNationality(String countryCode) {
        return new PidCredentialData("pseudonym", "familyName", "givenName",
                LOCAL_BIRTH_DATE, new BirthPlace("placeOfBirth", null, null),
                "birthFamilyName",
                new Address("formatted", "DE", "region", "locality", "12345", "streetAddress"), countryCode);
    }

    static PidCredentialData getMinimalPidCredentialData() {
        return new PidCredentialData("pseudonym", "familyName", "givenName",
                LOCAL_BIRTH_DATE, null, null, null, null);
    }
}
