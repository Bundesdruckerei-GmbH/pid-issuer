/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.issuance;

import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;

import java.text.ParseException;

public interface SdJwtBuilder<T extends FormatSpecificCredentialRequest> {
    String build(PidCredentialData pidCredentialData, T credentialRequest, String holderBindingKey) throws ParseException, FaultyRequestParameterException;
}
