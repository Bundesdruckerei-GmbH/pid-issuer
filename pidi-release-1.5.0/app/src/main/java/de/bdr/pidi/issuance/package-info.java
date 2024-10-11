/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
/**
 * Issuance is the part of the pid-issuer that builds the credentials.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"authorization", "revocation", "authorization::identificationApi", "authorization::issuanceApi", "base", "base::requests"}
)
package de.bdr.pidi.issuance;