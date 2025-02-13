/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.core.exception.IdentificationError;
import de.governikus.panstar.sdk.utils.constant.SamlError;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static de.governikus.panstar.sdk.utils.constant.SamlError.COMMON_INTERNAL_ERROR;
import static de.governikus.panstar.sdk.utils.constant.SamlError.DOCUMENT_BLOCKED;
import static de.governikus.panstar.sdk.utils.constant.SamlError.DOCUMENT_EXPIRED;
import static de.governikus.panstar.sdk.utils.constant.SamlError.EID_TYPE_FORBIDDEN;
import static de.governikus.panstar.sdk.utils.constant.SamlError.EID_TYPE_UNKNOWN;
import static de.governikus.panstar.sdk.utils.constant.SamlError.PAOS_CANCELLED_BY_USER;
import static de.governikus.panstar.sdk.utils.constant.SamlError.PA_SIGNATURE;
import static de.governikus.panstar.sdk.utils.constant.SamlError.REQUEST_ATTESTATION_FORMAT_UNSUPPORTED;
import static de.governikus.panstar.sdk.utils.constant.SamlError.REQUEST_ATTESTATION_INVALID_JSON;
import static de.governikus.panstar.sdk.utils.constant.SamlError.REQUEST_ATTESTATION_SUBJECT_REF_UNSUPPORTED;
import static de.governikus.panstar.sdk.utils.constant.SamlError.REQUEST_INCOMPLETE;
import static de.governikus.panstar.sdk.utils.constant.SamlError.REQUEST_MISSING_TERMINAL_RIGHTS;

class SamlCodeTranslationsTest {

    private static Stream<Arguments> samlPidiErrorTranslations() {
        return Stream.of(
                Arguments.of(DOCUMENT_BLOCKED, IdentificationError.CERTIFICATE_ERROR),
                Arguments.of(DOCUMENT_EXPIRED, IdentificationError.CERTIFICATE_ERROR),
                Arguments.of(EID_TYPE_FORBIDDEN, IdentificationError.CERTIFICATE_ERROR),
                Arguments.of(EID_TYPE_UNKNOWN, IdentificationError.CERTIFICATE_ERROR),
                Arguments.of(PA_SIGNATURE, IdentificationError.CERTIFICATE_ERROR),
                Arguments.of(PAOS_CANCELLED_BY_USER, IdentificationError.ABORTED),
                Arguments.of(COMMON_INTERNAL_ERROR, IdentificationError.ABORTED),
                Arguments.of(REQUEST_ATTESTATION_FORMAT_UNSUPPORTED, IdentificationError.SERVICE_UNAVAILABLE),
                Arguments.of(REQUEST_ATTESTATION_INVALID_JSON, IdentificationError.SERVICE_UNAVAILABLE),
                Arguments.of(REQUEST_ATTESTATION_SUBJECT_REF_UNSUPPORTED, IdentificationError.SERVICE_UNAVAILABLE),
                Arguments.of(REQUEST_INCOMPLETE, IdentificationError.SERVICE_UNAVAILABLE),
                Arguments.of(REQUEST_MISSING_TERMINAL_RIGHTS, IdentificationError.SERVICE_UNAVAILABLE)
        );
    }

    @ParameterizedTest
    @MethodSource("samlPidiErrorTranslations")
    void shouldCorrectMapped(SamlError error, IdentificationError expected ) {
        Assertions.assertEquals(expected, SamlCodeTranslations.getPidiError(error));
    }
}