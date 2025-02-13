/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.formats.msomdoc.MatcherUtils.getDiscloseableClaims as getDiscloseableClaimsUtil
import de.bdr.openid4vc.common.formats.msomdoc.MatcherUtils.resolveClaimValuesWithPath as resolveClaimValuesWithPathUtil
import de.bdr.openid4vc.common.formats.msomdoc.MatcherUtils.toJsonPrimitive as toJsonPrimitiveUtil
import de.bdr.openid4vc.common.vp.dcql.ClaimsPathPointer
import de.bdr.openid4vc.common.vp.pex.BaseInputDescriptorMatcher

object MsoMdocInputDescriptorMatcher :
    BaseInputDescriptorMatcher<MsoMdocCredential, Map<String, Map<String, CBORObject>>, CBORObject>(
        MsoMdocCredential::class
    ) {

    override fun MsoMdocCredential.getClaims() = namespacesAndValues

    override fun ClaimsPathPointer.resolveClaimValuesWithPath(
        claims: Map<String, Map<String, CBORObject>>
    ) = resolveClaimValuesWithPathUtil(claims)

    override fun CBORObject.toJsonPrimitive() = toJsonPrimitiveUtil()

    override fun MsoMdocCredential.getDiscloseableClaims() = getDiscloseableClaimsUtil()
}
