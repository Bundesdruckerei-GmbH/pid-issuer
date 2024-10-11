/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.jwk.JWK

class JwkKeyMaterial(override val jwk: JWK) : KeyMaterial
