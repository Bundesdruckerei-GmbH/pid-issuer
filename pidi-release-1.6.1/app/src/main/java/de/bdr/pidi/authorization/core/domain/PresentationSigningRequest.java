/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
