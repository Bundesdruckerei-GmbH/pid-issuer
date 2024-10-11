/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.base.PidServerException;
import org.springframework.http.HttpStatus;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class AuthorizationHandler implements OidHandler {
    private final String scheme;
    private final String host;
    private final Integer port;
    private final IdentificationApi identificationProvider;

    public AuthorizationHandler(URL baseUrl, IdentificationApi identificationProvider) {
        this.scheme = baseUrl.getProtocol();
        this.host = baseUrl.getHost();
        this.port = baseUrl.getPort();
        this.identificationProvider = identificationProvider;
    }

    @Override
    public void processAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session, boolean referencesPushedAuthRequest) {
        String issuerState = RandomUtil.randomString();

        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        try {
            var accessCodeUrl = new URI(scheme, null, host, port,
                            "/" + Requests.FINISH_AUTHORIZATION_REQUEST.getPath(session.getFlowVariant()),
                            "issuer_state=" + issuerState, null).toURL();
            final String sessionId;
            if (session instanceof WSessionImpl sessionImpl) {
                sessionId = String.valueOf(sessionImpl.getSessionId());
            } else {
                sessionId = null;
            }
            var samlAuthRequestUrl = identificationProvider.startIdentificationProcess(accessCodeUrl, issuerState, sessionId);

            response.withHttpStatus(HttpStatus.SEE_OTHER.value())
                    .addStringHeader("Location", samlAuthRequestUrl.toString());
        } catch (URISyntaxException | MalformedURLException me) {
            throw new PidServerException("could not create accessCodeUrl", me);
        }

    }
}
