/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import java.net.URI

interface StatusListSource {
    fun get(statusListUri: URI): StatusList
}
