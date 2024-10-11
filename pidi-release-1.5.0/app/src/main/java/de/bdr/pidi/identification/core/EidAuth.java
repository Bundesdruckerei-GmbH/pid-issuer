/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.model.Authentication;
import de.bdr.pidi.identification.core.model.ResponseData;

public interface EidAuth {

    String createSamlRedirectBindingUrl(String samlId, String responseUrl);

    /**
     * @param relayState     from SAML, used to correlate response to request
     * @param samlResponse   from SAML
     * @param sigAlg         the Signature algorithm
     * @param signature      the Signature
     * @param responseUrl
     * @param authentication the Authentication that contains the SAML identifier (to be compared with SAML response)
     * @return extracted data from samlResponse
     */
    ResponseData validateSamlResponseAndExtractPseudonym(String relayState, String samlResponse, String sigAlg, String signature, String responseUrl, Authentication authentication);

}
