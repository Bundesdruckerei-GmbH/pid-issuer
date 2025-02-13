/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.out.identification;

import java.util.Optional;

public interface IdentificationApi {
    Optional<String> validateSessionAndGetPseudonym(String sessionId);
}
