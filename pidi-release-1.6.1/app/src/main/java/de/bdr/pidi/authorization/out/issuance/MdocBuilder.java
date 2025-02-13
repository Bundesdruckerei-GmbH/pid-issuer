/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.issuance;

import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import java.text.ParseException;

public interface MdocBuilder<T extends FormatSpecificCredentialRequest> {
    String build(PidCredentialData pidCredentialData, T credentialRequest, String holderBindingKey, FlowVariant flowVariant) throws ParseException, FaultyRequestParameterException;
}
