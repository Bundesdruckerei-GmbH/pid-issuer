/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.data

enum class TokenType(val value: String) {
    BEARER("Bearer"),
    DPOP("DPoP"),
    UNKNOWN("unknown")
}
