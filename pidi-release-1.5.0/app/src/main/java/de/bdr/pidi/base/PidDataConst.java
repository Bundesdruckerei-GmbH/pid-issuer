/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PidDataConst {
    public static final String SD_JWT_PID = "pid";
    public static final String SD_JWT_VCTYPE = "https://example.bmi.bund.de/credential/pid/1.0";
    public static final String MDOC_ID = "PidMdoc";
    public static final String MDOC_TYPE = "eu.europa.ec.eudi.pid.1";
}
