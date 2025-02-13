/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

public class IdentificationFailedException
    extends OIDException {

    public IdentificationFailedException(String logMessage) {
        super("access_denied", "Identification failed", logMessage);
    }
}
