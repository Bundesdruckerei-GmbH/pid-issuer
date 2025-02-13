/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.exception.IdentificationError;
import de.governikus.panstar.sdk.utils.constant.SamlError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SamlCodeTranslations {
    public static IdentificationError getPidiError(final SamlError samlError) {
        return switch (samlError) {
            case DOCUMENT_BLOCKED,
                 DOCUMENT_EXPIRED,
                 EID_TYPE_FORBIDDEN,
                 EID_TYPE_UNKNOWN,
                 PA_SIGNATURE                                   -> IdentificationError.CERTIFICATE_ERROR;
            case PAOS_CANCELLED_BY_USER,
                 COMMON_INTERNAL_ERROR                          -> IdentificationError.ABORTED;
            case REQUEST_ATTESTATION_FORMAT_UNSUPPORTED,
                 REQUEST_ATTESTATION_INVALID_JSON,
                 REQUEST_ATTESTATION_SUBJECT_REF_UNSUPPORTED,
                 REQUEST_INCOMPLETE,
                 REQUEST_MISSING_TERMINAL_RIGHTS,
                 EIDAS_CANCELLED_BY_USER,
                 EIDAS_INTERNAL_ERROR,
                 EIDAS_GENERATE_NODE_REQUEST,
                 EIDAS_GENERATE_SP_RESPONSE,
                 EIDAS_HANDLE_NODE_RESPONSE,
                 EIDAS_METADATA_NOT_AVAILABLE,
                 EIDAS_LOA_NOT_SATISFIED,
                 EIDAS_NODE_ERROR                               -> IdentificationError.SERVICE_UNAVAILABLE;
        };
    }
}
