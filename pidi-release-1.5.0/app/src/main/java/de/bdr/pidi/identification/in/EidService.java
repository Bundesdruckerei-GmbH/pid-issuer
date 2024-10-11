/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.in;

import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.identification.core.AuthenticationService;
import de.bdr.pidi.identification.core.configuration.InfoConfiguration;
import de.bdr.pidi.identification.core.configuration.Tr03124Configuration;
import de.bdr.pidi.identification.core.model.Authentication;
import de.bdr.pidi.identification.in.api.EidApi;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Slf4j
@RestController
@PrimaryAdapter
public class EidService implements IdentificationApi, EidApi {
    private static final String MDC_SESSION_ID = "sessionId";

    private final AuthenticationService authenticationService;
    private final Tr03124Configuration tr03124Configuration;
    private final IdentificationResultCallback identificationResultCallback;
    private final PidCredentialDataMapper pidCredentialDataMapper;

    public EidService(AuthenticationService authenticationService,
                      @Qualifier("infoV2") InfoConfiguration infoConfiguration,
                      IdentificationResultCallback identificationResultCallback,
                      PidCredentialDataMapper pidCredentialDataMapper) {
        this.authenticationService = authenticationService;
        this.tr03124Configuration = infoConfiguration;
        this.identificationResultCallback = identificationResultCallback;
        this.pidCredentialDataMapper = pidCredentialDataMapper;
    }

    @Timed
    @Override
    public URL startIdentificationProcess(URL redirectUrl, String issuerState, String sessionId) {
        Authentication auth = authenticationService.initializeAuthentication(issuerState, redirectUrl, sessionId);
        var authToken = auth.getTokenId();
        var responseUrl = tr03124Configuration.createSamlConsumerUrl();
        var samlRedirectUrl = authenticationService.createSamlRedirectBindingUrl(authToken, responseUrl);
        try {
            return URI.create(samlRedirectUrl).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Timed
    @Override
    public ResponseEntity<Void> getSamlConsumer(String saMLResponse, String relayState, String sigAlg, String signature) {
        log.info("getSamlConsumer called with re");
        var authentication = authenticationService.loadBySamlId(relayState);
        MDC.put(MDC_SESSION_ID, String.valueOf(authentication.getSessionId()));
        log.debug("auth loaded");
        try {
            var result = authenticationService.receiveSamlResponse(relayState, saMLResponse, sigAlg, signature, tr03124Configuration.createSamlConsumerUrl());
            log.debug("decoded");
            identificationResultCallback.successfulIdentification(authentication.getExternalId(), pidCredentialDataMapper.map(result));
            log.debug("after callback");
        } catch (Exception e) {
            log.info("Failure in eID identification {}", authentication.getExternalId(), e);
            identificationResultCallback.identificationError(authentication.getExternalId(), e.getMessage());
            log.debug("after failure");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, authentication.getRedirectUrl().toString());
        log.debug("done");
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}
