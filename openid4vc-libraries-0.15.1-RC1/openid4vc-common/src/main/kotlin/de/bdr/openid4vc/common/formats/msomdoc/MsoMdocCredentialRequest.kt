/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MsoMdocCredentialRequest(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = MsoMdocCredentialFormat,
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("doctype") val doctype: String,
    // END TODO
) : FormatSpecificCredentialRequest() {
    init {
        validate()
        require(format == MsoMdocCredentialFormat) {
            "Format must be ${MsoMdocCredentialFormat.format}"
        }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MsoMdocCredentialRequest

        if (format != other.format) return false
        if (proof != other.proof) return false
        if (proofs != other.proofs) return false
        if (credentialEncryption != other.credentialEncryption) return false
        if (doctype != other.doctype) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + (proof?.hashCode() ?: 0)
        result = 31 * result + proofs.hashCode()
        result = 31 * result + (credentialEncryption?.hashCode() ?: 0)
        result = 31 * result + doctype.hashCode()
        return result
    }
}
