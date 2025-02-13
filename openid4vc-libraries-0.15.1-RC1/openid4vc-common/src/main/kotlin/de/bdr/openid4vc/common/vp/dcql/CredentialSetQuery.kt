/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import kotlinx.serialization.Serializable

@Serializable
data class CredentialSetQuery(val required: Boolean = true, val options: List<List<String>>) {

    fun validate(idsFromCredentials: Collection<String>) {
        require(options.isNotEmpty()) { "vp_query: Must have at least one option" }
        require(options.all { it.isNotEmpty() }) {
            "vp_query: Each option must have at least one entry"
        }
        options.forEach { options ->
            options.forEach { id ->
                require(idsFromCredentials.contains(id)) {
                    "vp_query: Id $id from option not found in credentials"
                }
            }
        }
    }
}
