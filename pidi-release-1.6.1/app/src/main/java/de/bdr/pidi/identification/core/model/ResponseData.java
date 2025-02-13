/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.model;

public record ResponseData(String pseudonym,
                           String familyNames, String givenNames,
                           String birthName, String dateOfBirth, String placeOfBirth,
                           Place residence,
                           String nationality, boolean ageOver18
) {
}
