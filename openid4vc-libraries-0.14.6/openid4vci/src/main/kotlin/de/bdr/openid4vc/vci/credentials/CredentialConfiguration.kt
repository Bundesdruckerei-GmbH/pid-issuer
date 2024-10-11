/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vci.proofs.ProofType

interface CredentialConfiguration {
    /**
     * The id to identify this configuration by.
     *
     * Maximum length 64 characters.
     */
    val id: String
    val format: CredentialFormat
    val keyBinding: Boolean
    val attestationBasedClientAuthentication: FeatureMode
    val dpop: FeatureMode
    val pkce: FeatureMode
    val par: FeatureMode
    val statusListPool: String?
    val proofTypesSupported: Set<ProofType>
}

enum class FeatureMode {
    REQUIRED,
    OPTIONAL,
    DISABLED
}
