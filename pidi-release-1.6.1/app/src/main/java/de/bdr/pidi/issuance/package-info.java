/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
/**
 * Issuance is the part of the pid-issuer that builds the credentials.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"authorization", "authorization::identificationApi", "authorization::issuanceApi", "base", "base::requests"}
)
package de.bdr.pidi.issuance;