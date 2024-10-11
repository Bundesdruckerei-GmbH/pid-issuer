/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Do NOT ignore unknown json properties.
 * @param hashBytesEnc - string, contentEncoding: "base64"
 */
public record PresentationSigningRequest(@JsonProperty("hash_bytes") String hashBytesEnc) {
    public PresentationSigningRequest() {
        this(null);
    }
}
