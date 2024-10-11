/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

public class UnsupportedResponseTypeException extends OIDException {
    public UnsupportedResponseTypeException(String message) {
        super("unsupported_response_type", message);
    }
}
