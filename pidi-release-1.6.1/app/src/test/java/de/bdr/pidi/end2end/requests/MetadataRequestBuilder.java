/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import org.springframework.http.HttpMethod;

public class MetadataRequestBuilder extends RequestBuilder<MetadataRequestBuilder>{
    public MetadataRequestBuilder() {
        super(HttpMethod.GET);
    }
}
