/*
 * Copyright 2024-2025 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.testdata;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidTestData {
    public static final String REDIRECT_URI = "https://secure.redirect.com";
    public static final String CODE_CHALLANGE = "VPvsxc7h-NOKbZX9pKqzgLdc3-3VL_U8B4cKRt6r2xE";
    public static final String CODE_VERIFIER = "ABCDEFGHIJklmnopqrstUVWXYZ-._~0123456789-50Zeichen";

    public static final String AGE_BIRTH_YEAR = "2000";
    public static final String BIRTH_DATE = AGE_BIRTH_YEAR + "-01-01";
    public static final LocalDate LOCAL_BIRTH_DATE = LocalDate.parse(BIRTH_DATE);
    public static final String AGE_IN_YEARS = String.valueOf(Period.between(LOCAL_BIRTH_DATE, LocalDate.now()).getYears());
    public static final String BIRTH_DATE_TIME = BIRTH_DATE + "T00:00:00Z";
}
