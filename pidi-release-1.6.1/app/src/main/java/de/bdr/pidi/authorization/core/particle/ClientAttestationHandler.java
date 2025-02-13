/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jwt.SignedJWT;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.openid4vc.vci.service.attestation.AttestationConstants;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidClientException;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import de.bdr.pidi.walletattestation.ClientAttestationJwt;
import de.bdr.pidi.walletattestation.ClientAttestationPopJwt;
import de.bdr.pidi.walletattestation.WalletAttestationRequest;
import de.bdr.pidi.walletattestation.WalletAttestationService;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * prerequisites: clientIdMatchHandler must have been run
 */
public class ClientAttestationHandler implements OidHandler {

    private static final String CLIENT_ASSERTION_KEY = "client_assertion";
    private static final String CLIENT_ASSERTION_TYPE_KEY = "client_assertion_type";

    private static final int CLIENT_ATTESTATION_JWT_INDEX = 0;
    private static final int CLIENT_ATTESTATION_POP_JWT_INDEX = 1;

    private final WalletAttestationService walletAttestationService;
    private final String issuerIdentifier;

    public ClientAttestationHandler(WalletAttestationService walletAttestationService, String issuerIdentifier) {
        this.walletAttestationService = walletAttestationService;
        this.issuerIdentifier = issuerIdentifier;
    }

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        processRequest(request, session);
    }

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        processRequest(request, session);
    }

    @Override
    public void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        // check pop jwt with c_nonce -> Handler is currently not used and will receive new requirements in the future
    }

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        processRequest(request, session);
    }

    private void processRequest(HttpRequest<?> request, WSession session) {
        validateClientAssertionKeyParameter(request.getParameters());
        var parts = validateClientAssertionParameter(request.getParameters());

        var clientId = session.getParameter(SessionKey.CLIENT_ID);
        try {
            var caJwt = ClientAttestationJwt.from(parts.get(CLIENT_ATTESTATION_JWT_INDEX));
            var capJwt = ClientAttestationPopJwt.from(parts.get(CLIENT_ATTESTATION_POP_JWT_INDEX));
            var walletAttestationRequest = new WalletAttestationRequest(caJwt, capJwt, clientId, issuerIdentifier);
            walletAttestationService.isValidWallet(walletAttestationRequest);
            session.putParameter(SessionKey.CLIENT_INSTANCE_KEY, caJwt.cnf().getJwk().toJSONString());
        } catch (ParseException e) {
            throw new InvalidClientException("Client attestation jwt is corrupted", e);
        } catch (IllegalArgumentException e) {
            throw new InvalidClientException("Client attestation jwt is invalid, " + e.getMessage(), e);
        }
    }

    private void validateClientAssertionKeyParameter(Map<String, String> params) {
        if (!params.containsKey(CLIENT_ASSERTION_TYPE_KEY)) {
            throw new InvalidClientException("Client assertion type is missing");
        }
        var clientAssertionType = params.get(CLIENT_ASSERTION_TYPE_KEY);
        if (clientAssertionType == null || clientAssertionType.isEmpty()) {
            throw new InvalidClientException("Client assertion type is empty");
        }
        if (!clientAssertionType.equals(AttestationConstants.CLIENT_ASSERTION_TYPE)) {
            throw new InvalidClientException("Client assertion type is invalid");
        }
    }

    private List<SignedJWT> validateClientAssertionParameter(Map<String, String> params) {
        if (!params.containsKey(CLIENT_ASSERTION_KEY)) {
            throw new InvalidClientException("Client assertion is missing");
        }
        var clientAssertion = params.get(CLIENT_ASSERTION_KEY);
        if (clientAssertion == null || clientAssertion.isEmpty()) {
            throw new InvalidClientException("Client assertion is empty");
        }
        var parts = clientAssertion.split("~");
        if (parts.length != 2) {
            throw new InvalidClientException("Client assertion length is invalid");
        }

        return Arrays.stream(parts).map(this::parseSignedJwt).toList();
    }

    private SignedJWT parseSignedJwt(String jwt) {
        try {
            return SignedJWT.parse(jwt);
        } catch (ParseException e) {
            throw new InvalidClientException("Client assertion could not be parsed", e);
        }
    }
}
