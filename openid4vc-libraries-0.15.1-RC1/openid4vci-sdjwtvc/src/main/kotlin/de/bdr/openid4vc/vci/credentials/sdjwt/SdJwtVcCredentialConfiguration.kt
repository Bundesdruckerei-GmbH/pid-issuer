/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.credentials.sdjwt

import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType
import de.bdr.openid4vc.vci.credentials.CredentialConfiguration
import de.bdr.openid4vc.vci.credentials.FeatureMode
import de.bdr.openid4vc.vci.credentials.FeatureMode.DISABLED
import de.bdr.openid4vc.vci.credentials.FeatureMode.OPTIONAL
import java.time.Duration

class SdJwtVcCredentialConfiguration(
    override val id: String,
    override val keyBinding: Boolean = false,
    override val pkce: FeatureMode = DISABLED,
    override val attestationBasedClientAuthentication: FeatureMode = DISABLED,
    override val dpop: FeatureMode = DISABLED,
    override val par: FeatureMode = OPTIONAL,
    override val statusListPool: String? = null,
    override val proofTypesSupported: Set<ProofType> = setOf(JwtProofType),
    val vct: String,
    val jadesSignatures: Boolean = false,
    val lifetime: Duration? = null,
    val numOfDecoysLimit: Int = 0,
) : CredentialConfiguration {
    override val format = SdJwtVcCredentialFormat
}
