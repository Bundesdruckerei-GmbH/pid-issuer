/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

import org.springframework.http.HttpMethod;

public class MetadataRequestBuilder extends RequestBuilder<MetadataRequestBuilder>{
    public MetadataRequestBuilder() {
        super(HttpMethod.GET);
    }
}
