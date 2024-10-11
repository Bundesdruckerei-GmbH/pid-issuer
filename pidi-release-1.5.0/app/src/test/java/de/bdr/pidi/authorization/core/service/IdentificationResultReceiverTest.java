/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentificationResultReceiverTest {

    private static final String STATE_OK = "ok1a2b3c4d5e6f7g8h9I0j";
    private static final String STATE_FAIL = "fail1a2b3c4d5e6f7g8h9I";

    private PidCredentialData pid;

    @Mock
    private SessionManager sessionManager;
    @Mock
    private WSessionImpl session;
    @Mock
    private PidSerializer pidSerializer;

    private IdentificationResultReceiver out;

    @BeforeEach
    void setUp() {
        out = new IdentificationResultReceiver(sessionManager, pidSerializer);
        pid = ServiceTestData.createPid();
    }

    @Test
    @DisplayName("Successful identification, OK")
    void test001() {
        when(sessionManager.loadByIssuerState(STATE_OK))
                .thenReturn(session);
        when(session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT))
                .thenReturn(true);
        var serialized = "Serialized PID";
        when(pidSerializer.toString(pid)).thenReturn(serialized);

        out.successfulIdentification(STATE_OK, pid);

        assertAll(
                () -> Mockito.verify(session)
                        .putParameter(SessionKey.IDENTIFICATION_RESULT, IdentificationResultReceiver.RESULT_OK),
                () -> Mockito.verify(pidSerializer)
                        .toString(pid),
                () -> Mockito.verify(session)
                        .putParameter(SessionKey.IDENTIFICATION_DATA, serialized)
        );
    }

    @Test
    @DisplayName("Successful identification, bad state")
    void test002() {
        when(sessionManager.loadByIssuerState(STATE_FAIL))
                .thenReturn(session);
        when(session.containsParameter(SessionKey.IDENTIFICATION_RESULT))
                .thenReturn(true);
        when(session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT)).thenReturn(true);

        Assertions.assertThrows(IllegalStateException.class,
                () -> out.successfulIdentification(STATE_FAIL, pid));
    }

    @Test
    @DisplayName("Identification error, OK")
    void test003() {
        when(sessionManager.loadByIssuerState(STATE_OK))
                .thenReturn(session);
        when(session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT))
                .thenReturn(true);

        var message = "too bad";
        out.identificationError(STATE_OK, message);

        assertAll(
                () -> Mockito.verify(session)
                        .putParameter(SessionKey.IDENTIFICATION_RESULT, IdentificationResultReceiver.RESULT_ERROR),
                () -> Mockito.verify(session)
                        .putParameter(SessionKey.IDENTIFICATION_ERROR, message)
        );
    }

    @Test
    @DisplayName("Identification error, bad state")
    void test004() {
        when(sessionManager.loadByIssuerState(STATE_FAIL))
                .thenReturn(session);
        when(session.containsParameter(SessionKey.IDENTIFICATION_RESULT))
                .thenReturn(true);
        when(session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT))
                .thenReturn(true);
        Assertions.assertThrows(IllegalStateException.class,
                () -> out.identificationError(STATE_FAIL, "too bad"));
    }

    @Test
    @DisplayName("Identification error, wrong request order")
    void test005() {
        when(sessionManager.loadByIssuerState(STATE_OK))
                .thenReturn(session);
        when(session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT))
                .thenReturn(false);
        InvalidRequestException exception = Assertions.assertThrows(InvalidRequestException.class,
                () -> out.identificationError(STATE_OK, "too bad"));
        assertAll(
                () -> assertThat(exception.getErrorCode(), is("invalid_request")),
                () -> assertThat(exception.getLogMessage(), is("IDENTIFICATION_RESULT is not the allowed next request"))
        );
    }

    @Test
    @DisplayName("Identification success, wrong request order")
    void test006() {
        when(sessionManager.loadByIssuerState(STATE_OK))
                .thenReturn(session);
        when(session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT))
                .thenReturn(false);
        var testDataSet = PidCredentialData.Companion.getTEST_DATA_SET();
        InvalidRequestException exception = Assertions.assertThrows(InvalidRequestException.class,
                () -> out.successfulIdentification(STATE_OK, testDataSet));
        assertAll(
                () -> assertThat(exception.getErrorCode(), is("invalid_request")),
                () -> assertThat(exception.getLogMessage(), is("IDENTIFICATION_RESULT is not the allowed next request"))
        );
    }
}
