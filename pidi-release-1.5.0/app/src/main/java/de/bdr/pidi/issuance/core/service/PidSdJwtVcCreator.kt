/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.core.service

import com.nimbusds.oauth2.sdk.ParseException
import com.nimbusds.openid.connect.sdk.assurance.claims.ISO3166_1Alpha2CountryCode
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.vci.credentials.FeatureMode.OPTIONAL
import de.bdr.openid4vc.vci.credentials.FeatureMode.REQUIRED
import de.bdr.openid4vc.vci.credentials.sdjwt.SdJwtVcCredentialConfiguration
import de.bdr.openid4vc.vci.credentials.sdjwt.SdJwtVcCredentialCreator
import de.bdr.pidi.authorization.out.identification.PidCredentialData
import de.bdr.pidi.base.PidDataConst.SD_JWT_PID
import de.bdr.pidi.base.PidDataConst.SD_JWT_VCTYPE
import de.bdr.pidi.base.PidServerException
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest
import de.bdr.pidi.issuance.util.CountryCodeMapper
import eu.europa.ec.eudi.sdjwt.SdObject
import eu.europa.ec.eudi.sdjwt.plain
import eu.europa.ec.eudi.sdjwt.sd
import eu.europa.ec.eudi.sdjwt.sdJwt
import eu.europa.ec.eudi.sdjwt.structured
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Collections.synchronizedMap
import java.util.UUID

class PidSdJwtVcCreator(publicUrl: String, signer: Signer, lifetime: Duration) :
    SdJwtVcCredentialCreator(
        issuer = publicUrl,
        configuration = SdJwtVcCredentialConfiguration(
            id = SD_JWT_PID,
            vct = SD_JWT_VCTYPE,
            keyBinding = true,
            dpop = OPTIONAL,
            par = REQUIRED,
            pkce = REQUIRED,
            lifetime = lifetime,
            numOfDecoysLimit = 1,
        ),
        signer = signer
    ) {
    private val log: Logger = LoggerFactory.getLogger(PidSdJwtVcCreator::class.java)
    private val pidCredentialDataMap: MutableMap<UUID, PidCredentialData> = synchronizedMap(mutableMapOf())

    companion object {
        private val BIRTHDATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    fun putPidCredentialData(key: UUID, value: PidCredentialData) {
        pidCredentialDataMap.put(key, value)
    }

    fun removePidCredentialData(key: UUID) {
        pidCredentialDataMap.remove(key)
    }

    fun convertCountryCode(countryCode: String): String {
        try {
            return CountryCodeMapper.mapToISO3166_1Alpha2CountryCode(countryCode)
        } catch (e: ParseException) {
            log.error("Could not parse countrycode {}", countryCode, e)
            throw PidServerException("PID could not get issued due to an data issue. Please contact the support of Bundesdruckerei GmbH.", e)
        }
    }

    override fun validateCredentialRequest(request: CredentialRequest): Boolean {
        if (request is SdJwtVcCredentialRequest)
            return request.vct == configuration.vct
        if (request is SdJwtVcAuthChannelCredentialRequest)
            return request.vct == configuration.vct
        return false
    }

    override fun createSdObject(issuanceId: UUID): SdObject {
        val data = pidCredentialDataMap.getValue(issuanceId)

        val age = Period.between(data.birthdate, LocalDate.now()).years

        return sdJwt {
            sd {
                put("family_name", data.familyName)
                put("given_name", data.givenName)

                put("birthdate", BIRTHDATE_FORMATTER.format(data.birthdate))
                put("age_birth_year", data.birthdate.year)
                put("age_in_years", age)

                data.birthFamilyName?.let { put("birth_family_name", it) }

                data.nationality?.let {
                    put(
                        "nationalities",
                        JsonArray(
                            listOf(JsonPrimitive(convertCountryCode(it)))
                        )
                    )
                }
            }

            plain {
                put("issuing_country", ISO3166_1Alpha2CountryCode.DE.toString())
                put("issuing_authority", ISO3166_1Alpha2CountryCode.DE.toString())}

            structured("age_equal_or_over") {
                sd {
                    put("12", age >= 12)
                    put("14", age >= 14)
                    put("16", age >= 16)
                    put("18", age >= 18)
                    put("21", age >= 21)
                    put("65", age >= 65)
                }
            }

            data.placeOfBirth?.let { placeOfBirth ->
                if (!placeOfBirth.isEmpty()) {

                    structured("place_of_birth") {
                        sd {
                            placeOfBirth.locality?.let { put("locality", it) }
                            placeOfBirth.country?.let {
                                put("country", convertCountryCode(it))
                            }
                            placeOfBirth.region?.let { put("region", it) }
                        }
                    }
                }
            }

            data.address?.let { address ->
                if (!address.isEmpty()) {
                    structured("address") {
                        sd {
                            address.locality?.let { put("locality", it) }
                            address.country?.let {
                                put("country", convertCountryCode(it))
                            }
                            address.region?.let { put("region", it) }
                            address.formatted?.let { put("formatted", it) }
                            address.postalCode?.let { put("postal_code", it) }
                            address.streetAddress?.let { put("street_address", it) }
                        }
                    }
                }
            }
        }
    }
}
