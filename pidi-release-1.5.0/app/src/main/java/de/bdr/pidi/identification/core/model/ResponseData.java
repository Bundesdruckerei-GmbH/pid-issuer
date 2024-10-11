/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.model;

public record ResponseData(String pseudonym,
                           String familyNames, String givenNames,
                           String birthName, String dateOfBirth, String placeOfBirth,
                           Place residence,
                           String nationality, boolean ageOver18
) {
}
