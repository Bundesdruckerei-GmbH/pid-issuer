/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

import org.springframework.http.HttpMethod;

public class SessionRequestBuilder extends RequestBuilder<SessionRequestBuilder> {

    public SessionRequestBuilder() {
        super(HttpMethod.POST);
    }

    public static SessionRequestBuilder valid() {
        // only b1 flow has session endpoint
        var path = "b1/session";
        return new SessionRequestBuilder()
                .withUrl(path);
    }
}
