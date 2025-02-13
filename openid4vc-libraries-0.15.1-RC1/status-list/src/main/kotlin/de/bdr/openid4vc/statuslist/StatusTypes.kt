/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

class StatusListRegistry {

    enum class StatusTypes(val v: Byte) {
        VALID(0x00),
        INVALID(0x01),
        SUSPENDED(0x02),
    }
}
