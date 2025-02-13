/*
 * Copyright 2024-2025 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import COSE.CoseException;
import COSE.OneKey;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.vci.service.statuslist.StatusReference;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.issuance.config.PidDataBuilderConfig;
import de.bdr.pidi.issuance.out.revoc.RevocationAdapter;
import de.bdr.pidi.issuance.out.sls.StatusListAdapter;
import de.bdr.pidi.testdata.ValidTestData;
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerAuth;
import de.bundesdruckerei.mdoc.kotlin.core.auth.StatusListInfo;
import de.bundesdruckerei.mdoc.kotlin.core.auth.ValidityInfo;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.bdr.pidi.issuance.ConfigTestData.ISSUANCE_CONFIG;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyCommonParameters.KTY;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyCommonParameters.X;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyCommonParameters.Y;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyTypes.EC2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IssuerSignedMdocBuilderTest extends MdocBuilderBase {
    private static final MsoMdocCredentialRequest CREDENTIAL_REQUEST = new MsoMdocCredentialRequest(MsoMdocCredentialFormat.INSTANCE, null, Collections.emptyList(), null, "eu.europa.ec.eudi.pid.1");
    private static MdocBuilder<MsoMdocCredentialRequest> mdocBuilder;
    private static String holderBindingKey;

    @BeforeAll
    void setUp() throws JOSEException {
        var pidDataBuilderConfig = new PidDataBuilderConfig(ISSUANCE_CONFIG);
        var statusListAdapter = mock(StatusListAdapter.class);
        var revocationAdapter = mock(RevocationAdapter.class);
        Mockito.when(statusListAdapter.acquireFreeIndex(any(FlowVariant.class))).thenReturn(new StatusReference("http://list-uri", 88));

        mdocBuilder = pidDataBuilderConfig.cMdocBuilder(statusListAdapter, revocationAdapter);
        holderBindingKey = JWK.parse(pidDataBuilderConfig.getSigner().getKeys().getCertificates().getFirst()).toJSONString();
    }

    @DisplayName("Mdoc is ISSUER_SIGNED and the Json contained issuerAuth and nameSpaces at the top level")
    @Test
    void test001() throws IOException {
        String mdoc = buildMdoc(IssuerSignedMdocBuilderTest::getPidCredentialData);
        byte[] decodedMdoc = getDecodedString(mdoc);
        Map<String, Object> mdocMap = CBOR_MAPPER.readValue(decodedMdoc, new TypeReference<>() {
        });

        assertThat(mdocMap.get("issuerAuth")).isNotNull();
        assertThat(mdocMap.get("nameSpaces")).isNotNull();
    }

    @DisplayName("Mdoc contained correct validFrom and validUntil")
    @Test
    void test002() throws IOException {
        String mdoc = buildMdoc(IssuerSignedMdocBuilderTest::getPidCredentialData);
        JsonNode issuerAuthNode = readIssuerAuth(mdoc);

        ObjectReader reader = CBOR_MAPPER.readerFor(new TypeReference<List<?>>() {
        });
        List<?> list = reader.readValue(issuerAuthNode);
        CBORObject cborObject = CBORObject.FromObject(list);
        IssuerAuth issuerAuth = IssuerAuth.Companion.fromCBOR(cborObject);
        ValidityInfo validityInfo = issuerAuth.getMso().getValidityInfo();
        Instant validFrom = validityInfo.getValidFrom().toInstant();
        Instant validUntil = validityInfo.getValidUntil().toInstant();
        assertThat(validFrom).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
        assertThat(validUntil).isCloseTo(Instant.now().plus(LIFETIME), within(10, ChronoUnit.SECONDS));

        assertThat(issuerAuth.getMso().getStatus()).isNotNull()
                .extracting("info").isNotNull().isInstanceOf(StatusListInfo.class)
                .extracting("uri").isEqualTo("http://list-uri");
    }

    @DisplayName("Mdoc contained all claims")
    @Test
    void test003() throws IOException {
        String mdoc = buildMdoc(IssuerSignedMdocBuilderTest::getPidCredentialData);
        Map<String, Object> mdocPidValues = readPidValues(mdoc);

        assertThat(mdocPidValues.get("family_name")).hasToString("familyName");
        assertThat(mdocPidValues.get("given_name")).hasToString("givenName");
        assertThat(mdocPidValues.get("birth_date")).hasToString(ValidTestData.BIRTH_DATE_TIME);
        assertThat(mdocPidValues.get("age_over_12")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_14")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_18")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_21")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_65")).hasToString("false");
        assertThat(mdocPidValues.get("age_in_years")).hasToString(ValidTestData.AGE_IN_YEARS);
        assertThat(mdocPidValues.get("age_birth_year")).hasToString(ValidTestData.AGE_BIRTH_YEAR);
        assertThat(mdocPidValues.get("family_name_birth")).hasToString("birthFamilyName");
        assertThat(mdocPidValues.get("birth_place")).hasToString("placeOfBirth");
        assertThat(mdocPidValues.get("resident_address")).hasToString("formatted");
        assertThat(mdocPidValues.get("resident_country")).hasToString("DE");
        assertThat(mdocPidValues.get("resident_state")).hasToString("region");
        assertThat(mdocPidValues.get("resident_city")).hasToString("locality");
        assertThat(mdocPidValues.get("resident_postal_code")).hasToString("12345");
        assertThat(mdocPidValues.get("resident_street")).hasToString("streetAddress");
        assertThat(mdocPidValues.get("nationality")).hasToString("DE");
        assertThat(mdocPidValues.get("issuing_authority")).hasToString("DE");
        assertThat(mdocPidValues.get("issuing_country")).hasToString("DE");
        Instant issuanceDate = Instant.parse((String) mdocPidValues.get("issuance_date"));
        Instant expiryDate = Instant.parse((String) mdocPidValues.get("expiry_date"));
        assertThat(issuanceDate).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
        assertThat(expiryDate).isCloseTo(Instant.now().plus(LIFETIME), within(10, ChronoUnit.SECONDS));
    }

    @NotNull
    private static Map<String, Object> readPidValues(String mdoc) throws IOException {
        byte[] decodedMdoc = getDecodedString(mdoc);
        JsonNode jsonNode = CBOR_MAPPER.readTree(decodedMdoc).findValue(DOC_TYPE);
        ObjectReader reader = CBOR_MAPPER.readerFor(new TypeReference<List<byte[]>>() {
        });
        List<byte[]> list = reader.readValue(jsonNode);
        return list.stream().map(IssuerSignedMdocBuilderTest::getDecodedMdocValue).collect(Collectors.toMap(
                DecodedMdocPidValue::elementIdentifier, DecodedMdocPidValue::elementValue));
    }

    @DisplayName("Mdoc built correct with minimal data")
    @Test
    void test004() throws IOException {
        String mdoc = buildMdoc(IssuerSignedMdocBuilderTest::getMinimalPidCredentialData);
        Map<String, Object> mdocPidValues = readPidValues(mdoc);

        assertThat(mdocPidValues.get("family_name")).hasToString("familyName");
        assertThat(mdocPidValues.get("given_name")).hasToString("givenName");
        assertThat(mdocPidValues.get("birth_date")).hasToString(ValidTestData.BIRTH_DATE_TIME);
        assertThat(mdocPidValues.get("age_over_12")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_14")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_18")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_21")).hasToString("true");
        assertThat(mdocPidValues.get("age_over_65")).hasToString("false");
        assertThat(mdocPidValues.get("age_in_years")).hasToString(ValidTestData.AGE_IN_YEARS);
        assertThat(mdocPidValues.get("age_birth_year")).hasToString(ValidTestData.AGE_BIRTH_YEAR);
        assertThat(mdocPidValues.get("family_name_birth")).isNull();
        assertThat(mdocPidValues.get("birth_place")).isNull();
        assertThat(mdocPidValues.get("resident_address")).isNull();
        assertThat(mdocPidValues.get("resident_country")).isNull();
        assertThat(mdocPidValues.get("resident_state")).isNull();
        assertThat(mdocPidValues.get("resident_city")).isNull();
        assertThat(mdocPidValues.get("resident_postal_code")).isNull();
        assertThat(mdocPidValues.get("resident_street")).isNull();
        assertThat(mdocPidValues.get("nationality")).isNull();
        assertThat(mdocPidValues.get("issuing_authority")).hasToString("DE");
        assertThat(mdocPidValues.get("issuing_country")).hasToString("DE");
    }

    @DisplayName("Mdoc issuerAuth (MSO) COSE_Sign1 was signed with ECDSA w/ SHA-256")
    @Test
    void test005() throws IOException {
        // When
        String mdoc = buildMdoc(/* Given */ IssuerSignedMdocBuilderTest::getPidCredentialData);

        // Then
        JsonNode issuerAuthNode = readIssuerAuth(mdoc);

        /*
          COSE_Sign1 = [
           Headers,
           payload : bstr / nil,
           signature : bstr
          ]
         */

        /* Example
           18(
             [
               / protected / h'a10126' / {
                   \ alg \ 1:-7 \ ECDSA 256 \
                 } / ,
               / unprotected / {
                 / kid / 4:'11'
               },
               / payload / 'This is the content.',
               / signature / h'8eb33e4ca31d1c465ab05aac34cc6b23d58fef5c083106c4
           d25a91aef0b0117e2af9a291aa32e14ab834dc56ed2a223444547e01f11d3b0916e5
           a4c345cacb36'
             ]
           )
         */

        JsonNode protectedHeaderParameters = CBOR_MAPPER.readTree(issuerAuthNode.get(0).binaryValue());
        assertThat(protectedHeaderParameters.get(String.valueOf(CoseHeaderParameters.ALG.label)).asInt()).isEqualTo(CoseAlgorithms.ECDSA.value);
    }

    @DisplayName("Mdoc issuerAuth (MSO) COSE_Sign1 contained key chain in unprotected header")
    @Test
    void test006() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = IssuerSignedMdocBuilderTest::getPidCredentialData;
        Certificate[] deviceBindingKeyCertificateChain = givenDeviceBindingKeyCertificateChain();

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        JsonNode issuerAuthNode = readIssuerAuth(mdoc);

        /*
          COSE_Sign1 = [
           Headers,
           payload : bstr / nil,
           signature : bstr
          ]
         */

        /* Example
           18(
             [
               / protected / h'a10126' / {
                   \ alg \ 1:-7 \ ECDSA 256 \
                 } / ,
               / unprotected / {
                 / kid / 4:'11'
               },
               / payload / 'This is the content.',
               / signature / h'8eb33e4ca31d1c465ab05aac34cc6b23d58fef5c083106c4
           d25a91aef0b0117e2af9a291aa32e14ab834dc56ed2a223444547e01f11d3b0916e5
           a4c345cacb36'
             ]
           )
         */

        JsonNode unprotectedHeaderParameters = issuerAuthNode.get(1);
        List<? extends Certificate> certificateChain = getCertificateChain(unprotectedHeaderParameters);

        assertThat(certificateChain.size()).isEqualTo(deviceBindingKeyCertificateChain.length).isEqualTo(1);
        assertThat(certificateChain.getFirst()).isEqualTo(deviceBindingKeyCertificateChain[0]);
    }

    @DisplayName("Mdoc issuerAuth (MSO) was structured according to ISO/IEC 18013-5")
    @Test
    void test007() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = IssuerSignedMdocBuilderTest::getPidCredentialData;

        var publicKey = (ECPublicKey) givenDeviceBindingKeyCertificateChain()[0].getPublicKey();
        BigInteger deviceBindingKeyX = publicKey.getW().getAffineX();
        BigInteger deviceBindingKeyY = publicKey.getW().getAffineY();

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        JsonNode issuerAuthNode = readIssuerAuth(mdoc);

        JsonNode msoWrappedInBst = CBOR_MAPPER.readTree(issuerAuthNode.get(2).binaryValue()); // "The payload is wrapped in a bstr to ensure that it is transported without changes."
        JsonNode mso = CBOR_MAPPER.readTree(msoWrappedInBst.binaryValue());

        assertThat(mso.get("version").asText()).isEqualTo("1.0");
        assertThat(mso.get("digestAlgorithm").asText()).isEqualTo("SHA-256");
        assertThat(mso.get("docType").asText()).isEqualTo(DOC_TYPE);

        JsonNode deviceKey = mso.get("deviceKeyInfo").get("deviceKey");
        assertThat(deviceKey.get(KTY.labelStr()).asInt()).isEqualTo(EC2.value);

        BigInteger msoDeviceKeyX = new BigInteger(1, deviceKey.get(X.labelStr()).binaryValue());
        assertThat(msoDeviceKeyX).isEqualTo(deviceBindingKeyX);

        BigInteger msoDeviceKeyY = new BigInteger(1, deviceKey.get(Y.labelStr()).binaryValue());
        assertThat(msoDeviceKeyY).isEqualTo(deviceBindingKeyY);
    }

    @DisplayName("issuance_date and expiry_date where encoded as 'tdate = #6.0(tstr)' or 'full-date = #6.1004(tstr)'")
    @Test
    void test008() throws IOException {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = IssuerSignedMdocBuilderTest::getPidCredentialData;

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        JsonNode pidNameSpaceNode = readPidNameSpace(mdoc);
        Stream<JsonNode> nameSpaceDataElementNodes = StreamSupport.stream(Spliterators.spliteratorUnknownSize(pidNameSpaceNode.iterator(), Spliterator.ORDERED), false);
        List<CBORObject> pidMetadataDateObjects = nameSpaceDataElementNodes.map(IssuerSignedMdocBuilderTest::decodeFromBytes)
                .filter(o -> o.get("elementIdentifier").AsString().equals("issuance_date")
                        || o.get("elementIdentifier").AsString().equals("expiry_date"))
                .toList();

        assertThat(pidMetadataDateObjects).isNotEmpty().allSatisfy(o ->
                assertThat(o.get("elementValue")).satisfies(
                        IssuerSignedMdocBuilderTest::assertTypeIsStandardDateTimeString)
        );
    }

    private static void assertTypeIsStandardDateTimeString(CBORObject v) {
        assertThat(v.getType()).isEqualTo(CBORType.TextString);
        assertThat(v.isTagged()).isTrue();
        assertThat(v.getMostInnerTag().ToInt32Checked()).isZero();
        assertThatNoException().isThrownBy(() -> Instant.parse(v.AsString()));
    }

    @DisplayName("Mdoc IssuerAuth has correct signature")
    @Test
    void test009() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, CoseException {
        String mdoc = buildMdoc(IssuerSignedMdocBuilderTest::getPidCredentialData);
        byte[] decodedMdoc = getDecodedString(mdoc);
        var issuerAuth = IssuerAuth.Companion.fromCBOR(CBORObject.DecodeFromBytes(decodedMdoc).get("issuerAuth"));
        var pubKey = new OneKey(getSignerPub(), null);
        assertThat(issuerAuth.validate(pubKey)).isTrue();
    }

    @DisplayName("ValidityInfo used 'tdate' type for date/time attributes")
    @Test
    void test010() throws IOException {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = IssuerSignedMdocBuilderTest::getPidCredentialData;

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        JsonNode issuerAuth = readIssuerAuth(mdoc);
        CBORObject msoBstr = decodeFromBytes(issuerAuth.get(2));
        CBORObject mso = decodeFromBstr(msoBstr);
        CBORObject validityInfo = mso.get("validityInfo");
        CBORObject signed = validityInfo.get("signed");
        CBORObject validFrom = validityInfo.get("validFrom");
        CBORObject validUntil = validityInfo.get("validUntil");

        assertThat(signed).satisfies(
                IssuerSignedMdocBuilderTest::assertTypeIsStandardDateTimeString
        );
        assertThat(validFrom).satisfies(
                IssuerSignedMdocBuilderTest::assertTypeIsStandardDateTimeString
        );
        assertThat(validUntil).satisfies(
                IssuerSignedMdocBuilderTest::assertTypeIsStandardDateTimeString
        );
    }

    private static CBORObject decodeFromBstr(CBORObject o) {
        return CBORObject.DecodeFromBytes(o.GetByteString());
    }

    private static CBORObject decodeFromBytes(JsonNode n) {
        try {
            return CBORObject.DecodeFromBytes(n.binaryValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the untagged COSE_Sign1 structure according to RFC 8152 (COSE Single Signer Data Object)
     */
    private static JsonNode readIssuerAuth(String mdoc) throws IOException {
        byte[] decodedMdoc = getDecodedString(mdoc);
        return CBOR_MAPPER.readTree(decodedMdoc).findValue("issuerAuth");
    }

    private static JsonNode readPidNameSpace(String mdoc) throws IOException {
        byte[] decodedMdoc = getDecodedString(mdoc);
        return CBOR_MAPPER.readTree(decodedMdoc).findValue("nameSpaces").findValue(DOC_TYPE);
    }

    private static DecodedMdocPidValue getDecodedMdocValue(byte[] cbor) {
        try {
            return CBOR_MAPPER.readValue(cbor, DecodedMdocPidValue.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record DecodedMdocPidValue(String random, int digestID, String elementIdentifier, Object elementValue) {
    }

    private String buildMdoc(Supplier<PidCredentialData> pidCredentialDataSupplier) {
        PidCredentialData pidCredentialData = pidCredentialDataSupplier.get();
        try {
            return mdocBuilder.build(pidCredentialData, CREDENTIAL_REQUEST, holderBindingKey, FlowVariant.C);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static List<? extends Certificate> getCertificateChain(JsonNode unprotectedHeaderParameters) throws IOException, CertificateException {
        JsonNode x5ChainParameter = unprotectedHeaderParameters.get(String.valueOf(CoseHeaderParameters.X5CHAIN.label));
        byte[] x5ChainDer = x5ChainParameter.binaryValue();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return new ArrayList<>(certificateFactory.generateCertificates(new ByteArrayInputStream(x5ChainDer)));
    }
}
