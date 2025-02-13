/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.formats.json.JsonCredentialInputDescriptorMatcher

object SdJwtVcCredentialFormat : CredentialFormat {
    override val format = "vc+sd-jwt"
    override val credentialRequestClass = SdJwtVcCredentialRequest::class
    override val credentialDescriptionClass = SdJwtVcCredentialDescription::class
    override val formatDescriptionClass = SdJwtVcFormatDescription::class
    override val credentialQueryClass = SdJwtVcCredentialQuery::class
    override val credentialQueryMatcher = SdJwtVcDcqlCredentialQueryMatcher
    override val inputDescriptorMatcher = JsonCredentialInputDescriptorMatcher
}
