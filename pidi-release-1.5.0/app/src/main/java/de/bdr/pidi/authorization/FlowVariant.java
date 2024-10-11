/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum FlowVariant {
    B("b"),
    B1("b1"),
    /** issuer signed - device */
    C("c"),
    /** issuer signed - cloud */
    C1("c1"),
    C2("c2"),
    D("d");

    public final String urlPath;

    public static FlowVariant fromUrlPath(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return null;
        }
        for (FlowVariant variant : FlowVariant.values()) {
            if (variant.urlPath.equals(urlPath)) {
                return variant;
            }
        }
        return null;
    }
}
