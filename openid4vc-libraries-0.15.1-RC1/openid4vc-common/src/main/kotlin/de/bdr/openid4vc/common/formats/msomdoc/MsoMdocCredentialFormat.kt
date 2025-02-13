/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import de.bdr.openid4vc.common.CredentialFormat

object MsoMdocCredentialFormat : CredentialFormat {
    override val format = "mso_mdoc"
    override val credentialRequestClass = MsoMdocCredentialRequest::class
    override val credentialDescriptionClass = MsoMdocCredentialDescription::class
    override val formatDescriptionClass = MsoMdocFormatDescription::class
    override val credentialQueryClass = MsoMdocCredentialQuery::class
    override val credentialQueryMatcher = MsoMdocDcqlCredentialQueryMatcher
    override val inputDescriptorMatcher = MsoMdocInputDescriptorMatcher
}
