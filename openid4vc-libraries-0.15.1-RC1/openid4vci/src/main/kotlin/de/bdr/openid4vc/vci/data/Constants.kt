/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.data

// TODO: use commons library
object Constants {
    const val FORMAT_VC_SD_JWT = "vc+sd-jwt"
    const val SCHEME_OPENID_CREDENTIAL_OFFER = "openid-credential-offer"
    const val QUERY_PARAM_CREDENTIAL_OFFER = "credential_offer"
    const val PROOF_TYPE_JWT = "jwt"
    const val PROOF_TYPE_OPENID4VCI_PROOF_JWT = "openid4vci-proof+jwt"
}
