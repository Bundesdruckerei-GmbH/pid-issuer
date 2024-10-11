/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;


import de.bdr.pidi.authorization.FlowVariant;
import org.springframework.http.HttpMethod;

public class FinishAuthorizationRequestBuilder extends RequestBuilder<FinishAuthorizationRequestBuilder> {


    public FinishAuthorizationRequestBuilder() {
        super(HttpMethod.GET);

    }

    public FinishAuthorizationRequestBuilder(String url) {
        this();
        withUrl(url);
    }

    public static FinishAuthorizationRequestBuilder valid(FlowVariant flowVariant, String issuerState) {
        return new FinishAuthorizationRequestBuilder()
                .withUrl(getFinishAuthorizationPath(flowVariant))
                .withIssuerState(issuerState);
    }

    public static String getFinishAuthorizationPath(FlowVariant flowVariant) {
        return "/%s/finish-authorization".formatted(flowVariant.urlPath);
    }

    public FinishAuthorizationRequestBuilder withIssuerState(String issuerState) {
        withQueryParam("issuer_state", issuerState);
        return this;
    }
}
