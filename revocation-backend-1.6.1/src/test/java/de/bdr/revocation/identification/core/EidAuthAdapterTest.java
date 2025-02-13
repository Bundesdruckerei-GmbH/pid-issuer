/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.core.configuration.MultiSamlConfiguration;
import de.bdr.revocation.identification.core.exception.CryptoConfigException;
import de.bdr.revocation.identification.core.exception.SamlResponseValidationFailedException;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bund.bsi.eid240.PersonalDataType;
import de.bund.bsi.eid240.RestrictedIDType;
import de.governikus.panstar.sdk.saml.configuration.SamlConfiguration;
import de.governikus.panstar.sdk.saml.response.ProcessedSamlResult;
import de.governikus.panstar.sdk.utils.saml.SAMLUtils;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.bdr.revocation.identification.core.model.AuthenticationState.STARTED;
import static de.bdr.revocation.identification.core.model.ModelTestData.defaultAuthentication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
@ExtendWith(MockitoExtension.class)
class EidAuthAdapterTest {

    @Mock
    private MultiSamlConfiguration msConf;

    static final String[][] PERSONS = {
            {"Erika", "Mustermann", "Gabler", "D", "Köln", "STRUCTURED", "Hauptstraße 1", "21614", "Buxtehude", "Niedersachsen", "19901003","D" },
            {"John", "Doe", null, "US", "Washington", "FREE", "Keine Wohnung in Deutschland", null, null, null, "19530617", null },
            {"John", "Doe", null, "US", "Washington", "NO", "Gar keine Wohnung", null, null, null, "19530617", null }
    };

    private EidAuthAdapter out;

    @BeforeEach
    void setup() {
        out = new EidAuthAdapter(msConf);
    }

    PersonalDataType personal(int i) {
        var data = PERSONS[i];
        PersonalDataType result = Mockito.mock(PersonalDataType.class);
        RestrictedIDType pseudonym = Mockito.mock(RestrictedIDType.class);
        Mockito.when(result.getRestrictedID()).thenReturn(pseudonym);
        byte[] idData = data[0].getBytes();
        Mockito.when(pseudonym.getID()).thenReturn(idData);
        return result;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void test_mapResponse_ok(int i) {
        var relayState = "relayState";
        de.bdr.revocation.identification.core.model.Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getSamlId()).thenReturn(relayState);
        ProcessedSamlResult processed = Mockito.mock(ProcessedSamlResult.class);
        var personalData = personal(i);
        Mockito.when(processed.getPersonalData()).thenReturn(personalData);

        var result = out.mapEidResponse(relayState, auth, processed);

        Assertions.assertAll(
                () -> Assertions.assertNotNull(result),
                () -> Assertions.assertEquals(PERSONS[i][0], decode(result.pseudonym()))
        );
    }

    private static String decode(String s) {
        return new String(Base64.getDecoder().decode(s));
    }

    @Test
    void test_createSamlRedirectBinding_badConfig() {
        var sessionId = "session";
        SamlConfiguration configurationMock = Mockito.mock(SamlConfiguration.class);
        Mockito.when(msConf.getConfigurations()).thenReturn(List.of(configurationMock));

        assertThrows(CryptoConfigException.class, () -> out.createSamlRedirectBindingUrl(sessionId));
    }

    @Test
    void when_no_certificate_is_valid_then_fail_with_exception() {
        Authentication expectedAuthentication = defaultAuthentication().authenticationState(STARTED).build();

        try (MockedStatic<SAMLUtils> samlSdkHelper = Mockito.mockStatic(SAMLUtils.class)) {
            samlSdkHelper.when(() -> SAMLUtils
                            .checkQuerySignature(anyString(), any(), anyString(), anyString(), any(X509Certificate.class), eq(false)))
                    .thenCallRealMethod();
            var exception = assertThrows(SamlResponseValidationFailedException.class, () -> out.validateSamlResponseAndExtractPseudonym(
                    "TestSamlId",
                    "TestSamlResponse",
                    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", "signature", expectedAuthentication));

            assertEquals("no certificate found that validates signature", exception.getMessage());
        }
    }
}