/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.flows;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.FlowController;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionManagement;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.FinishAuthException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.particle.OidHandler;
import de.bdr.pidi.base.PidServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

import java.util.List;

@Slf4j
public abstract class BaseFlowController extends FlowController {

    protected static final String PROCESSING_MSG = "Processing {} request {}";

    protected final FlowVariant flowVariant;
    protected final String authorizationScheme;

    protected BaseFlowController(SessionManager sm, AuthorizationConfiguration authorizationConfiguration,
                                 List<OidHandler> oidHandlers, FlowVariant flowVariant) {
        super(sm, oidHandlers);
        this.flowVariant = flowVariant;
        this.authorizationScheme = authorizationConfiguration.getAuthorizationScheme();
    }

    @Override
    public WResponseBuilder processPushedAuthRequest(HttpRequest<?> request) {
        final WSession session;
        try {
            session = sm.init(flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "pushed auth", request.getParameters());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessPushedAuthRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);
        } finally {
            sm.persist(session);
        }
        return builder;
    }

    @Override
    public WResponseBuilder processAuthRequest(HttpRequest<?> request) {
        final WSession session;
        try {
            var requestUri = request.getParameters().get("request_uri");
            session = sm.loadByRequestUri(requestUri, flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "auth", request.getParameters());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessAuthRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(Requests.IDENTIFICATION_RESULT);
        } finally {
            sm.persist(session);
        }
        return builder;
    }

    @Override
    public WResponseBuilder processFinishAuthRequest(HttpRequest<?> request) {
        final WSession session;
        try {
            var issuerState = request.getParameters().get("issuer_state");
            session = sm.loadByIssuerState(issuerState, flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "finish auth", request.getBody());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessFinishAuthRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(Requests.TOKEN_REQUEST);
        } catch (OIDException | PidServerException e) {
            var givenUri = session.getParameter(SessionKey.REDIRECT_URI);
            var state = session.getParameter(SessionKey.STATE);
            throw new FinishAuthException(givenUri, state, e);
        } finally {
            sm.persist(session);
        }
        return builder;
    }

    @Override
    public WResponseBuilder processTokenRequest(HttpRequest<?> request) {
        return processTokenRequest(request, Requests.CREDENTIAL_REQUEST);
    }

    protected WResponseBuilder processTokenRequest(HttpRequest<?> request, Requests nextExpectedRequest) {
        final WSession session;
        try {
            var authCode = request.getParameters().get("code");
            session = sm.loadByAuthorizationCode(authCode, flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "token", request.getParameters());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessTokenRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(nextExpectedRequest);
        } finally {
            sm.persist(session);
        }
        return builder;
    }

    @Override
    public WResponseBuilder processCredentialRequest(HttpRequest<CredentialRequest> request) {
        return processCredentialRequest(request, Requests.CREDENTIAL_REQUEST);
    }

    protected WResponseBuilder processCredentialRequest(HttpRequest<CredentialRequest> request, Requests nextExpectedRequest) {
        final WSession session;
        try {
            var authorization = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
            session = sm.loadByAccessToken(authorizationScheme, authorization, flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "credential", request.getBody());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessCredentialRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(nextExpectedRequest);
        } finally {
            sm.persist(session);
        }
        return builder;
    }
}
