/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import COSE.AlgorithmID
import COSE.Sign1Message
import COSE.sign
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.Sign1Message
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.signing.SignerToJwsSignerAdapter
import de.bdr.openid4vc.statuslist.StatusListException.Reason.INVALID_CLAIM
import de.bdr.openid4vc.statuslist.StatusListException.Reason.MISSING_CLAIMS
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.Date

/** Represents a status list token as specified by draft-ietf-oauth-status-list 00. */
class StatusListToken(
    val statusListUri: String,
    val issuerUri: String,
    val issuedAt: Instant,
    val statusList: StatusList,
    val expiresAt: Instant? = null,
    val ttl: Duration? = null,
    val aggregationUri: String? = null,
    val jwt: SignedJWT? = null,
    val cwt: Sign1Message? = null
) {

    companion object {

        const val STATUS_LIST_JWT_TYPE = "statuslist+jwt"

        const val STATUS_LIST_CWT_TYPE = "statuslist+cwt"

        const val STATUS_LIST_CLAIM = "status_list"

        const val AGGREGATION_URI_CLAIM = "aggregation_uri"

        const val BITS_CLAIM = "bits"

        const val LIST_CLAIM = "lst"

        const val TTL_CLAIM = "ttl"

        val CWT_ISS = CBORObject.FromObject(1)

        val CWT_SUB = CBORObject.FromObject(2)

        val CWT_IAT = CBORObject.FromObject(6)

        val CWT_EXP = CBORObject.FromObject(4)

        val CWT_TTL = CBORObject.FromObject(65534)

        val CWT_STATUS_LIST = CBORObject.FromObject(65535)

        const val CWT_TAG = 61

        val CWT_HEADER_ALG = CBORObject.FromObject(1)

        val CWT_HEADER_TYP = CBORObject.FromObject(16)

        val CWT_SIGNATURE_ALGORITHMS =
            setOf(
                AlgorithmID.ECDSA_256,
                AlgorithmID.ECDSA_384,
                AlgorithmID.ECDSA_512,
                AlgorithmID.RSA_PSS_256,
                AlgorithmID.RSA_PSS_384,
                AlgorithmID.RSA_PSS_512
            )

        /**
         * Parses a status list token from the serialized form.
         *
         * **Note** The checks required by the standard are performed but no signature verification
         * is done on parsing. This needs to happen afterward using the [verify] methods.
         */
        fun parse(serialized: String) = from(SignedJWT.parse(serialized))

        /**
         * Creates a status list token from a [SignedJWT].
         *
         * **Note** The checks required by the standard are performed but no signature verification
         * is done on parsing. This needs to happen afterward using the [verify] methods.
         */
        fun from(jwt: SignedJWT): StatusListToken {
            // check mandatory claims
            val issuerUri = jwt.jwtClaimsSet.issuer ?: fail(MISSING_CLAIMS)

            if (!JWSAlgorithm.Family.SIGNATURE.contains(jwt.header.algorithm)) {
                fail(StatusListException.Reason.UNSUPPORTED_ALGORITHM)
            }

            val typ = jwt.header.type ?: fail(MISSING_CLAIMS)
            if (typ != JOSEObjectType(STATUS_LIST_JWT_TYPE))
                fail(StatusListException.Reason.INVALID_TYPE)

            val statusListUri = jwt.jwtClaimsSet.subject ?: fail(MISSING_CLAIMS)

            val issuedAt = jwt.jwtClaimsSet.issueTime ?: fail(MISSING_CLAIMS)
            if (issuedAt.after(Date.from(Instant.now()))) {
                fail(StatusListException.Reason.INVALID_TIME)
            }

            val expiresAt = jwt.jwtClaimsSet.expirationTime
            if (expiresAt != null && expiresAt.before(Date.from(Instant.now()))) {
                fail(StatusListException.Reason.EXPIRED)
            }

            val sl =
                (jwt.jwtClaimsSet.claims[STATUS_LIST_CLAIM] ?: fail(MISSING_CLAIMS)) as? Map<*, *>
                    ?: fail(INVALID_CLAIM)
            val bits = (sl[BITS_CLAIM] ?: fail(MISSING_CLAIMS)) as? Long ?: fail(INVALID_CLAIM)
            val lst = (sl[LIST_CLAIM] ?: fail(MISSING_CLAIMS)) as? String ?: fail(INVALID_CLAIM)

            val aggregationUri = sl[AGGREGATION_URI_CLAIM]
            if (aggregationUri !is String?) fail(INVALID_CLAIM)

            val statusList = StatusList.fromEncoded(bits.toInt(), lst)

            val ttl = jwt.jwtClaimsSet.getLongClaim(TTL_CLAIM)?.let { Duration.ofSeconds(it) }

            return StatusListToken(
                statusListUri,
                issuerUri,
                issuedAt.toInstant(),
                statusList,
                expiresAt?.toInstant(),
                ttl,
                aggregationUri,
                jwt = jwt
            )
        }

        fun from(cwt: CBORObject): StatusListToken {
            val (sign1, claims) =
                try {
                    val sign1 = Sign1Message(cwt.untagIf(CWT_TAG))
                    Pair(sign1, CBORObject.DecodeFromBytes(sign1.GetContent()))
                } catch (e: Exception) {
                    fail(StatusListException.Reason.INVALID_STATUS_LIST)
                }

            // check mandatory claims
            val issuerUri = claims[CWT_ISS]?.claimAsString() ?: fail(MISSING_CLAIMS)

            val alg = sign1.protectedAttributes[CWT_HEADER_ALG]

            if (!CWT_SIGNATURE_ALGORITHMS.contains(AlgorithmID.FromCBOR(alg))) {
                fail(StatusListException.Reason.UNSUPPORTED_ALGORITHM)
            }

            val typ =
                sign1.protectedAttributes[CWT_HEADER_TYP].claimAsString() ?: fail(MISSING_CLAIMS)

            if (typ != STATUS_LIST_CWT_TYPE) fail(StatusListException.Reason.INVALID_TYPE)

            val statusListUri = claims[CWT_SUB].claimAsString() ?: fail(MISSING_CLAIMS)

            val issuedAt = claims[CWT_IAT].claimAsInstant() ?: fail(MISSING_CLAIMS)
            if (issuedAt.isAfter(Instant.now())) {
                fail(StatusListException.Reason.INVALID_TIME)
            }

            val expiresAt = claims[CWT_EXP].claimAsInstant()
            if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
                fail(StatusListException.Reason.EXPIRED)
            }

            val sl = claims[CWT_STATUS_LIST] ?: fail(MISSING_CLAIMS)

            val aggregationUri = sl[StatusList.CBOR_AGGREGATION_URI_CLAIM].claimAsString()

            val statusList = StatusList.fromCbor(sl)

            val ttl =
                try {
                    claims[CWT_TTL]?.AsInt64Value()?.let { Duration.ofSeconds(it) }
                } catch (e: Exception) {
                    fail(INVALID_CLAIM)
                }

            return StatusListToken(
                statusListUri,
                issuerUri,
                issuedAt,
                statusList,
                expiresAt,
                ttl,
                aggregationUri,
                cwt = sign1
            )
        }

        private fun CBORObject.claimAsInstant() =
            try {
                Instant.ofEpochMilli(AsInt64Value())
            } catch (e: Exception) {
                fail(INVALID_CLAIM)
            }

        private fun CBORObject.claimAsString() =
            try {
                AsString()
            } catch (e: Exception) {
                fail(INVALID_CLAIM)
            }

        fun fetch(
            statusListUri: URI,
            client: HttpClient = defaultHttpClient,
            httpTimeout: Duration = Duration.ofSeconds(5)
        ): StatusListToken {
            val request =
                HttpRequest.newBuilder(statusListUri)
                    .GET()
                    .header("accept", "application/statuslist+jwt")
                    .timeout(httpTimeout)
                    .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                fail(
                    StatusListException.Reason.INVALID_STATUS_LIST,
                    "Received ${response.statusCode()} response when fetching $statusListUri"
                )
            }
            return parse(response.body())
        }
    }

    fun asJwt(
        signer: Signer,
        customizer: (header: JWSHeader.Builder, claimsSet: JWTClaimsSet.Builder) -> Unit = { _, _ ->
        }
    ): JWT {
        // header "alg" and "kid" set by signer
        val header =
            JWSHeader.Builder(signer.algorithm.jwsAlgorithm)
                .type(JOSEObjectType(STATUS_LIST_JWT_TYPE))

        val claimsSet = JWTClaimsSet.Builder()

        // mandatory claims
        claimsSet.subject(statusListUri)
        claimsSet.issuer(issuerUri)
        claimsSet.issueTime(Date(issuedAt.toEpochMilli()))
        claimsSet.claim(STATUS_LIST_CLAIM, statusList.toJsonObject(aggregationUri))
        ttl?.let { claimsSet.claim(TTL_CLAIM, ttl.seconds) }

        // optional claims
        if (expiresAt != null) claimsSet.expirationTime(Date(expiresAt.toEpochMilli()))

        // customize
        customizer(header, claimsSet)

        // build JWT
        val jwt = SignedJWT(header.build(), claimsSet.build())

        // sign JWT by external signer
        jwt.sign(SignerToJwsSignerAdapter(signer))

        return jwt
    }

    fun asCwt(
        signer: Signer,
        customizer: (content: CBORObject, sign1: Sign1Message) -> Unit = { _, _ -> }
    ): CBORObject {
        val sign1 = Sign1Message()
        sign1.protectedAttributes[CWT_HEADER_ALG] = signer.algorithm.coseAlgorithm.AsCBOR()
        sign1.protectedAttributes[CWT_HEADER_TYP] = CBORObject.FromObject("statuslist+cwt")

        val cbor = CBORObject.NewMap()
        cbor[CWT_ISS] = CBORObject.FromObject(issuerUri)
        cbor[CWT_SUB] = CBORObject.FromObject(statusListUri)
        cbor[CWT_IAT] = CBORObject.FromObject(issuedAt.toEpochMilli())
        expiresAt?.toEpochMilli()?.let { cbor[CWT_EXP] = CBORObject.FromObject(it) }
        ttl?.seconds?.let { cbor[CWT_TTL] = CBORObject.FromObject(it) }
        cbor[CWT_STATUS_LIST] = statusList.toCborObject(aggregationUri)

        customizer(cbor, sign1)

        sign1.SetContent(cbor.EncodeToBytes())
        sign1.sign(signer)
        return CBORObject.FromObjectAndTag(sign1.EncodeToCBORObject(), CWT_TAG)
    }

    /**
     * Checks if this status list has an expiration date and it is not expired.
     *
     * @return [false] if this status list has no expiration date or if it is expired, [true]
     *   otherwise
     */
    fun isNotExpired(): Boolean {
        return expiresAt != null && expiresAt.isAfter(Instant.now())
    }

    /** @return The value of the status referenced in the referenced token */
    fun get(referencedToken: String) = get(ReferencedToken.parse(referencedToken))

    /** @return The value of the status referenced in the referenced token */
    fun get(referencedToken: JWT) = get(ReferencedToken.from(referencedToken))

    /** @return The value of the status referenced in the referenced token */
    fun get(referencedToken: ReferencedToken): Byte {
        if (referencedToken.issuerUri != issuerUri)
            fail(INVALID_CLAIM, "iss in referenced token and status list token does not match")

        if (referencedToken.statusListUri != statusListUri)
            fail(
                INVALID_CLAIM,
                "status list uri in referenced token and status list token does not match"
            )

        return statusList.get(referencedToken.index)
    }
}
