/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.HexFormat;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DigestUtil {
    public static String computeDigest(String input) {
        return HexFormat.of().formatHex(DigestUtils.sha256(input));
    }
}
