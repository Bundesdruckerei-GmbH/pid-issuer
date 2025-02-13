/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.base;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PidDataConst {
    public static final String SD_JWT_PID = "pid";
    public static final String SD_JWT_VCTYPE_PATH = "credentials/pid/1.0";
    public static final String MDOC_ID = "PidMdoc";
    public static final String MDOC_TYPE = "eu.europa.ec.eudi.pid.1";
}
