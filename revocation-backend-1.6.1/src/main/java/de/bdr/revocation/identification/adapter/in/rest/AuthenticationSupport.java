/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.rest;

import de.bdr.revocation.identification.core.exception.EidWrappingException;
import de.bdr.revocation.identification.core.exception.SamlRequestException;
import de.bdr.revocation.identification.core.exception.SamlResponseException;
import de.bdr.revocation.identification.core.exception.SessionValidationException;
import de.bdr.revocation.identification.core.IdentificationException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class AuthenticationSupport {

    private static final String MALFORMED_SESSION_HEADER = "Malformed session header";
    private static final String HEADER_MISMATCH = "Header mismatch";
    private static final int ID_MAX_LENGTH = 80;
    public static final String SESSION_ID_HEADER = "X-Session-ID";

    private final Pattern idParameterPattern = Pattern.compile("[a-zA-Z0-9_-]+(?:=|%3D|%3d){0,2}");

    public HttpHeaders createCacheControlHeaders() {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        return headers;
    }

    public HttpHeaders createRedirectHeaders(String redirectUrl) {
        var headers = createUncachedHeaders();
        headers.add(HttpHeaders.LOCATION, redirectUrl);
        return headers;
    }

    public HttpHeaders createUncachedHeaders() {
        var headers = new HttpHeaders();
        headers.setCacheControl("no-cache, no-store");
        headers.setPragma("no-cache");
        return headers;
    }

    public String extractSingleParameterValue(HttpServletRequest request, String parameterName) {
        String[] parameterValues = request.getParameterValues(parameterName);
        if (parameterValues == null || parameterValues.length != 1 || StringUtils.isEmpty(parameterValues[0])) {
            throw new SessionValidationException("Could not extract " + parameterName, "The request contained not exactly 1 value for this parameter.");
        }
        return parameterValues[0];
    }

    public String parseSessionIdFromHeader(HttpServletRequest request, String givenSessionId) {
        var ids = request.getHeaders(SESSION_ID_HEADER);
        if (!ids.hasMoreElements()) {
            throw new SessionValidationException("Missing Header", "No session header passed in the request.");
        }
        var id = ids.nextElement();
        var matcher = idParameterPattern.matcher(id);
        if (!matcher.matches() || id.length() > ID_MAX_LENGTH) {
            throw new SessionValidationException(MALFORMED_SESSION_HEADER, MALFORMED_SESSION_HEADER);
        }
        if (!id.equals(givenSessionId)) {
            throw new SessionValidationException(HEADER_MISMATCH, "Header value found but is not same as value passed to check against.");
        }
        if (ids.hasMoreElements()) {
            throw new SessionValidationException(HEADER_MISMATCH, "More than one header value found.");
        }
        return givenSessionId;
    }

    public RuntimeException handleInSamlProcessing(IdentificationException me, boolean inRequest) {
        if (me instanceof EidWrappingException) {
            return me;
        } else {
            if (inRequest)
                return new SamlRequestException("SAML request failed", me);
            else
                return new SamlResponseException("SAML response failed", me);
        }
    }
}
