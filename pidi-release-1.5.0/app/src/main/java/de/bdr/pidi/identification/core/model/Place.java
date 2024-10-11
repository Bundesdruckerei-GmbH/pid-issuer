/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.model;

public record Place(PlaceType type,
                    String freeText,
                    /** optional */String street, String city,
                    /** state of region, optional*/String state,
                    String country, /** optional */String zipCode) {

    public static Place fromStructured(String street, String city, String state, String country, String zipCode) {
        return new Place(PlaceType.STRUCTURED, null, street, city, state, country, zipCode);
    }

    public static Place fromFreeText(String freeText) {
        return new Place(PlaceType.FREE_TEXT, freeText, null, null, null, null, null);
    }

    public static Place fromNoPlace(String noPlace) {
        return new Place(PlaceType.NO_PLACE, noPlace, null, null, null, null, null);
    }
}
