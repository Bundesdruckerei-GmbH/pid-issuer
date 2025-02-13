/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.proofs.ProofTypesSupported
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Defined in HAIP 7.2.2.
 * https://vcstuff.github.io/oid4vc-haip-sd-jwt-vc/draft-oid4vc-haip-sd-jwt-vc.html#section-7.2.2
 */
@Serializable
data class SdJwtVcCredentialDescription(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SdJwtVcCredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    @SerialName("proof_types_supported")
    override val proofTypesSupported: ProofTypesSupported? = null,
    @SerialName("display") override val display: List<JsonObject>? = null,
    @SerialName("vct") val vct: String,
    /** Use [Claim] for the base interpretation of the values. */
    @SerialName("claims") val claims: JsonObject? = null,
    @SerialName("order") val order: List<String>? = null,
) : CredentialDescription() {

    init {
        require(format == SdJwtVcCredentialFormat) {
            "Format must be ${SdJwtVcCredentialFormat.format}"
        }
    }

    /** Use [Display] for [DisplayType] for the base interpretation. */
    @Serializable
    data class Claim<DisplayType>(
        @SerialName("mandatory") val mandatory: Boolean = false,
        @SerialName("value_type") val valueType: String? = null,
        @SerialName("display") val display: List<DisplayType>? = null,
    ) {
        @Serializable
        data class Display(
            @SerialName("name") val name: String? = null,
            @SerialName("locale") val locale: String? = null,
        )
    }
}
