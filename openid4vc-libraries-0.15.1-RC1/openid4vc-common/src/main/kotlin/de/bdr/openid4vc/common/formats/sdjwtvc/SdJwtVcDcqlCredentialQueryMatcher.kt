/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.formats.json.JsonCredentialDcqlCredentialQueryMatcher

/** Credential query matcher for SD-JWT VC credentials. */
object SdJwtVcDcqlCredentialQueryMatcher :
    JsonCredentialDcqlCredentialQueryMatcher<SdJwtVcCredential, SdJwtVcCredentialMetaQuery>(
        SdJwtVcCredential::class,
        SdJwtVcCredentialMetaQuery::class,
    ) {

    override fun credentialFulfillsMetaQuery(
        credentialMetaQuery: SdJwtVcCredentialMetaQuery,
        credential: SdJwtVcCredential,
    ): Boolean {
        return credentialMetaQuery.vctValues.contains(credential.vct)
    }
}
