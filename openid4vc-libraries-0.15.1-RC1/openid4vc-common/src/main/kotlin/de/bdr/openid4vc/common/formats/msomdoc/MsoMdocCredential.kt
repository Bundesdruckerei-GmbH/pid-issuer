/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer

interface MsoMdocCredential : Credential {
    val doctype: String
    val namespacesAndValues: Map<String, Map<String, CBORObject>>

    fun namespacesAndValues(
        toDisclose: Set<DistinctClaimsPathPointer>
    ): Map<String, Map<String, CBORObject>>

    fun value(namespace: String, elementIdentifier: String) =
        namespacesAndValues[namespace]?.get(elementIdentifier)
}
