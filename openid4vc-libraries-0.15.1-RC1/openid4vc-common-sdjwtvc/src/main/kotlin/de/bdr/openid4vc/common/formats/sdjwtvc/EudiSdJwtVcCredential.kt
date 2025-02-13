/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.common.credentials.IssuerInfo
import de.bdr.openid4vc.common.credentials.StatusInfo
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_CLAIM
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.MISSING_CLAIM
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import eu.europa.ec.eudi.sdjwt.JsonPointer
import eu.europa.ec.eudi.sdjwt.JwtSignatureVerifier
import eu.europa.ec.eudi.sdjwt.KeyBindingVerifier
import eu.europa.ec.eudi.sdjwt.MustBePresent
import eu.europa.ec.eudi.sdjwt.NoSignatureValidation
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.SdJwtVerifier
import eu.europa.ec.eudi.sdjwt.asClaims
import eu.europa.ec.eudi.sdjwt.present
import eu.europa.ec.eudi.sdjwt.recreateClaims
import eu.europa.ec.eudi.sdjwt.serialize
import isNonNegativeInteger
import java.time.Instant
import jsonStringContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class EudiSdJwtVcCredential(
    override val vct: String,
    override val issuer: IssuerInfo,
    override val issuedAt: Instant?,
    override val validFrom: Instant?,
    override val expiresAt: Instant?,
    override val claims: JsonObject,
    override val discloseable: Set<DistinctClaimsPathPointer>,
    override val status: StatusInfo?,
    val sdJwt: SdJwt.Issuance<SignedJWT>,
    val sdJwtVcAsString: String,
    val kbJwt: JWT? = null,
) : SdJwtVcCredential {

    override val format = SdJwtVcCredentialFormat

    override fun withIssuer(issuer: IssuerInfo) = copy(issuer = issuer)

    override fun withStatus(status: StatusInfo) = copy(status = status)

    override fun claims(toDisclose: Set<DistinctClaimsPathPointer>): JsonObject {
        return JsonObject(
            sdJwt
                .present(
                    toDisclose.mapTo(mutableSetOf()) {
                        JsonPointer.parse(it.toJsonPointer()) ?: error("Invalid JSON Pointer")
                    }
                )
                ?.recreateClaims { it.jwtClaimsSet.asClaims() }
                ?: throw IllegalArgumentException("Invalid toDisclose set")
        )
    }
}

fun EudiSdJwtVcCredential(sdJwt: SdJwt.Issuance<SignedJWT>): EudiSdJwtVcCredential {
    val rawClaims = JsonObject(sdJwt.jwt.jwtClaimsSet.asClaims())
    val claims = JsonObject(sdJwt.recreateClaims { rawClaims })
    return EudiSdJwtVcCredential(
        vct = claims["vct"]?.jsonStringContent() ?: throw IllegalArgumentException("Missing vct"),
        issuer = IssuerInfo(identifier = claims.iss()),
        issuedAt = Instant.ofEpochSecond(claims.iat()),
        validFrom = claims.nbf()?.let(Instant::ofEpochSecond),
        expiresAt = claims.exp()?.let(Instant::ofEpochSecond),
        claims = claims,
        discloseable = sdJwt.discloseable(rawClaims, claims),
        status = null,
        sdJwt = sdJwt,
        sdJwtVcAsString = sdJwt.serialize(),
        kbJwt = null,
    )
}

fun EudiSdJwtVcCredential(sdJwtVcAsString: String): EudiSdJwtVcCredential {
    val (rawClaims, sdJwt, kbJwt) = parseSdJwtVc(sdJwtVcAsString)
    val claims = JsonObject(sdJwt.recreateClaims { rawClaims })
    return EudiSdJwtVcCredential(
        vct = claims["vct"]?.jsonStringContent() ?: throw IllegalArgumentException("Missing vct"),
        issuer = IssuerInfo(identifier = claims.iss()),
        issuedAt = Instant.ofEpochSecond(claims.iat()),
        validFrom = claims.nbf()?.let(Instant::ofEpochSecond),
        expiresAt = claims.exp()?.let(Instant::ofEpochSecond),
        claims = claims,
        discloseable = sdJwt.discloseable(rawClaims, claims),
        status = null,
        sdJwt = sdJwt,
        sdJwtVcAsString = sdJwtVcAsString,
        kbJwt = kbJwt,
    )
}

private fun SdJwt<JWT>.discloseable(
    rawClaims: JsonObject,
    claims: JsonObject,
): Set<DistinctClaimsPathPointer> {
    val result = mutableSetOf<DistinctClaimsPathPointer>()
    recreateClaims(
        visitor = { pointer, disclosure ->
            if (disclosure != null) {
                result.add(DistinctClaimsPathPointer.fromJsonPointer(pointer.toString(), claims))
            }
        },
        claimsOf = { rawClaims },
    )
    return result
}

private fun JsonObject.iss() =
    get("iss")?.jsonStringContent() ?: throw SpecificIllegalArgumentException(MISSING_CLAIM, "iss")

private fun JsonObject.iat(): Long {
    val iat = get("iat") ?: throw SpecificIllegalArgumentException(MISSING_CLAIM, "iat")
    require(iat is JsonPrimitive && iat.isNonNegativeInteger) {
        throw SpecificIllegalArgumentException(INVALID_CLAIM, "iat")
    }
    return iat.content.toLong()
}

private fun JsonObject.exp(): Long? {
    val exp = get("exp")
    require(exp is JsonPrimitive? && exp?.isNonNegativeInteger != false) {
        throw SpecificIllegalArgumentException(INVALID_CLAIM, "exp")
    }
    return exp?.content?.toLong()
}

private fun JsonObject.nbf(): Long? {
    val nbf = get("nbf")
    require(nbf is JsonPrimitive? && nbf?.isNonNegativeInteger != false) {
        throw SpecificIllegalArgumentException(INVALID_CLAIM, "nbf")
    }
    return nbf?.content?.toLong()
}

private fun parseSdJwtVc(
    sdJwtVcAsString: String
): Triple<JsonObject, SdJwt.Issuance<SignedJWT>, JWT?> {
    val claims: JsonObject
    val sdJwt: SdJwt<SignedJWT>
    var kbJwt: JWT? = null

    if (sdJwtVcAsString.endsWith("~")) {
        val jwtAndClaimsAndDisclosures =
            runBlocking {
                    SdJwtVerifier.verifyIssuance(
                        JwtSignatureVerifier.NoSignatureValidation,
                        sdJwtVcAsString,
                    )
                }
                .getOrThrow()
        val jwt = SignedJWT.parse(jwtAndClaimsAndDisclosures.jwt.first)
        claims = JsonObject(jwtAndClaimsAndDisclosures.jwt.second)
        sdJwt = SdJwt.Issuance(jwt, jwtAndClaimsAndDisclosures.disclosures)
    } else {
        val jwtAndClaimsAndDisclosuresAndKbJwt =
            runBlocking {
                    SdJwtVerifier.verifyPresentation(
                        JwtSignatureVerifier.NoSignatureValidation,
                        KeyBindingVerifier.MustBePresent,
                        sdJwtVcAsString,
                    )
                }
                .getOrThrow()
        val jwt = SignedJWT.parse(jwtAndClaimsAndDisclosuresAndKbJwt.first.jwt.first)
        kbJwt = SignedJWT.parse(jwtAndClaimsAndDisclosuresAndKbJwt.second?.first)
        claims = JsonObject(jwtAndClaimsAndDisclosuresAndKbJwt.first.jwt.second)
        sdJwt = SdJwt.Issuance(jwt, jwtAndClaimsAndDisclosuresAndKbJwt.first.disclosures)
    }

    return Triple(claims, sdJwt, kbJwt)
}
