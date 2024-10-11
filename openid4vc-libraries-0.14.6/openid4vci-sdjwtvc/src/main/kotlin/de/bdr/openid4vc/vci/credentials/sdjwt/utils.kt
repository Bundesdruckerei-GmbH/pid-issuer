/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials.sdjwt

internal fun Map<String, Any>?.getPath(vararg path: String) = getPath(path.toList())

internal fun Map<String, Any>?.getPath(path: List<String>): Map<String, Any>? {
    if (this == null || path.isEmpty()) return this
    return (this[path[0]] as? Map<String, Any>).getPath(path.subList(1, path.size))
}
