/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.out.identification.Address;
import de.bdr.pidi.authorization.out.identification.BirthPlace;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;

import java.time.LocalDate;

public class ServiceTestData {

    private ServiceTestData() {}

    public static PidCredentialData createPid() {
        BirthPlace birthPlace = new BirthPlace("Lübeck", null, null);
        Address address = new Address("Hinter den Bergen, bei den 7 Zwergen, DE", null,
                null, null, null, null);
        return new PidCredentialData("pseudonym", "Musterfrau", "Erich",
                LocalDate.of(1989, 11, 9), birthPlace,
                null, address,
                "DE"
        );
    }

    public static String createPidString() {
        return """
                {"pseudonym":"pseudonym","familyName":"Donau","givenName":"Johanna","birthdate":[1949,5,23],
                "placeOfBirth":{"country":"DE",
                "region":"Bayern"},"birthFamilyName":"Rhein",
                "address":{"country":"DE",
                "locality":"München","postalCode":"80992","streetAddress":"Hanauer Str. 1"},
                "nationality":"DE"}
                """;
    }

    static String createPidString_withBrokenCountryCode() {
        return """
                {"pseudonym":"pseudonym","familyName":"Putt","givenName":"Ka","birthdate":[1998,4,1],
                "placeOfBirth":{"country":{"value":-4.6}},
                "address":{"locality":"Madrid"},
                "nationality":"DE"}
                """;
    }
}
