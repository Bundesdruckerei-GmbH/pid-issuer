/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vp.pex.FormatDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MsoMdocFormatDescription(@SerialName("alg") val alg: List<String>) : FormatDescription {
    @Transient override val type: CredentialFormat = MsoMdocCredentialFormat
}
