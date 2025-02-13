/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
/**
 * The Authorization Server part of the pid-issuer
 * <p>
 * OID4V
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"walletattestation",
        "authorization::identificationApi",  "authorization::issuanceApi", "authorization", "clientconfiguration", "base", "base::requests"})
package de.bdr.pidi.authorization;