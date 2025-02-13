/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.core.configuration.MultiSamlConfiguration;
import de.bdr.revocation.identification.core.exception.AuthenticationStateException;
import de.bdr.revocation.identification.core.exception.CryptoConfigException;
import de.bdr.revocation.identification.core.exception.EidWrappingException;
import de.bdr.revocation.identification.core.exception.IdentificationError;
import de.bdr.revocation.identification.core.exception.MissingAuthDataException;
import de.bdr.revocation.identification.core.exception.SamlCryptoConfigException;
import de.bdr.revocation.identification.core.exception.SamlResponseValidationFailedException;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.identification.core.model.ResponseData;
import de.governikus.panstar.sdk.saml.configuration.SamlConfiguration;
import de.governikus.panstar.sdk.saml.exception.SamlAuthenticationException;
import de.governikus.panstar.sdk.saml.exception.SamlRequestException;
import de.governikus.panstar.sdk.saml.exception.UnsuccessfulSamlAuthenticationProcessException;
import de.governikus.panstar.sdk.saml.request.SamlRequestGenerator;
import de.governikus.panstar.sdk.saml.response.ProcessedSamlResult;
import de.governikus.panstar.sdk.saml.response.SamlResponseHandler;
import de.governikus.panstar.sdk.utils.RequestData;
import de.governikus.panstar.sdk.utils.exception.InvalidInputException;
import de.governikus.panstar.sdk.utils.saml.SAMLUtils;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.config.InitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EidAuthAdapter implements EidAuth {

    private final MultiSamlConfiguration multiSamlConfiguration;
    private final Map<SamlConfiguration, SamlRequestGenerator> requestGeneratorMap = new ConcurrentHashMap<>();
    private final Map<SamlConfiguration, SamlResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();

    public EidAuthAdapter(@Qualifier("samlConfig") MultiSamlConfiguration config) {
        this.multiSamlConfiguration = config;
    }

    @Override
    public synchronized String createSamlRedirectBindingUrl(String sessionId) {

        var requestedData = new RequestData().restrictedID(true);

        try {
            // build the SAML request query string that will be sent to the eID-server
            return requestGenerator(multiSamlConfiguration.getConfigurations().getFirst())
                    .createSamlRequestUrl(requestedData, sessionId);
        } catch (InvalidInputException | SamlRequestException e) {
            throw new SamlCryptoConfigException("failed to SAML request", true, e);
        }
    }

    private SamlRequestGenerator requestGenerator(SamlConfiguration samlConfiguration) {
        var requestGenerator = this.requestGeneratorMap.get(samlConfiguration);
        if (requestGenerator == null) {
            try {
                requestGenerator = new SamlRequestGenerator(samlConfiguration);
                this.requestGeneratorMap.put(samlConfiguration, requestGenerator);
            } catch (InitializationException | InvalidInputException | SamlRequestException e) {
                throw new CryptoConfigException("failed to initialize PanStar SDK", e);
            }
        }
        return requestGenerator;
    }

    private ProcessedSamlResult validateAndExtractAttributes(String samlResponse, String relayState, String sigAlg, String signature) {
        ProcessedSamlResult result;
        var samlConfiguration = validateCertificatesAgainstQuery(samlResponse, relayState, sigAlg, signature);
        try {
            result = responseHandler(samlConfiguration).parseSamlResponse(samlResponse, relayState, signature, sigAlg);
        } catch (SamlAuthenticationException | InvalidInputException e) {
            throw new SamlResponseValidationFailedException("could not decode the SAML Response", null, e);
        } catch (UnsuccessfulSamlAuthenticationProcessException e) {
            throw toSamlResponseValidationFailedException(e, "could not resolve the SAML Response");
        }
        return result;
    }

    /**
     * validate the signature of the SAML response against the configured certificates and selects the matching one
     * (per ThreadLocal).
     *
     * @param samlResponse the <em>SAMLResponse</em> query parameter
     * @param relayState   the <em>RelayState</em> query parameter, may be <code>null</code>
     * @param sigAlg       the <em>SigAlg</em> query parameter
     * @param signature    the <em>Signature</em> query parameter
     * @return the <code>SamlConfiguration</code> that was successful in verifying the signature
     * @throws SamlCryptoConfigException or SamlResponseValidationFailedException if the response could not be verified
     *                                   and increases the failure counter
     */
    private SamlConfiguration validateCertificatesAgainstQuery(String samlResponse, String relayState, String sigAlg, String signature) {
        for (var configuration : multiSamlConfiguration.getConfigurations()) {
            if (SAMLUtils.checkQuerySignature(samlResponse, relayState, sigAlg, signature,
                    configuration.getSamlKeyMaterial().getSamlResponseSignatureValidatingCertificate(), false)) {
                return configuration;
            }
        }
        throw toSamlResponseValidationFailedException(null, "no certificate found that validates signature");
    }

    private SamlResponseHandler responseHandler(SamlConfiguration samlConfiguration) {
        return Optional.ofNullable(this.responseHandlerMap.get(samlConfiguration)).orElseGet(() -> {
            try {
                var responseHandler = new SamlResponseHandler(samlConfiguration);
                storeResponseHandlerForConfiguration(samlConfiguration, responseHandler);
                return responseHandler;
            } catch (InitializationException | InvalidInputException e) {
                throw new CryptoConfigException("failed to initialize PanStar SDK", e);
            }
        });
    }

    void storeResponseHandlerForConfiguration(SamlConfiguration samlConfiguration, SamlResponseHandler responseHandler) {
        this.responseHandlerMap.put(samlConfiguration, responseHandler);
    }

    private SamlResponseValidationFailedException toSamlResponseValidationFailedException(UnsuccessfulSamlAuthenticationProcessException e, String message) {
        var visibleCode = translateSamlStatusCodeToOurVisibleCode(e).map(IdentificationError::toString).orElse(EidWrappingException.ERR_CODE_SYSTEM);

        return new SamlResponseValidationFailedException(message, visibleCode, e);
    }

    Optional<IdentificationError> translateSamlStatusCodeToOurVisibleCode(UnsuccessfulSamlAuthenticationProcessException e) {
        if (e == null || e.getSamlError() == null) {
            return Optional.empty();
        }
        return Optional.of(SamlCodeTranslations.getPidiError(e.getSamlError()));
    }

    @Override
    public ResponseData validateSamlResponseAndExtractPseudonym(String relayState, String samlResponse, String sigAlg,
                                                                String signature,
                                                                Authentication authentication) {
        ProcessedSamlResult processed =
                validateAndExtractAttributes(samlResponse, relayState, sigAlg, signature);
        return mapEidResponse(relayState, authentication, processed);
    }

    ResponseData mapEidResponse(String relayState, Authentication authentication, ProcessedSamlResult processed) {
        if (processed == null) {
            throw new MissingAuthDataException("the SAML Response did not include the attributes");
        }
        if (!relayState.equals(authentication.getSamlId())) {
            throw new AuthenticationStateException("SAML ID from Authentication did not match the relayState");
        }
        return new ResponseData(Base64.getUrlEncoder().encodeToString(processed.getPersonalData().getRestrictedID().getID()));
    }
}
