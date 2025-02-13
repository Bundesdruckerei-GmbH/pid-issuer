/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.query

import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.vp.dcql.DcqlQuery

class CorrectResultTestCase(
    val name: String,
    val query: DcqlQuery,
    val credentials: Map<String, Credential>,
) {
    override fun toString() = name

    companion object {
        fun all() = DcqlQueryTestCase.entries.flatMap { it.correctResultTestCases }
    }
}
