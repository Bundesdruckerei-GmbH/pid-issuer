/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.service.IssuerMetadataService;
import de.bdr.pidi.authorization.in.model.SdJwtVcMetadata;
import de.bdr.pidi.authorization.in.model.SdJwtVcMetadataKeys;
import de.bdr.pidi.base.PidServerException;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

@PrimaryAdapter
@RestController
public class MetadataController {
    private static final EnumSet<FlowVariant> ALLOWED_FLOW_VARIANTS = EnumSet.of(FlowVariant.C, FlowVariant.C1, FlowVariant.C2, FlowVariant.B, FlowVariant.B1);
    private static final String DPOP_SIGNING_ALGS =
            JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toJSONString).toList().toString();
    private static final String CREDENTIAL_ISSUER_IDENTIFIER_PLACEHOLDER = "<CredentialIssuerIdentifier>";

    private final String authorizationMetadata;
    private final Map<FlowVariant, String> credentialMetadataMap;
    private final AuthorizationConfiguration authorizationConfiguration;
    private final IssuerMetadataService issuerMetadataService;

    public MetadataController(AuthorizationConfiguration authorizationConfiguration, IssuerMetadataService issuerMetadataService) {
        this.authorizationConfiguration = authorizationConfiguration;
        this.issuerMetadataService = issuerMetadataService;
        authorizationMetadata = readMetadata("metadata/authorization.json");
        var credentialMetadataC = readMetadata("metadata/credential_C.json");
        var credentialMetadataC2 = readMetadata("metadata/credential_C2.json");
        var credentialMetadataB = readMetadata("metadata/credential_B.json");
        var credentialMetadataB1 = readMetadata("metadata/credential_B1.json");
        credentialMetadataMap = Map.of(
                FlowVariant.C, credentialMetadataC, FlowVariant.C1, credentialMetadataC, FlowVariant.C2, credentialMetadataC2,
                FlowVariant.B, credentialMetadataB, FlowVariant.B1, credentialMetadataB1);
        if (!credentialMetadataMap.keySet().containsAll(ALLOWED_FLOW_VARIANTS)) {
            throw new PidServerException("credential metadata mapping incomplete");
        }
    }

    @GetMapping(path = {
            "/{variantPath}/.well-known/oauth-authorization-server",
            "/.well-known/oauth-authorization-server/{variantPath}"}, produces = "application/json")
    public String getAuthorizationMetadata(@PathVariable String variantPath) {
        var flowVariant = validateFlowVariantPath(variantPath);
        var credentialIssuer = authorizationConfiguration.getCredentialIssuerIdentifier(flowVariant);
        return authorizationMetadata.replace(CREDENTIAL_ISSUER_IDENTIFIER_PLACEHOLDER, credentialIssuer)
                .replace("\"<DpopSigningAlgs>\"", DPOP_SIGNING_ALGS);
    }

    @GetMapping(path = "/{variantPath}/.well-known/openid-credential-issuer", produces = "application/json")
    public String getCredentialMetadata(@PathVariable String variantPath) {
        var flowVariant = validateFlowVariantPath(variantPath);
        var credentialIssuer = authorizationConfiguration.getCredentialIssuerIdentifier(flowVariant);
        return credentialMetadataMap.get(flowVariant).replace(CREDENTIAL_ISSUER_IDENTIFIER_PLACEHOLDER, credentialIssuer);
    }

    @GetMapping(path = {
            "/{variantPath}/.well-known/jwt-vc-issuer",
            "/.well-known/jwt-vc-issuer/{variantPath}"}, produces = "application/json")
    public SdJwtVcMetadata getSdJwtVcMetadata(@PathVariable String variantPath) {
        var flowVariant = validateFlowVariantPath(variantPath);
        var jwks = issuerMetadataService.getIssuerJwks();
        if (jwks == null || jwks.isEmpty()) {
            throw new PidServerException("No certificate found for " + variantPath);
        }
        Collection<Object> issuerJwks = new ArrayList<>();
        jwks.forEach(jwk -> issuerJwks.add(jwk.toJSONObject()));
        return new SdJwtVcMetadata(authorizationConfiguration.getCredentialIssuerIdentifier(flowVariant), new SdJwtVcMetadataKeys(issuerJwks));
    }

    private static String readMetadata(String filename) {
        try (var inputStream = MetadataController.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IOException(filename + " not found");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(filename + " not reachable.", e);
        }
    }

    private static FlowVariant validateFlowVariantPath(String path) {
        final FlowVariant flowVariant = FlowVariant.fromUrlPath(path);
        if (flowVariant == null) {
            throw new InvalidRequestException("'%s' is not a known flow variant path".formatted(path));
        }
        if (!ALLOWED_FLOW_VARIANTS.contains(flowVariant)) {
            throw new InvalidRequestException("'%s' is not an allowed flow variant path".formatted(path));
        }
        return flowVariant;
    }
}
