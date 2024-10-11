/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.dpop

import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.vci.logging.Oid4VcLog.log
import de.bdr.openid4vc.vci.service.HttpRequest
import de.bdr.openid4vc.vci.service.endpoints.MissingNonceException
import de.bdr.openid4vc.vci.service.endpoints.NonceService
import de.bdr.openid4vc.vci.utils.clock
import java.nio.charset.StandardCharsets.US_ASCII
import java.security.MessageDigest
import java.text.ParseException
import java.time.Duration
import java.time.Instant
import java.util.Base64

/** Performs validation of DPoP proofs according to RFC-9449. */
class DPoPValidator(
    publicUrl: String,
    val jtiStorage: de.bdr.openid4vc.vci.data.storage.JtiStorage,
    val nonceService: NonceService?,
    val proofValidityDuration: Duration,
    val proofTimeTolerance: Duration
) {

    private val https = publicUrl.startsWith("https://")

    /**
     * Validates and returns a DPoP proof from an HTTP request.
     *
     * This method can be invoked in two modes:
     * * If no JWK is passed in using the `expected` parameter, a proof is extracted from the
     *   request, validated accordingly and returned. It is assumed, that the proof is not bound to
     *   an access token. `null` may be returned if no proof is present.
     * * If a JWK is passed in, the proof is expected to be bound to an access token. An
     *   `IllegalArgumentException` will be thrown if no proof is present. The proof is validated
     *   accordingly. It is checked that the used key matches the passed in JWK and the ath claim is
     *   checked based on the authorization header.
     */
    fun validate(request: HttpRequest<*>, expected: JWK? = null): DPoPProof? {
        val dPoPProof =
            try {
                DPoPProof.parse(request)
                    ?: if (expected != null) {
                        throw IllegalArgumentException("missing dpop proof")
                    } else {
                        return null
                    }
            } catch (e: ParseException) {
                throw IllegalArgumentException("dpop proof parsing error", e)
            }
        validate(dPoPProof, request, expected)
        return dPoPProof
    }

    private fun validate(proof: DPoPProof, request: HttpRequest<*>, expected: JWK?) {
        require(proof.htm == request.method) {
            log.debug("DPoP: proof.htm = {}, request.htm = {}", proof.htu, request.method)
            "htm value mismatch"
        }

        val expectedHtu = request.htu
        require(proof.htu == expectedHtu) {
            log.debug("DPoP: proof.htu = {}, request.htu = {}", proof.htu, expectedHtu)
            "htu value mismatch"
        }

        val now = Instant.now(clock)
        val issueTime = proof.jwt.jwtClaimsSet.issueTime.toInstant()

        val validFrom = issueTime.minus(proofTimeTolerance)
        val validUntil = issueTime.plus(proofValidityDuration).plus(proofTimeTolerance)

        require(!now.isAfter(validUntil)) { "proof too old" }

        require(!now.isBefore(validFrom)) { "proof too young" }

        require(jtiStorage.isUnused(proof.jti, validUntil)) { "JTI already used" }

        // ath and jwk are only validated if the proof is bound to an access token, this is
        // indicated by a JWK passed in as expected
        if (expected != null) {
            val expectedAth = request.ath
            require(expectedAth == proof.ath) {
                log.debug("DPoP: proof.ath = {}, request.ath = {}", proof.ath, expectedAth)
                "ath mismatch"
            }
            require(expected == proof.jwk) { "key mismatch" }
        }

        if (nonceService != null) {
            nonceService.validate(
                proof.nonce ?: throw MissingNonceException(nonceService.generate())
            )
        } else require(proof.nonce == null) { "Proof contained a nonce but none was expected" }
    }

    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()

    private val HttpRequest<*>.ath: String
        get() {
            val header = headers["authorization"] ?: error("Missing access token in request")
            val accessToken =
                if (header.startsWith("DPoP ")) {
                    header.substring(5)
                } else {
                    error("No DPoP access token in request")
                }
            val md = MessageDigest.getInstance("SHA-256")
            return base64UrlEncoder.encodeToString(md.digest(accessToken.toByteArray(US_ASCII)))
        }

    private val HttpRequest<*>.htu: String
        get() {
            val protocol =
                if (https) {
                    "https"
                } else {
                    "http"
                }
            val host = this.headers["host"]
            val path = this.path
            val htu = "$protocol://$host$path"
            return htu
        }
}
