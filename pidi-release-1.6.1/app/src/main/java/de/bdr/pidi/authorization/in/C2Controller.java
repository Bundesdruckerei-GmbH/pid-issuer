/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import com.fasterxml.jackson.databind.JsonNode;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.flows.C2FlowController;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PrimaryAdapter
@RestController
@RequestMapping("c2")
public class C2Controller {

    private final AuthenticationDelegate authenticationFlow;

    public C2Controller(C2FlowController flowController, HttpLibraryAdapter httpAdapter) {
        authenticationFlow = new AuthenticationDelegate(httpAdapter, flowController);
    }

    @PostMapping(path = Requests.Paths.PAR, consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<JsonNode> par(@RequestHeader MultiValueMap<String,String> allHeaders,  @RequestParam MultiValueMap<String,String> allParams) {
        return authenticationFlow.handlePar(allHeaders, allParams);
    }

    @GetMapping(path = Requests.Paths.AUTHORIZE)
    public ResponseEntity<String> authorize(@RequestHeader MultiValueMap<String, String> allHeaders, @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleAuthorize(allHeaders, allParams);
    }

    /*
    * The endpoint itself is not specified in OAuth 2.0 or OID4VCI, but the response behaviour must be implemented
    * according to that of the Authorization Endpoint of the Authorization Code Flow in OAuth 2.0.
     */
    @GetMapping(path = Requests.Paths.FINISH_AUTHORIZATION)
    public ResponseEntity<String> finishAuthorization(@RequestHeader MultiValueMap<String, String> allHeaders, @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleFinishAuthorization(allHeaders, allParams);
    }

    @PostMapping(path = Requests.Paths.TOKEN, consumes = "application/x-www-form-urlencoded", produces = "application/json")
    public ResponseEntity<JsonNode> invalidGrantType(@RequestParam MultiValueMap<String,String> allParams) {
        return authenticationFlow.handleInvalidGrantType(allParams);
    }

    @PostMapping(path = Requests.Paths.TOKEN, params = "grant_type=authorization_code", consumes = "application/x-www-form-urlencoded", produces = "application/json")
    public ResponseEntity<JsonNode> token(@RequestHeader MultiValueMap<String,String> allHeaders,  @RequestParam MultiValueMap<String,String> allParams) {
        return authenticationFlow.handleToken(allHeaders, allParams);
    }

    @PostMapping(path = Requests.Paths.CREDENTIAL, consumes = "application/json", produces = "application/json")
    public ResponseEntity<JsonNode> issueCredential(@RequestHeader MultiValueMap<String, String> allHeaders,
                                                  @RequestParam MultiValueMap<String, String> allParams,
                                                  @RequestBody String body) {
        return authenticationFlow.handleCredential(FlowVariant.C2, allHeaders, allParams, body);
    }

    @PostMapping(path = Requests.Paths.PRESENTATION_SIGNING, consumes = "application/json", produces = "application/json")
    public ResponseEntity<JsonNode> signHash(@RequestHeader MultiValueMap<String, String> allHeaders,
                                                    @RequestParam MultiValueMap<String, String> allParams,
                                                    @RequestBody String body) {
        return authenticationFlow.handlePresentationSigning(allHeaders, allParams, body);
    }
}
