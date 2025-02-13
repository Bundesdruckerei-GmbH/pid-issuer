/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.identification.core.model.ResponseData;

public interface EidAuth {

    String createSamlRedirectBindingUrl(String samlId);

    /**
     * @param relayState     from SAML, used to correlate response to request
     * @param samlResponse   from SAML
     * @param sigAlg         the Signature algorithm
     * @param signature      the Signature
     * @param authentication the Authentication that contains the SAML identifier (to be compared with SAML response)
     * @return extracted data from samlResponse
     */
    ResponseData validateSamlResponseAndExtractPseudonym(String relayState, String samlResponse, String sigAlg, String signature, Authentication authentication);

}
