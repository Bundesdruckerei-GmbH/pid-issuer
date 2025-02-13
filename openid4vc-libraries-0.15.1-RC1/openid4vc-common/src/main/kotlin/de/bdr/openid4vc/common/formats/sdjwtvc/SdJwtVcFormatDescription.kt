/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vp.pex.FormatDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SdJwtVcFormatDescription : FormatDescription {
    @Transient override val type: CredentialFormat = SdJwtVcCredentialFormat

    override fun hashCode(): Int {
        return 394736936
    }

    override fun equals(other: Any?) = other is SdJwtVcFormatDescription
}
