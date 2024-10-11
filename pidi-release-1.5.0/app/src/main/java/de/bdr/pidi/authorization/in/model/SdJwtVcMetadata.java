/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in.model;

public record SdJwtVcMetadata(String issuer, SdJwtVcMetadataKeys jwks) {
}

