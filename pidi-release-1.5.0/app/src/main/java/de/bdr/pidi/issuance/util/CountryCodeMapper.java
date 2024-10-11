/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.util;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.assurance.claims.CountryCode;
import com.nimbusds.openid.connect.sdk.assurance.claims.ISO3166_1Alpha2CountryCode;
import com.nimbusds.openid.connect.sdk.assurance.claims.ISO3166_1Alpha3CountryCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CountryCodeMapper {
    private static final String UNKNOWN_COUNTRY_CODE_MSG = "Unknown country code: ";

    @SuppressWarnings("java:S100")
    public static String mapToISO3166_1Alpha2CountryCode(String countryCode) throws ParseException {
        if (StringUtils.isBlank(countryCode)) {
            return null;
        }
        if ("D".equalsIgnoreCase(countryCode)) {
            return ISO3166_1Alpha2CountryCode.DE.getValue();
        }
        var cc = CountryCode.parse(countryCode);
        return switch (cc) {
            case ISO3166_1Alpha3CountryCode cc3 ->
                    Optional.ofNullable(cc3.toAlpha2CountryCode())
                            .map(ISO3166_1Alpha2CountryCode::getValue)
                            .orElseThrow(() -> new ParseException(UNKNOWN_COUNTRY_CODE_MSG + cc));
            case ISO3166_1Alpha2CountryCode cc2 when cc2.getCountryName() != null -> cc2.getValue();
            default -> throw new ParseException(UNKNOWN_COUNTRY_CODE_MSG + cc);
        };
    }
}
