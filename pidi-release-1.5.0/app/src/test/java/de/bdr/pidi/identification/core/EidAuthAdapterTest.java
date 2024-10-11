/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.configuration.MultiSamlConfiguration;
import de.bdr.pidi.identification.core.exception.CryptoConfigException;
import de.bdr.pidi.identification.core.model.Authentication;
import de.bund.bsi.eid240.GeneralDateType;
import de.bund.bsi.eid240.GeneralPlaceType;
import de.bund.bsi.eid240.PersonalDataType;
import de.bund.bsi.eid240.PlaceType;
import de.bund.bsi.eid240.RestrictedIDType;
import de.governikus.panstar.sdk.saml.configuration.SamlConfiguration;
import de.governikus.panstar.sdk.saml.response.ProcessedSamlResult;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EidAuthAdapterTest {

    @Mock
    private MultiSamlConfiguration msConf1;
    @Mock
    private MultiSamlConfiguration msConf2;
    @Mock
    private MeterRegistry micrometer;

    static final String[][] PERSONS = {{"Erika", "Mustermann", "Gabler", "D", "Köln", "STRUCTURED", "Hauptstraße 1", "21614", "Buxtehude", "Niedersachsen", "19901003","D" },
            {"John", "Doe", null, "US", "Washington", "FREE", "Keine Wohnung in Deutschland", null, null, null, "19530617", null }};

    private EidAuthAdapter out;

    @BeforeEach
    void setup() {
        out = new EidAuthAdapter(msConf1, msConf2, micrometer);
    }

    PersonalDataType personal(int i) {
        var data = PERSONS[i];
        PersonalDataType result = Mockito.mock(PersonalDataType.class);
        RestrictedIDType pseudonym = Mockito.mock(RestrictedIDType.class);
        Mockito.when(result.getRestrictedID()).thenReturn(pseudonym);
        byte[] idData = data[0].getBytes();
        Mockito.when(pseudonym.getID()).thenReturn(idData);
        Mockito.when(result.getGivenNames()).thenReturn(data[0]);
        Mockito.when(result.getFamilyNames()).thenReturn(data[1]);
        Mockito.when(result.getBirthName()).thenReturn(data[2]);
        Mockito.when(result.getNationality()).thenReturn(data[3]);
        GeneralPlaceType generalPlace = Mockito.mock(GeneralPlaceType.class);
        Mockito.when(result.getPlaceOfBirth()).thenReturn(generalPlace);
        Mockito.when(generalPlace.getFreetextPlace()).thenReturn(data[4]);
        GeneralPlaceType residence =
        switch (data[5]) {
            case "FREE": {
                var t = Mockito.mock(GeneralPlaceType.class);
                Mockito.when(t.getFreetextPlace()).thenReturn(data[6]);
                yield t;
            }
            case "STRUCTURED": {
                var t = Mockito.mock(GeneralPlaceType.class);
                var p = Mockito.mock(PlaceType.class);
                Mockito.when(t.getStructuredPlace()).thenReturn(p);
                Mockito.when(p.getStreet()).thenReturn(data[6]);
                Mockito.when(p.getZipCode()).thenReturn(data[7]);
                Mockito.when(p.getCity()).thenReturn(data[8]);
                Mockito.when(p.getState()).thenReturn(data[9]);
                Mockito.when(p.getCountry()).thenReturn(data[11]);
                yield t;
            }
            default: yield null;
        };
        Mockito.when(result.getPlaceOfResidence()).thenReturn(residence);
        GeneralDateType birthDate = Mockito.mock(GeneralDateType.class);
        Mockito.when(result.getDateOfBirth()).thenReturn(birthDate);
        Mockito.when(birthDate.getDateString()).thenReturn(data[10]);
        return result;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void test_mapResponse_ok(int i) {
        var relayState = "relayState";
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getSamlId()).thenReturn(relayState);
        ProcessedSamlResult processed = Mockito.mock(ProcessedSamlResult.class);
        var personalData = personal(i);
        Mockito.when(processed.getPersonalData()).thenReturn(personalData);

        var result = out.mapEidResponse(relayState, auth, processed);

        Assertions.assertAll(
                () -> Assertions.assertNotNull(result),
                () -> Assertions.assertEquals(PERSONS[i][0], result.givenNames()),
                () -> Assertions.assertEquals(PERSONS[i][1], result.familyNames()),
                () -> Assertions.assertEquals(PERSONS[i][2], result.birthName()),
                () -> Assertions.assertEquals(PERSONS[i][4], result.placeOfBirth()),
                () -> Assertions.assertEquals(PERSONS[i][10], result.dateOfBirth())
        );
        switch (PERSONS[i][5]) {
            case "FREE":
                Assertions.assertEquals(PERSONS[i][6], result.residence().freeText());
                break;
            case "STRUCTURED":
                Assertions.assertEquals(PERSONS[i][6], result.residence().street());
                Assertions.assertEquals("DE", result.residence().country());
                break;
            default:
        }
    }

    @Test
    void test_createSamlRedirectBinding_badConfig() {
        var sessionId = "session";
        var responseUrl = "https://return.to/saml";
        Mockito.when(msConf1.getResponseUrl()).thenReturn(responseUrl);
        SamlConfiguration configurationMock = Mockito.mock(SamlConfiguration.class);
        Mockito.when(msConf1.getConfigurations()).thenReturn(List.of(configurationMock));

        assertThrows(CryptoConfigException.class, () -> out.createSamlRedirectBindingUrl(sessionId, responseUrl));

    }

}
