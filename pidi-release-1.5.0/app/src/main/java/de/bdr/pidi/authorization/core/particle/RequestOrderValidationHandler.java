/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.base.requests.SeedCredentialRequest;

public class RequestOrderValidationHandler implements OidHandler {

    private void validateRequest(WSession session, Requests request) {
        if (!session.isNextAllowedRequest(request)) {
            throw InvalidRequestException.forWrongRequestOrder(request);
        }
    }

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateRequest(session, Requests.PUSHED_AUTHORIZATION_REQUEST);
    }

    @Override
    public void processAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session, boolean referencesPushedAuthRequest) {
        validateRequest(session, Requests.AUTHORIZATION_REQUEST);
    }

    @Override
    public void processFinishAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateRequest(session, Requests.FINISH_AUTHORIZATION_REQUEST);
    }

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        if (!session.isNextAllowedRequest(Requests.TOKEN_REQUEST)) {
            throw new InvalidGrantException(Requests.TOKEN_REQUEST + " is not the allowed next request");
        }
    }

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateRequest(session, Requests.SEED_TOKEN_REQUEST);
    }

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        validateRequest(session, Requests.CREDENTIAL_REQUEST);
    }

    @Override
    public void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        validateRequest(session, Requests.SEED_CREDENTIAL_REQUEST);
    }

    @Override
    public void processPresentationSigningRequest(HttpRequest<PresentationSigningRequest> request, WResponseBuilder response, WSession session) {
        validateRequest(session, Requests.PRESENTATION_SIGNING_REQUEST);
    }
}
