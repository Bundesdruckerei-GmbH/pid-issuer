/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.rest;

import de.bdr.revocation.identification.adapter.in.rest.api.EidApi;
import de.bdr.revocation.identification.adapter.in.rest.api.model.AuthenticationUrlResponse;
import de.bdr.revocation.identification.adapter.in.rest.api.model.LoggedInResponse;
import de.bdr.revocation.identification.core.AuthenticationService;
import de.bdr.revocation.identification.core.IdentificationException;
import de.bdr.revocation.identification.core.configuration.Tr03124Configuration;
import de.bdr.revocation.identification.core.exception.SamlRequestException;
import de.bdr.revocation.identification.core.exception.SamlResponseException;
import de.bdr.revocation.identification.core.model.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class AuthController implements EidApi {

    private final AuthenticationService authenticationService;
    private final AuthenticationSupport authenticationSupport;
    private final ServletSupport servletSupport;

    private final Tr03124Configuration tr03124Configuration;

    @Override
    public ResponseEntity<AuthenticationUrlResponse> getAuthenticationUrl() {
        var auth = this.authenticationService.initializeAuthentication();

        var headers = authenticationSupport.createCacheControlHeaders();
        var body = buildAuthenticationLinkWithSecondFactor(auth);

        return ResponseEntity.ok().headers(headers).body(body);
    }

    @Override
    public ResponseEntity<Void> getTcTokenRedirectUrl(String auth) {
        try {
            var authToken = authenticationSupport.extractSingleParameterValue(servletSupport.getServletRequest(),
                    tr03124Configuration.getAuthParam());
            var samlRedirectUrl = authenticationService.createSamlRedirectBindingUrl(authToken);
            log.trace("XXX samlRedirectUrl: {}", samlRedirectUrl);

            HttpHeaders headers = authenticationSupport.createRedirectHeaders(samlRedirectUrl);

            return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        } catch (IdentificationException me) {
            throw authenticationSupport.handleInSamlProcessing(me, true);
        } catch (RuntimeException re) {
            throw new SamlRequestException("SAML request failed", re);
        }
    }

    @Override
    public ResponseEntity<Void> getSamlConsumer(String samlResponse, String relayState, String sigAlg, String signature) {
        try {
            var request = servletSupport.getServletRequest();
            log.debug("header-host: {}", request.getHeader(HttpHeaders.HOST));
            log.debug("header-x-forwarded-host: {}", request.getHeader("X-Forwarded-Host"));

            var referenceId = authenticationService
                    .receiveSamlResponse(relayState, samlResponse, sigAlg, signature);

            String redirectUrl = tr03124Configuration.createLoggedInUrl(referenceId);

            HttpHeaders headers = authenticationSupport.createRedirectHeaders(redirectUrl);

            return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        } catch (IdentificationException me) {
            throw authenticationSupport.handleInSamlProcessing(me, false);
        } catch (RuntimeException re) {
            throw new SamlResponseException("SAML response failed", re);
        }
    }

    @Override
    public ResponseEntity<LoggedInResponse> finishLogin(String referenceId, String xSessionID) {
        xSessionID = authenticationSupport.parseSessionIdFromHeader(servletSupport.getServletRequest(), xSessionID);
        var authentication = authenticationService.retrieveAuth(xSessionID, referenceId);

        authenticationService.finishAuthentication(authentication);
        return toResponse(authentication);
    }

    private ResponseEntity<LoggedInResponse> toResponse(Authentication authentication) {
        return ResponseEntity.ok(new LoggedInResponse(authentication.getSessionId(), authentication.getRemainingValidityInSeconds()));
    }

    @Override
    public ResponseEntity<LoggedInResponse> refreshSession(String xSessionID) {
        xSessionID = authenticationSupport.parseSessionIdFromHeader(servletSupport.getServletRequest(), xSessionID);
        var authentication = authenticationService.retrieveAuth(xSessionID, null);

        authenticationService.extendAuthenticationValidity(authentication);
        return toResponse(authentication);
    }

    @Override
    public ResponseEntity<Void> terminateAuthentication(String xSessionID) {
        xSessionID = authenticationSupport.parseSessionIdFromHeader(servletSupport.getServletRequest(), xSessionID);
        authenticationService.terminateAuthentication(xSessionID);

        HttpHeaders headers = authenticationSupport.createCacheControlHeaders();
        return ResponseEntity.noContent().headers(headers).build();
    }

    private AuthenticationUrlResponse buildAuthenticationLinkWithSecondFactor(Authentication auth) {
        var authLink = this.tr03124Configuration.createAuthenticationLink(auth);
        return new AuthenticationUrlResponse(authLink, auth.getSessionId(), auth.getRemainingValidityInSeconds());
    }
}
