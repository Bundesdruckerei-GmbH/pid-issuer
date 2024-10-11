/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.signing.Pkcs12Signer;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.base.PidDataConst;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class IssuerSignedSdJwtBuilderTest extends SdJwtBuilderTestBase {

    private static SdJwtVcCredentialRequest credentialRequest;
    private static SdJwtBuilder<SdJwtVcCredentialRequest> sdJwtBuilder;
    private static String holderBindingKey;

    private static ECDSAVerifier ecdsaVerifier;

    @BeforeAll
    static void setUp() throws JOSEException {
        // Create sdJwtBuilder
        var pkcs12Signer = new Pkcs12Signer(Objects.requireNonNull(IssuerSignedSdJwtBuilderTest.class.getResourceAsStream("/keystore/issuance-test-keystore.p12")), "issuance-test");
        var pidSdJwtVcCreator = new PidSdJwtVcCreator("http://localhost:8080/c1", pkcs12Signer, Duration.ofDays(14L));
        sdJwtBuilder = new IssuerSignedSdJwtBuilder(pidSdJwtVcCreator);

        // Create verifier
        ecdsaVerifier = new ECDSAVerifier(pkcs12Signer.getKeys().getJwk().toECKey());

        // Prepare parameters
        holderBindingKey = pkcs12Signer.getKeys().getJwk().toJSONString();
        credentialRequest = new SdJwtVcCredentialRequest(SdJwtVcCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.SD_JWT_VCTYPE);
    }

    @DisplayName("Issuer signed SdJwt contained the correct plain properties iss, issuing_authority, _sd_alg and vct")
    @Test
    void test001() throws ParseException {
        String sdJwt = sdJwtBuilder.build(getPidCredentialData(), credentialRequest, holderBindingKey);

        String decodedPayloadString = getDecodedPayload(sdJwt);

        verifySignature(sdJwt, ecdsaVerifier);
        assertThat(decodedPayloadString).contains("\"issuing_authority\":\"DE\"", "\"vct\":\"https://example.bmi.bund.de/credential/pid/1.0\"", "\"_sd_alg\":\"sha-256\"", "\"iss\":\"http://localhost:8080/c1\"");
    }


    @DisplayName("Issuer signed SdJwt contained a disclosure for all sd properties")
    @Test
    void test002() throws ParseException, NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        String sdJwt = sdJwtBuilder.build(getPidCredentialData(), credentialRequest, holderBindingKey);

        String decodedPayload = getDecodedPayload(sdJwt);
        String decodedSignature = getSignature(sdJwt);

        List<String> disclosures = getDisclosures(decodedSignature);
        List<String> listSdHashes = getAllSdHashVales(decodedPayload);
        List<String> hashedDisclosures = toHashedDisclosures(disclosures, messageDigest);

        verifySignature(sdJwt, ecdsaVerifier);
        assertThat(listSdHashes)
                .hasSameSizeAs(disclosures)
                .hasSameElementsAs(hashedDisclosures);
    }


    @DisplayName("All claims were disclosed")
    @Test
    void test03() throws ParseException {
        Base64.Decoder urlDecoder = Base64.getUrlDecoder();

        String sdJwt = sdJwtBuilder.build(getPidCredentialData(), credentialRequest, holderBindingKey);
        String decodedSignature = getSignature(sdJwt);

        List<String> disclosures = getDisclosures(decodedSignature);

        HashMap<String, JsonNode> parsedDisclosuresMap = disclosures.stream()
                .map(disclosureJson -> new String(urlDecoder.decode(disclosureJson)))
                .map(toJsonNode())
                .collect(Collectors.toMap(disclosureJsonNode -> disclosureJsonNode.get(1).asText(),
                        disclosureJsonNode -> disclosureJsonNode.get(2),
                        (a, b) -> new ArrayNode(OBJECT_MAPPER.getNodeFactory()).add(a).add(b),
                        HashMap::new));

        assertThat(parsedDisclosuresMap.get("family_name").asText()).isEqualTo("familyName");
        assertThat(parsedDisclosuresMap.get("given_name").asText()).isEqualTo("givenName");
        assertThat(parsedDisclosuresMap.get("birthdate").asText()).isEqualTo("2000-01-01");
        assertThat(parsedDisclosuresMap.get("age_birth_year").asInt()).isEqualTo(2000);
        assertThat(parsedDisclosuresMap.get("birth_family_name").asText()).isEqualTo("birthFamilyName");
        assertThat(parsedDisclosuresMap.get("nationalities").size()).isEqualTo(1);
        assertThat(parsedDisclosuresMap.get("nationalities").get(0).asText()).isEqualTo("DE");
        assertThat(parsedDisclosuresMap.get("12").asBoolean()).isTrue();
        assertThat(parsedDisclosuresMap.get("16").asBoolean()).isTrue();
        assertThat(parsedDisclosuresMap.get("18").asBoolean()).isTrue();
        assertThat(parsedDisclosuresMap.get("21").asBoolean()).isTrue();
        assertThat(parsedDisclosuresMap.get("65").asBoolean()).isFalse();
        assertThat(parsedDisclosuresMap.get("locality")).hasSize(2);
        assertThat(parsedDisclosuresMap.get("locality").get(0).asText()).isEqualTo("placeOfBirth");
        assertThat(parsedDisclosuresMap.get("locality").get(1).asText()).isEqualTo("locality");
        assertThat(parsedDisclosuresMap.get("country").asText()).isEqualTo("DE");
        assertThat(parsedDisclosuresMap.get("region").asText()).isEqualTo("region");
        assertThat(parsedDisclosuresMap.get("formatted").asText()).isEqualTo("formatted");
        assertThat(parsedDisclosuresMap.get("postal_code").asText()).isEqualTo("12345");
        assertThat(parsedDisclosuresMap.get("street_address").asText()).isEqualTo("streetAddress");
    }

    @DisplayName("3 letter ICAO country code was supported for nationality")
    @Test
    void test04() {
        // Given
        PidCredentialData pidCredentialData = getPidCredentialDataOfNationality("AZE");

        // When, then
        assertThatNoException().isThrownBy(() -> sdJwtBuilder.build(pidCredentialData, credentialRequest, holderBindingKey));
    }

    @DisplayName("invalid and not included in iso-3166-1 country codes for nationality are not supported")
    @ParameterizedTest
    @ValueSource(strings = {"_", "!-", "123", "ABCD", "AAA", "AA", "Z", "GBD", "GBN", "GBO", "GBS", "GBP", "KS", "RKS", "EU", "EUE"})
    void test05(String invalidCountryCode) {
        // Given
        PidCredentialData pidCredentialData = getPidCredentialDataOfNationality(invalidCountryCode);

        // When, then
        assertThatExceptionOfType(PidServerException.class).isThrownBy(() -> sdJwtBuilder.build(pidCredentialData, credentialRequest, holderBindingKey));
    }
}
