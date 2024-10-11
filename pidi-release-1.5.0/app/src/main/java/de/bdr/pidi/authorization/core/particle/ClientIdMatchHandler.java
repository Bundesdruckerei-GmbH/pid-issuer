/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public class ClientIdMatchHandler implements OidHandler {
    public static final String CLIENT_ID = "client_id";
    private final ClientConfigurationService clientConfigurationService;

    public ClientIdMatchHandler(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
    }

    @Override
    public void processAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session, boolean referencesPushedAuthRequest) {
        UUID clientIdFromRequest = getAndValidateClientId(request.getParameters());
        UUID clientIdFromSession = UUID.fromString(session.getParameter(SessionKey.CLIENT_ID));

        if(!clientIdFromRequest.equals(clientIdFromSession)){
            throw new InvalidRequestException("client_id parameter from par request doesn't match client_id");
        }
    }

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        getAndSaveClientId(request, session);
    }

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        getAndSaveClientId(request, session);
    }

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        getAndSaveClientId(request, session);
    }

    private void getAndSaveClientId(HttpRequest<?> request, WSession session) {
        UUID clientId = getAndValidateClientId(request.getParameters());
        MDC.put(SessionKey.CLIENT_ID.getValue(), clientId.toString());
        session.putParameter(SessionKey.CLIENT_ID, clientId.toString());
    }

    private UUID getAndValidateClientId(Map<String, String> parameters) {
        UUID clientId = validateClientIdParameter(parameters);
        if (!clientConfigurationService.isValidClientId(clientId)) {
            throw new ClientNotRegisteredException("Client Id not registered: " + clientId);
        }
        return clientId;
    }

    private UUID validateClientIdParameter(Map<String, String> params) {
        if (!params.containsKey(CLIENT_ID)) {
            throw new ValidationFailedException(InvalidRequestException.missingParameter(CLIENT_ID));
        }
        String clientIdString = params.get(CLIENT_ID);
        if (clientIdString == null || clientIdString.isEmpty()) {
            throw new ValidationFailedException("Invalid client id", "client id must not be empty");
        }
        try {
            return UUID.fromString(clientIdString);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException("Invalid client id", "client id must be a valid UUID");
        }
    }

}
