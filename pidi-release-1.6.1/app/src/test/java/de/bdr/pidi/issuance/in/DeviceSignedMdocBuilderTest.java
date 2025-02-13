/*
 * Copyright 2024-2025 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.HeaderKeys;
import COSE.OneKey;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.issuance.config.PidDataBuilderConfig;
import de.bdr.pidi.issuance.core.service.EMacKey;
import de.bdr.pidi.testdata.TestUtils;
import de.bdr.pidi.testdata.ValidTestData;
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript;
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerAuth;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceAuthentication;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceSigned;
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.COSEMac0;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static de.bdr.pidi.issuance.ConfigTestData.ISSUANCE_CONFIG;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyCommonParameters.KTY;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyCommonParameters.X;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyCommonParameters.Y;
import static de.bdr.pidi.issuance.in.MdocBuilderBase.CoseKeyTypes.EC2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeviceSignedMdocBuilderTest extends MdocBuilderBase {
    private static final SessionTranscript ST = new SessionTranscript(null, null, CBORObject.FromObject("test").EncodeToBytes());
    private static MsoMdocAuthChannelCredentialRequest credentialRequest;
    private static PrivateKey devicePrivateKey;
    private static MdocBuilder<MsoMdocAuthChannelCredentialRequest> mdocBuilder;

    @BeforeAll
    void setUp() throws JOSEException {
        var pidDataBuilderConfig = new PidDataBuilderConfig(ISSUANCE_CONFIG);
        mdocBuilder = pidDataBuilderConfig.bMdocBuilder();

        var deviceKeyPair = TestUtils.DEVICE_KEY_PAIR;
        devicePrivateKey = deviceKeyPair.toPrivateKey();
        var verifierPub = new ECKey.Builder(new Curve(Curve.P_256.getName()), deviceKeyPair.toECPublicKey()).build();
        credentialRequest = new MsoMdocAuthChannelCredentialRequest(MsoMdocAuthChannelCredentialFormat.INSTANCE, null, Collections.emptyList(), null, DOC_TYPE, verifierPub, ST.asCBOR().EncodeToBytes());
    }

    @DisplayName("Mdoc is DOCUMENT and has matching top level structure")
    @Test
    void test001() throws IOException {
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getPidCredentialData);
        byte[] decodedMdoc = getDecodedString(mdoc);

        Map<String, Object> mdocMap = CBOR_MAPPER.readValue(decodedMdoc, new TypeReference<>() {});
        assertThat(mdocMap).containsOnlyKeys("docType", "deviceSigned", "issuerSigned");
        Map<String, Object> deviceSigned = CBOR_MAPPER.convertValue(mdocMap.get("deviceSigned"), new TypeReference<>() {});
        assertThat(deviceSigned).containsOnlyKeys("deviceAuth", "nameSpaces");
        Map<String, Object> issuerSigned = CBOR_MAPPER.convertValue(mdocMap.get("issuerSigned"), new TypeReference<>() {});
        assertThat(issuerSigned).containsOnlyKeys("issuerAuth");
    }

    @DisplayName("Mdoc contained correct validFrom and validUntil")
    @Test
    void test002() {
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getPidCredentialData);
        var issuerAuth = readIssuerAuth(mdoc);

        var validityInfo = issuerAuth.getMso().getValidityInfo();
        var validFrom = validityInfo.getValidFrom().toInstant();
        var validUntil = validityInfo.getValidUntil().toInstant();
        assertThat(validFrom).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
        assertThat(validUntil).isCloseTo(Instant.now().plus(LIFETIME), within(10, ChronoUnit.SECONDS));
    }

    @DisplayName("Mdoc contained all claims")
    @Test
    void test003() throws IOException {
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getPidCredentialData);
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

    @DisplayName("Mdoc built correct with minimal data")
    @Test
    void test004() throws IOException {
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getMinimalPidCredentialData);
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
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getPidCredentialData);

        // Then
        var issuerAuth = readIssuerAuth(mdoc);

        var protectedHeader = issuerAuth.getProtectedAttributes();
        Map<Integer, Integer> protectedHeaderParameters = CBOR_MAPPER.readValue(protectedHeader.EncodeToBytes(), new TypeReference<>() {});
        assertThat(protectedHeaderParameters)
                .hasSize(1)
                .containsEntry(CoseHeaderParameters.ALG.label, CoseAlgorithms.ECDSA.value);
    }

    @DisplayName("Mdoc issuerAuth (MSO) COSE_Sign1 contained key chain in unprotected header")
    @Test
    void test006() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = DeviceSignedMdocBuilderTest::getPidCredentialData;
        Certificate[] deviceBindingKeyCertificateChain = givenDeviceBindingKeyCertificateChain();

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        var issuerAuth = readIssuerAuth(mdoc);
        var unprotectedHeader = issuerAuth.getUnprotectedAttributes();
        var certificateChain = getCertificateChain(unprotectedHeader);

        assertThat(certificateChain)
                .hasSize(deviceBindingKeyCertificateChain.length)
                .hasSize(1)
                .containsExactly(deviceBindingKeyCertificateChain);
    }

    @DisplayName("Mdoc issuerAuth (MSO) was structured according to ISO/IEC 18013-5")
    @Test
    void test007() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = DeviceSignedMdocBuilderTest::getPidCredentialData;

        var publicKey = (ECPublicKey) givenDeviceBindingKeyCertificateChain()[0].getPublicKey();
        BigInteger deviceBindingKeyX = publicKey.getW().getAffineX();
        BigInteger deviceBindingKeyY = publicKey.getW().getAffineY();

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        var issuerAuth = readIssuerAuth(mdoc);

        var msoWrappedInBst = CBORObject.DecodeFromBytes(issuerAuth.asCBOR().get(2).GetByteString()); // "The payload is wrapped in a bstr to ensure that it is transported without changes."
        var mso = CBORObject.DecodeFromBytes(msoWrappedInBst.GetByteString());

        assertThat(mso.get("version").AsString()).isEqualTo("1.0");
        assertThat(mso.get("digestAlgorithm").AsString()).isEqualTo("SHA-256");
        assertThat(mso.get("docType").AsString()).isEqualTo(DOC_TYPE);

        var valueDigest0 = mso.get("valueDigests").get(DOC_TYPE).get(0);
        assertThat(valueDigest0.GetByteString()).isEqualTo(new byte[]{0x00});

        var deviceKey = mso.get("deviceKeyInfo").get("deviceKey");
        assertThat(deviceKey.get(KTY.label).AsInt32Value()).isEqualTo(EC2.value);

        BigInteger msoDeviceKeyX = new BigInteger(1, deviceKey.get(X.label).GetByteString());
        assertThat(msoDeviceKeyX).isEqualTo(deviceBindingKeyX);

        BigInteger msoDeviceKeyY = new BigInteger(1, deviceKey.get(Y.label).GetByteString());
        assertThat(msoDeviceKeyY).isEqualTo(deviceBindingKeyY);

        var kaNameSpaces = mso.get("deviceKeyInfo").get("keyAuthorizations").get("nameSpaces");
        assertThat(kaNameSpaces.get(0).AsString()).isEqualTo(DOC_TYPE);
    }

    @DisplayName("issuance_date and expiry_date where encoded as 'tdate = #6.0(tstr)' or 'full-date = #6.1004(tstr)'")
    @Test
    void test008() {
        // Given
        Supplier<PidCredentialData> pidCredentialDataSupplier = DeviceSignedMdocBuilderTest::getPidCredentialData;

        // When
        String mdoc = buildMdoc(pidCredentialDataSupplier);

        // Then
        var pidNameSpace = readPidNameSpace(mdoc);
        var issuanceDate = pidNameSpace.get("issuance_date");
        var expiryDate = pidNameSpace.get("expiry_date");

        assertThat(issuanceDate.getType()).isEqualTo(CBORType.TextString);
        assertThat(issuanceDate.getMostInnerTag().ToInt32Checked()).isZero();
        assertThatNoException().isThrownBy(() -> Instant.parse(issuanceDate.AsString()));

        assertThat(expiryDate.getType()).isEqualTo(CBORType.TextString);
        assertThat(expiryDate.getMostInnerTag().ToInt32Checked()).isZero();
        assertThatNoException().isThrownBy(() -> Instant.parse(expiryDate.AsString()));
    }

    @DisplayName("Mdoc IssuerAuth has correct signature")
    @Test
    void test009() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, CoseException {
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getPidCredentialData);
        var issuerAuth = readIssuerAuth(mdoc);
        var pubKey = new OneKey(getSignerPub(), null);
        assertThat(issuerAuth.validate(pubKey)).isTrue();
    }

    @DisplayName("COSEMac0")
    @Test
    void test010() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, InvalidKeyException, CoseException {
        String mdoc = buildMdoc(DeviceSignedMdocBuilderTest::getPidCredentialData);
        var deviceSigned = readDeviceSigned(mdoc);
        var deviceMac = deviceSigned.getDeviceAuth().getDevAuth().asCBOR();
        var coseMac0 = COSEMac0.Companion.fromCBOR(deviceMac);
        assertThat(coseMac0.GetContent()).isNull();

        // reapply attached content for validation
        var deviceAuthentication = new DeviceAuthentication(ST, DOC_TYPE, deviceSigned.getNameSpaces().asTaggedCBOR());
        coseMac0.SetContent(deviceAuthentication.asBytes());

        // build shared secret with signer pub and device pri
        var sharedSecret = new EMacKey((ECPrivateKey) devicePrivateKey, (ECPublicKey) getSignerPub(), ST);

        assertThat(coseMac0.Validate(sharedSecret.getByte())).isTrue();
        assertThat(coseMac0.getProtectedAttributes().get(HeaderKeys.Algorithm.AsCBOR())).isEqualTo(AlgorithmID.HMAC_SHA_256.AsCBOR());
    }

    @DisplayName("invalid and not included in iso-3166-1 country codes for nationality are not supported")
    @ParameterizedTest
    @ValueSource(strings = {"_", "!-", "123", "BQAQ", "AAA", "AA", "Z", "GBD", "GBN", "GBO", "GBS", "GBP", "KS", "RKS", "EU", "EUE"})
    void test011(String invalidCountryCode) {
        // Given
        PidCredentialData pidCredentialData = getPidCredentialDataOfNationality(invalidCountryCode);

        // When, then
        assertThatExceptionOfType(PidServerException.class).isThrownBy(() -> buildMdoc(() -> pidCredentialData));
    }

    private static IssuerAuth readIssuerAuth(String mdoc) {
        byte[] decodedMdoc = getDecodedString(mdoc);
        var cbor = CBORObject.DecodeFromBytes(decodedMdoc).get("issuerSigned").get("issuerAuth");
        return IssuerAuth.Companion.fromCBOR(cbor);
    }

    private static DeviceSigned readDeviceSigned(String mdoc) {
        byte[] decodedMdoc = getDecodedString(mdoc);
        var cbor = CBORObject.DecodeFromBytes(decodedMdoc).get("deviceSigned");
        return DeviceSigned.Companion.fromCBOR(cbor);
    }

    private static CBORObject readPidNameSpace(String mdoc) {
        byte[] decodedMdoc = getDecodedString(mdoc);
        var deviceSigned = CBORObject.DecodeFromBytes(decodedMdoc).get("deviceSigned");
        return CBORObject.DecodeFromBytes(deviceSigned.get("nameSpaces").GetByteString()).get(DOC_TYPE);
    }

    private static Map<String, Object> readPidValues(String mdoc) throws IOException {
        byte[] decodedMdoc = getDecodedString(mdoc);
        var deviceSigned = CBORObject.DecodeFromBytes(decodedMdoc).get("deviceSigned");
        var nameSpaces = CBORObject.DecodeFromBytes(deviceSigned.get("nameSpaces").GetByteString());
        return CBOR_MAPPER.readValue(nameSpaces.get(DOC_TYPE).EncodeToBytes(), new TypeReference<>() {});
    }

    protected static List<Certificate> getCertificateChain(CBORObject unprotectedHeaderParameters) throws CertificateException {
        var x5ChainParameter = unprotectedHeaderParameters.get(CoseHeaderParameters.X5CHAIN.label);
        byte[] x5ChainDer = x5ChainParameter.GetByteString();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return new ArrayList<>(certificateFactory.generateCertificates(new ByteArrayInputStream(x5ChainDer)));
    }

    private String buildMdoc(Supplier<PidCredentialData> pidCredentialDataSupplier) {
        PidCredentialData pidCredentialData = pidCredentialDataSupplier.get();
        try {
            return mdocBuilder.build(pidCredentialData, credentialRequest, null, FlowVariant.B);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
