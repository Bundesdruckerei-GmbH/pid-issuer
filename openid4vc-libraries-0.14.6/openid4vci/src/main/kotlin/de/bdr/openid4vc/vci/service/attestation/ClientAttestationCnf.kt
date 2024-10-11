/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.attestation

import com.nimbusds.jose.jwk.JWK
import java.lang.IllegalArgumentException

class ClientAttestationCnf(cnf: Map<String, Any>) {

    val jwk =
        (cnf["jwk"] as? Map<String, Any?>)?.let { JWK.parse(it) }
            ?: throw IllegalArgumentException("cnf.jwk value is invalid")

    val haipKeyType = cnf["key_type"] as? String

    val haipUserAuthentication = cnf["user_authentication"] as? String
}
