/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.proofs.ProofTypesSupported
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/* Definied in OID4VCI Appendix E ISO mDL */
@Serializable
data class MsoMdocCredentialDescription(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = MsoMdocCredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    // TODO These are only there for the interop event and will probably be changed afterwards.
    @SerialName("credential_alg_values_supported")
    val credentialAlgValuesSupported: List<Int>? = null,
    @SerialName("credential_crv_values_supported")
    val credentialCrvValuesSupported: List<Int>? = null,
    @SerialName("policy") val policy: Policy? = null,
    // END TODO
    @SerialName("proof_types_supported")
    override val proofTypesSupported: ProofTypesSupported? = null,
    @SerialName("display") override val display: List<JsonObject>? = null,
    /** Use [Claim] for the base interpretation of the values. */
    @SerialName("claims") val claims: Map<String, JsonObject>? = null,
    @SerialName("doctype") val doctype: String,
) : CredentialDescription() {

    init {
        require(format == MsoMdocCredentialFormat) {
            "Format must be ${MsoMdocCredentialFormat.format}"
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
