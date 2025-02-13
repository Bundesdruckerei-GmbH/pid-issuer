/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WSessionManagement;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdentificationResultReceiver implements IdentificationApi.IdentificationResultCallback {
    static final String RESULT_OK = "Success";
    static final String RESULT_ERROR = "Error";

    private final SessionManager sessionManager;
    private final PidSerializer pidSerializer;

    public IdentificationResultReceiver(SessionManager sessionManager, PidSerializer pidSerializer) {
        this.sessionManager = sessionManager;
        this.pidSerializer = pidSerializer;
    }

    @Override
    public void identificationError(String issuerState, String errorDescription) {
        log.warn("Error in identification {}, {}", issuerState, errorDescription);
        var session = sessionManager.loadByIssuerState(issuerState);
        log.debug("loaded");
        if (!session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT)) {
            throw InvalidRequestException.forWrongRequestOrder(Requests.IDENTIFICATION_RESULT);
        }
        if (session.containsParameter(SessionKey.IDENTIFICATION_RESULT)) {
            throw new IllegalStateException("the session has already received a result");
        }

        session.putParameter(SessionKey.IDENTIFICATION_RESULT, RESULT_ERROR);
        session.putParameter(SessionKey.IDENTIFICATION_ERROR, errorDescription);

        ((WSessionManagement) session).setNextExpectedRequest(Requests.FINISH_AUTHORIZATION_REQUEST);
        log.debug("set");
        sessionManager.persist(session);
        log.debug("stored");
    }

    @Override
    public void successfulIdentification(String issuerState, PidCredentialData pidData) {
        log.info("Successful identification {}, {}", issuerState, pidData);
        var session = sessionManager.loadByIssuerState(issuerState);
        log.debug("loaded");
        if (!session.isNextAllowedRequest(Requests.IDENTIFICATION_RESULT)) {
            throw InvalidRequestException.forWrongRequestOrder(Requests.IDENTIFICATION_RESULT);
        }
        if (session.containsParameter(SessionKey.IDENTIFICATION_RESULT)) {
            throw new IllegalStateException("the session has already received a result");
        }
        log.debug("before serialization");
        var serialized = this.pidSerializer.toString(pidData);
        log.debug("after serialization");
        session.putParameter(SessionKey.IDENTIFICATION_RESULT, RESULT_OK);
        session.putParameter(SessionKey.IDENTIFICATION_DATA, serialized);

        ((WSessionManagement) session).setNextExpectedRequest(Requests.FINISH_AUTHORIZATION_REQUEST);
        log.debug("set");
        sessionManager.persist(session);
        log.debug("stored");
    }
}
