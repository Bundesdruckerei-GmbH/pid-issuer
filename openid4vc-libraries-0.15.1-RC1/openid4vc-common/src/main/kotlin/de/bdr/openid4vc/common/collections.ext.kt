/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

fun <E> Sequence<E>.toSetWithout(e: E): Set<E> {
    val result = toMutableSet()
    result.remove(e)
    return result
}
