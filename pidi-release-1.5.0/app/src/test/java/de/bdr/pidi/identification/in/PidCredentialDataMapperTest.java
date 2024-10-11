/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.in;

import de.bdr.pidi.identification.core.model.Place;
import de.bdr.pidi.identification.core.model.ResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

class PidCredentialDataMapperTest {

    final PidCredentialDataMapper out = new PidCredentialDataMapperImpl();
    final DateTimeFormatter day = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Test
    void test_map() {
        ResponseData given = givenResponseData();

        var result = out.map(given);

        Assertions.assertAll(
                () -> Assertions.assertEquals(given.familyNames(), result.getFamilyName()),
                () -> Assertions.assertEquals(given.givenNames(), result.getGivenName()),
                () -> Assertions.assertEquals(given.birthName(), result.getBirthFamilyName()),
                () -> Assertions.assertEquals(given.dateOfBirth(), day.format(result.getBirthdate())),
                () -> Assertions.assertEquals(given.placeOfBirth(), result.getPlaceOfBirth().getLocality()),
                () -> Assertions.assertEquals(given.residence().freeText(), result.getAddress().getFormatted()),
                () -> Assertions.assertEquals(given.residence().street(), result.getAddress().getStreetAddress()),
                () -> Assertions.assertEquals(given.residence().city(), result.getAddress().getLocality()),
                () -> Assertions.assertEquals(given.residence().zipCode(), result.getAddress().getPostalCode()),
                () -> Assertions.assertEquals(given.residence().state(), result.getAddress().getRegion()),
                () -> {
                    if (given.residence().country() == null) {
                        Assertions.assertNull(result.getAddress().getCountry());
                    } else {
                        Assertions.assertEquals(given.residence().country(), result.getAddress().getCountry());
                    }
                },
                () -> {
                    if (given.nationality() == null) {
                        Assertions.assertNull(result.getNationality());
                    } else {
                        Assertions.assertEquals(given.nationality(), result.getNationality());
                    }
                }
        );
    }

    @Test
    void test_badBirthDate() {
        ResponseData given = givenResponseData_badBirthdate();

        Assertions.assertThrows(DateTimeParseException.class, () -> out.map(given));
    }

    private ResponseData givenResponseData() {
        return _given("DE", "18330507","DE");
    }

    private ResponseData givenResponseData_badBirthdate() {
        return _given("DE", "18-321#456","DE");
    }

    private ResponseData _given(String nationality, String birthDate, String addressCountry) {
        return new ResponseData("CAFEBABE12345678CAFEBABE12345678",
                "Brahms", "Johannes", null, birthDate, "Hamburg",
                Place.fromStructured("Peterstraße 39", "Hamburg", "Hamburg", addressCountry, "20355"),
                nationality, true);
    }

}
