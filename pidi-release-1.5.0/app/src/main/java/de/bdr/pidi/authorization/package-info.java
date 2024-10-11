/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
/**
 * The Authorization Server part of the pid-issuer
 * <p>
 * OID4V
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"walletattestation",
        "authorization::identificationApi",  "authorization::issuanceApi", "authorization", "clientconfiguration", "base", "base::requests"})
package de.bdr.pidi.authorization;