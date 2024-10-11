/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.assurance.claims.ISO3166_1Alpha2CountryCode;
import de.bdr.openid4vc.common.signing.Signer;
import de.bdr.openid4vc.vci.credentials.mdoc.MDocCredentialConfiguration;
import de.bdr.openid4vc.vci.credentials.mdoc.MDocCredentialCreator;
import de.bdr.openid4vc.vci.credentials.mdoc.MdocData;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.issuance.util.CountryCodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static de.bdr.pidi.base.PidDataConst.MDOC_TYPE;

@Slf4j
public class IssuerSignedMdocCreator extends MDocCredentialCreator {
    private static final DateTimeFormatter BIRTHDATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ConcurrentMap<UUID, PidCredentialData> pidCredentialDataMap = new ConcurrentHashMap<>();

    public IssuerSignedMdocCreator(@NotNull MDocCredentialConfiguration configuration, @NotNull Signer signer) {
        super(configuration, signer);
    }

    @NotNull
    @Override
    public MdocData create(@NotNull UUID key) {
        var data = pidCredentialDataMap.get(key);
        Instant validFrom = Instant.now();
        Instant validUntil = validFrom.plus(getConfiguration().getLifetime());
        Map<String, Map<String, Object>> namespaces = Map.of(MDOC_TYPE, getMdocDataMap(data, validFrom, validUntil));

        return new MdocData(validFrom, validUntil, namespaces);
    }

    public void putPidCredentialData(UUID key, PidCredentialData value) {
        pidCredentialDataMap.put(key, value);
    }

    public void removePidCredentialData(UUID key) {
        pidCredentialDataMap.remove(key);
    }

    protected static Map<String, Object> getMdocDataMap(PidCredentialData data, Instant issuanceDate, Instant expiryDate) {
        var ageInYears = Period.between(data.getBirthdate(), LocalDate.now()).getYears();
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("family_name", data.getFamilyName());
        dataMap.put("given_name", data.getGivenName());
        dataMap.put("birth_date", BIRTHDATE_FORMATTER.format(data.getBirthdate()));
        dataMap.put("age_in_years", ageInYears);
        dataMap.put("age_birth_year", data.getBirthdate().getYear());
        dataMap.put("age_over_12", ageInYears >= 12);
        dataMap.put("age_over_14", ageInYears >= 14);
        dataMap.put("age_over_16", ageInYears >= 16);
        dataMap.put("age_over_18", ageInYears >= 18);
        dataMap.put("age_over_21", ageInYears >= 21);
        dataMap.put("age_over_65", ageInYears >= 65);
        dataMap.computeIfAbsent("family_name_birth", key -> data.getBirthFamilyName());

        if (data.getPlaceOfBirth() != null) {
            dataMap.computeIfAbsent("birth_place", key -> data.getPlaceOfBirth().getLocality());
        }

        if (data.getAddress() != null) {
            dataMap.computeIfAbsent("resident_address", key -> data.getAddress().getFormatted());
            if (data.getAddress().getCountry() != null) {
                dataMap.computeIfAbsent("resident_country", key -> convertCountryCode(data.getAddress().getCountry()));
            }
            dataMap.computeIfAbsent("resident_state", key -> data.getAddress().getRegion());
            dataMap.computeIfAbsent("resident_city", key -> data.getAddress().getLocality());
            dataMap.computeIfAbsent("resident_postal_code", key -> data.getAddress().getPostalCode());
            dataMap.computeIfAbsent("resident_street", key -> data.getAddress().getStreetAddress());
        }
        if (data.getNationality() != null) {
            dataMap.computeIfAbsent("nationality", key -> convertCountryCode(data.getNationality()));
        }

        dataMap.put("issuance_date", Date.from(issuanceDate));
        dataMap.put("expiry_date", Date.from(expiryDate));
        dataMap.put("issuing_authority", ISO3166_1Alpha2CountryCode.DE.toString());
        dataMap.put("issuing_country", ISO3166_1Alpha2CountryCode.DE.toString());

        return dataMap;
    }

    private static String convertCountryCode(String countryCode) {
        try {
            return CountryCodeMapper.mapToISO3166_1Alpha2CountryCode(countryCode);
        } catch (ParseException e) {
            log.error("Could not parse countrycode {}", countryCode, e);
            throw new PidServerException("PID could not get issued due to an data issue. Please contact the support of Bundesdruckerei GmbH.", e);
        }
    }
}
