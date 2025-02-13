/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static de.bdr.pidi.testdata.ValidTestData.CODE_CHALLANGE;
import static de.bdr.pidi.testdata.ValidTestData.CODE_VERIFIER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PkceHandlerTest {

    @Mock
    HttpRequest<?> request;
    @Mock
    WResponseBuilder response;
    @Mock
    WSession session;

    PkceHandler out;

    @BeforeEach
    void setUp() {
        out = new PkceHandler();
    }

    public static void initValidAuthRequestParams(Map<String, String> params) {
        assertNotNull(params);

        params.put("code_challenge", CODE_CHALLANGE);
        params.put("code_challenge_method", "S256");
    }

    public static void initInvalidAuthRequestParams(Map<String, String> params) {
        assertNotNull(params);

        params.put("code_challenge", "invalidCodeChallange?}&$!");
        params.put("code_challenge_method", "S256");
    }

    public static void initMissingAuthRequestParams(Map<String, String> params) {
        assertNotNull(params);

        params.remove("code_challenge");
        params.remove("code_challenge_method");
    }

    public static void initValidTokenRequestParams(Map<String, String> params) {
        assertNotNull(params);

        params.put("code_verifier", CODE_VERIFIER);
    }

    public static void initInvalidTokenRequestParams(Map<String, String> params) {
        assertNotNull(params);

        params.put("code_verifier", "invalidCodeVerifier");
    }

    public static void initMissingTokenRequestParams(Map<String, String> params) {
        assertNotNull(params);

        params.remove("code_verifier");
    }

    @Test
    void testProcessPushedAuthRequest_ok() {
        var map = new HashMap<String, String>();
        when(request.getParameters()).thenReturn(map);

        initValidAuthRequestParams(map);

        assertDoesNotThrow(() -> out.processPushedAuthRequest(request, response, session));
    }

    @Test
    void testProcessPushedAuthRequest_invalidParameters() {
        var map = new HashMap<String, String>();
        when(request.getParameters()).thenReturn(map);

        initInvalidAuthRequestParams(map);

        var e1 = assertThrows(ValidationFailedException.class, () -> out.processPushedAuthRequest(request, response, session));
        assertEquals("Invalid code challenge", e1.getMessage());
        assertEquals("Invalid code challenge base64", e1.getLogMessage());

        map.put("code_challenge", "invalidCodeChallange");
        var e2 = assertThrows(ValidationFailedException.class, () -> out.processPushedAuthRequest(request, response, session));
        assertEquals("Invalid code challenge", e2.getMessage());
        assertEquals("Invalid code challenge length", e2.getLogMessage());

        map.put("code_challenge_method", "4711");
        var e3 = assertThrows(ValidationFailedException.class, () -> out.processPushedAuthRequest(request, response, session));
        assertEquals("Invalid code challenge method", e3.getMessage());
        assertEquals("Invalid code challenge method", e3.getLogMessage());
    }

    @Test
    void testProcessPushedAuthRequest_missingParameters() {
        var map = new HashMap<String, String>();
        when(request.getParameters()).thenReturn(map);

        initMissingAuthRequestParams(map);

        assertThrows(ValidationFailedException.class, () -> out.processPushedAuthRequest(request, response, session));
    }

    @Test
    void testProcessAuthRequest_ok() {
        var map = new HashMap<String, String>();
        when(request.getParameters()).thenReturn(map);

        initValidAuthRequestParams(map);

        assertDoesNotThrow(() -> out.processAuthRequest(request, response, session, false));
        assertDoesNotThrow(() -> out.processAuthRequest(request, response, session, true));
    }

    @Test
    void testProcessTokenRequest_ok() {
        var map = new HashMap<String, String>();
        initValidTokenRequestParams(map);

        when(request.getParameters()).thenReturn(map);
        when(session.getParameter(SessionKey.CODE_CHALLENGE)).thenReturn(CODE_CHALLANGE);

        assertDoesNotThrow(() -> out.processTokenRequest(request, response, session));
    }

    @Test
    void testProcessTokenRequest_invalidParameters() {
        var map = new HashMap<String, String>();
        initInvalidTokenRequestParams(map);

        when(request.getParameters()).thenReturn(map);

        assertThrows(ValidationFailedException.class, () -> out.processTokenRequest(request, response, session));
    }

    @Test
    void testProcessTokenRequest_missingParameters() {
        var map = new HashMap<String, String>();
        initMissingTokenRequestParams(map);

        when(request.getParameters()).thenReturn(map);

        assertThrows(ValidationFailedException.class, () -> out.processTokenRequest(request, response, session));
    }

    @Test
    void testProcessTokenRequest_verificationError() {
        var map = new HashMap<String, String>();
        initValidTokenRequestParams(map);

        when(request.getParameters()).thenReturn(map);
        when(session.getParameter(SessionKey.CODE_CHALLENGE)).thenReturn("blabla");

        assertThrows(VerificationFailedException.class, () -> out.processTokenRequest(request, response, session));
    }
}
