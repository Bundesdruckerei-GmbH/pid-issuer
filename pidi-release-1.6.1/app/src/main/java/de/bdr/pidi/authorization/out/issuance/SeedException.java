/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.issuance;

public class SeedException
        extends RuntimeException {

    private final Kind kind;

    public SeedException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public SeedException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public enum Kind {
        /** data is missing in the input */
        MISSING_DATA,
        /**
         * the seed does not have the correct structure,
         * the seed is not handed to the right Issuer
         */
        INVALID,
        /**
         * Signature verification failed
         */
        CRYPTO
    }
}
