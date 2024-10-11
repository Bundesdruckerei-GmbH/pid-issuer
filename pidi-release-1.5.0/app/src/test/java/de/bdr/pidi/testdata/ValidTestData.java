/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.testdata;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidTestData {
    public static final String REDIRECT_URI = "https://secure.redirect.com";
    public static final String CODE_CHALLANGE = "VPvsxc7h-NOKbZX9pKqzgLdc3-3VL_U8B4cKRt6r2xE";
    public static final String CODE_VERIFIER = "ABCDEFGHIJklmnopqrstUVWXYZ-._~0123456789-50Zeichen";
}
