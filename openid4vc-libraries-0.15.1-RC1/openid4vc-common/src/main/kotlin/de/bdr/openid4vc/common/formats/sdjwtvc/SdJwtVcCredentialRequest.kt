/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SdJwtVcCredentialRequest(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SdJwtVcCredentialFormat,
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("vct") val vct: String,
) : FormatSpecificCredentialRequest() {
    init {
        require(format == SdJwtVcCredentialFormat) {
            "Format must be ${SdJwtVcCredentialFormat.format}"
        }
    }
}
