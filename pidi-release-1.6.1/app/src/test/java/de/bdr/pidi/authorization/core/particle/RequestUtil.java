/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpHeaders;
import de.bdr.openid4vc.vci.service.HttpRequest;
import kotlin.Pair;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Collections;
import java.util.Map;

public class RequestUtil {
    public static HttpRequest<?> getHttpRequest(Map<String, String> params) {
        return HttpRequest.Companion.bodyless(
                "POST",
                "https://localhost",
                "par",
                new HttpHeaders(new LinkedMultiValueMap<>()),
                params);
    }

    public static <T> HttpRequest<T> getHttpRequest(T body) {
        return new HttpRequest<>(
                "POST",
                "https://localhost",
                "par",
                new HttpHeaders(new Pair<>("Content-Type", "application/json"), new Pair<>("Authorization", "DPoP token")),
                Collections.emptyMap(),
                body);
    }
}
