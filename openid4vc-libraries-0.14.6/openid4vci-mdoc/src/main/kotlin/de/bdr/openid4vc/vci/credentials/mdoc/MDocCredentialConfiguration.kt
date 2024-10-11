/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials.mdoc

import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProofType
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType
import de.bdr.openid4vc.vci.credentials.CredentialConfiguration
import de.bdr.openid4vc.vci.credentials.FeatureMode
import de.bdr.openid4vc.vci.credentials.FeatureMode.DISABLED
import java.time.Duration

class MDocCredentialConfiguration(
    override val id: String,
    override val attestationBasedClientAuthentication: FeatureMode = DISABLED,
    override val dpop: FeatureMode = DISABLED,
    override val pkce: FeatureMode = DISABLED,
    override val par: FeatureMode = FeatureMode.OPTIONAL,
    override val statusListPool: String? = null,
    override val proofTypesSupported: Set<ProofType> = setOf(JwtProofType, CwtProofType),
    val batchSize: Int? = null,
    val lifetime: Duration = Duration.ofDays(30),
    val docType: String,
    val credentialStructure: CredentialStructure = CredentialStructure.DOCUMENT,
) : CredentialConfiguration {

    override val format = MsoMdocCredentialFormat
    override val keyBinding: Boolean = true

    init {
        if (statusListPool != null) {
            error("Status list for mdoc not supported yet")
        }
    }
}

enum class CredentialStructure {
    DOCUMENT,
    ISSUER_SIGNED
}
