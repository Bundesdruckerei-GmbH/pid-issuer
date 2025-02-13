/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.formats.msomdoc.MatcherUtils.getDiscloseableClaims as getDiscloseableClaimsUtil
import de.bdr.openid4vc.common.formats.msomdoc.MatcherUtils.resolveClaimValuesWithPath as resolveClaimValuesWithPathUtil
import de.bdr.openid4vc.common.formats.msomdoc.MatcherUtils.toJsonPrimitive as toJsonPrimitiveUtil
import de.bdr.openid4vc.common.vp.dcql.BaseDcqlCredentialQueryMatcher
import de.bdr.openid4vc.common.vp.dcql.ClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.ClaimsQuery

/** DCQL Credential query matcher for [MsoMdocCredential]s. */
object MsoMdocDcqlCredentialQueryMatcher :
    BaseDcqlCredentialQueryMatcher<
        MsoMdocCredential,
        MsoMdocCredentialMetaQuery,
        Map<String, Map<String, CBORObject>>,
        CBORObject,
    >(MsoMdocCredential::class, MsoMdocCredentialMetaQuery::class) {

    override fun ClaimsQuery.getPath() =
        DistinctClaimsPathPointer(
            namespace ?: error("Missing namespace in claims query"),
            claimName ?: error("Missing claim name in claims query"),
        )

    override fun MsoMdocCredential.getClaims() = namespacesAndValues

    override fun MsoMdocCredential.getDiscloseableClaims() = getDiscloseableClaimsUtil()

    override fun ClaimsPathPointer.resolveClaimValuesWithPath(
        claims: Map<String, Map<String, CBORObject>>
    ) = resolveClaimValuesWithPathUtil(claims)

    override fun CBORObject.toJsonPrimitive() = toJsonPrimitiveUtil()

    override fun credentialFulfillsMetaQuery(
        credentialMetaQuery: MsoMdocCredentialMetaQuery,
        credential: MsoMdocCredential,
    ) = credentialMetaQuery.doctypeValues.contains(credential.doctype)
}
