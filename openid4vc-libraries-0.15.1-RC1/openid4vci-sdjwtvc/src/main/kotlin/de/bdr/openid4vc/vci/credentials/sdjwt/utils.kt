/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.credentials.sdjwt

internal fun Map<String, Any>?.getPath(vararg path: String) = getPath(path.toList())

internal fun Map<String, Any>?.getPath(path: List<String>): Map<String, Any>? {
    if (this == null || path.isEmpty()) return this
    return (this[path[0]] as? Map<String, Any>).getPath(path.subList(1, path.size))
}
