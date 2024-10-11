/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in;

import com.fasterxml.jackson.databind.JsonNode;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.flows.B1FlowController;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
@RequestMapping("b1")
public class B1Controller {

    private final AuthenticationDelegate authenticationFlow;
    private final B1FlowController flowController;
    private final HttpLibraryAdapter httpAdapter;

    public B1Controller(B1FlowController flowController, HttpLibraryAdapter httpAdapter) {
        this.flowController = flowController;
        this.httpAdapter = httpAdapter;
        authenticationFlow = new AuthenticationDelegate(httpAdapter, flowController);
    }

    @PostMapping(path = Requests.Paths.PAR, consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<JsonNode> par(@RequestHeader MultiValueMap<String, String> allHeaders, @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handlePar(allHeaders, allParams);
    }

    @GetMapping(path = Requests.Paths.AUTHORIZE)
    public ResponseEntity<String> authorize(@RequestHeader MultiValueMap<String, String> allHeaders, @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleAuthorize(allHeaders, allParams);
    }

    @GetMapping(path = Requests.Paths.FINISH_AUTHORIZATION)
    public ResponseEntity<String> finishAuthorization(@RequestHeader MultiValueMap<String, String> allHeaders,
                                                      @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleFinishAuthorization(allHeaders, allParams);
    }

    @PostMapping(path = Requests.Paths.TOKEN, consumes = "application/x-www-form-urlencoded", produces = "application/json")
    public ResponseEntity<JsonNode> invalidGrantType(@RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleInvalidGrantType(allParams);
    }

    @PostMapping(path = Requests.Paths.TOKEN, params = "grant_type=authorization_code", consumes = "application/x-www-form-urlencoded", produces = "application/json")
    public ResponseEntity<JsonNode> token(@RequestHeader MultiValueMap<String, String> allHeaders, @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleToken(allHeaders, allParams);
    }

    @PostMapping(path = Requests.Paths.TOKEN, params = "grant_type=urn:ietf:params:oauth:grant-type:seed_credential", consumes = "application/x-www-form-urlencoded", produces = "application/json")
    public ResponseEntity<JsonNode> seedCredentialToken(@RequestHeader MultiValueMap<String, String> allHeaders, @RequestParam MultiValueMap<String, String> allParams) {
        return authenticationFlow.handleSeedCredentialToken(allHeaders, allParams);
    }

    @PostMapping(path = Requests.Paths.CREDENTIAL, consumes = "application/json", produces = "application/json")
    public ResponseEntity<JsonNode> issueCredential(@RequestHeader MultiValueMap<String, String> allHeaders,
                                                    @RequestParam MultiValueMap<String, String> allParams,
                                                    @RequestBody String body) {
        return authenticationFlow.handleSeedCredential(FlowVariant.B1, allHeaders, allParams, body);
    }

    @PostMapping(path = Requests.Paths.SESSION, produces = "application/json")
    public ResponseEntity<JsonNode> issueSession() {
        HttpRequest<?> request = httpAdapter.getLibraryHttpRequest(HttpMethod.POST, HttpHeaders.EMPTY, HttpHeaders.EMPTY);
        WResponseBuilder response = flowController.processSessionRequest(request);
        return response.buildJSONResponseEntity();
    }
}
