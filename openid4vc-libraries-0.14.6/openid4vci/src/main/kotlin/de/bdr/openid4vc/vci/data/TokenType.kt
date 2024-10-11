/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.data

enum class TokenType(val value: String) {
    BEARER("Bearer"),
    DPOP("DPoP"),
    UNKNOWN("unknown")
}
