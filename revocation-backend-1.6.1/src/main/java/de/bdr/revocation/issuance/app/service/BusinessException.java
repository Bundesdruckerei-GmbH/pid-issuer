/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.app.service;

public class BusinessException extends Exception {
    public BusinessException(Exception e) {
        super(e);
    }
}
