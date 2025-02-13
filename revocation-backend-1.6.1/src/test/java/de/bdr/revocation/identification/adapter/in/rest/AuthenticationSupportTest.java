/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.rest;

import de.bdr.revocation.identification.core.IdentificationException;
import de.bdr.revocation.identification.core.exception.SamlCryptoConfigException;
import de.bdr.revocation.identification.core.exception.SamlRequestException;
import de.bdr.revocation.identification.core.exception.SamlResponseException;
import de.bdr.revocation.identification.core.exception.SessionValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AuthenticationSupportTest {

    @InjectMocks
    AuthenticationSupport out;

    @Test
    void when_createTextHeaders_then_valid_result() {
        var headers = out.createCacheControlHeaders();
        assertEquals("no-store", headers.getCacheControl());
    }

    @Test
    void when_createRedirectHeaders_then_valid_result() {
        var testUrl = "TestUrl";
        var headers = out.createRedirectHeaders(testUrl);
        assertEquals(URI.create(testUrl), headers.getLocation());
    }

    @Test
    void given_one_parameter_value_when_extractSingleParameterValue_then_returns_parameter() {
        var testRequest = new MockHttpServletRequest();
        var paramName = "TestParam";
        var paramValue = "TestValue";
        testRequest.addParameter(paramName, paramValue);
        assertEquals(paramValue, out.extractSingleParameterValue(testRequest, paramName));
    }

    @Test
    void given_no_parameter_value_when_extractSingleParameterValue_then_exception() {
        var testRequest = new MockHttpServletRequest();
        var paramName = "TestParam";
        assertThrows(SessionValidationException.class, () -> out.extractSingleParameterValue(testRequest, paramName));
    }

    @Test
    void given_too_many_parameter_values_when_extractSingleParameterValue_then_exception() {
        var testRequest = new MockHttpServletRequest();
        var paramName = "TestParam";
        var paramValue = "TestValue";
        var unwantedParamValue = "Superfluous";
        testRequest.addParameter(paramName, paramValue, unwantedParamValue);
        assertThrows(SessionValidationException.class, () -> out.extractSingleParameterValue(testRequest, paramName));
    }

    @Test
    void given_no_header_when_parseSessionIdFromHeader_then_null() {
        var request = new MockHttpServletRequest();

        assertThrows(SessionValidationException.class, () -> out.parseSessionIdFromHeader(request, null));
    }

    @Test
    void given_no_header_but_parameter_when_parseSessionIdFromHeader_then_exception() {
        var request = new MockHttpServletRequest();

        assertThrows(SessionValidationException.class, () -> out.parseSessionIdFromHeader(request, "abc"));
    }

    @Test
    void given_header_when_parseSessionIdFromHeader_then_return_parameter() {
        var request = new MockHttpServletRequest();
        var id = "Sers";
        request.addHeader(AuthenticationSupport.SESSION_ID_HEADER, id);

        assertEquals(id, out.parseSessionIdFromHeader(request, id));
    }

    @Test
    void given_header_with_different_parameter_when_parseSessionIdFromHeader_then_exception() {
        var request = new MockHttpServletRequest();
        request.addHeader(AuthenticationSupport.SESSION_ID_HEADER, "Sers");

        assertThrows(SessionValidationException.class, () -> out.parseSessionIdFromHeader(request, "Wombat"));
    }

    @Test
    void given_duplicate_header_when_parseSessionIdFromHeader_then_exception() {
        var request = new MockHttpServletRequest();
        var id = "Sers";
        var otherId = "Dude";
        request.addHeader(AuthenticationSupport.SESSION_ID_HEADER, id);
        request.addHeader(AuthenticationSupport.SESSION_ID_HEADER, otherId);

        assertThrows(SessionValidationException.class, () -> out.parseSessionIdFromHeader(request, id));
    }

    /**
     * too short, too long, invalid characters
     */
    @ParameterizedTest
    @ValueSource( strings = {"",
            "a12345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "Sers:23'\\"
    })
    void given_invalid_header_when_parseSessionIdFromHeader_then_exception(String id) {
        var request = new MockHttpServletRequest();
        request.addHeader(AuthenticationSupport.SESSION_ID_HEADER, id);

        assertThrows(SessionValidationException.class, () -> out.parseSessionIdFromHeader(request, id));
    }

    @Test
    void given_header_max_when_parseSessionIdFromHeader_then_return() {
        var request = new MockHttpServletRequest();
        var id = "Ab3-Dzg_90123456789012345678901234567890123456789012345678901234567890123456=%3D";
        request.addHeader(AuthenticationSupport.SESSION_ID_HEADER, id);

        assertEquals(id, out.parseSessionIdFromHeader(request, id));
    }

    @Test
    void given_EidWrappingException_when_handleInSamlProcessing_then_return_EidWrappingException() {
        var exception = new SamlCryptoConfigException("Boohoo", false);
        assertEquals(exception, out.handleInSamlProcessing(exception, false));
    }

    @Test
    void given_no_EidWrappingException_in_request_when_handleInSamlProcessing_then_return_SamlRequestException() {
        var exception = new IdentificationException("Boohoo");
        assertInstanceOf(SamlRequestException.class, out.handleInSamlProcessing(exception, true));
    }

    @Test
    void given_no_EidWrappingException_not_in_request_when_handleInSamlProcessing_then_return_SamlRequestException() {
        var exception = new IdentificationException("Boohoo");
        assertInstanceOf(SamlResponseException.class, out.handleInSamlProcessing(exception, false));
    }
}
