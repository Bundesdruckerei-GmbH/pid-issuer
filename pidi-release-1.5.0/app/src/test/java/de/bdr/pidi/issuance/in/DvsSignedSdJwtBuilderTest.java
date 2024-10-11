/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.X509CertUtils;
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256Key;
import de.bdr.pidi.authorization.out.issuance.FaultyRequestParameterException;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.base.PidDataConst;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.issuance.core.DvsVerifier;
import de.bdr.pidi.testdata.TestUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DvsSignedSdJwtBuilderTest extends SdJwtBuilderTestBase {
    public static final String ISSUER_URL = "http://localhost:8080/b";
    public static final Duration CREDENTIAL_LIFETIME = Duration.ofDays(14L);
    private static SdJwtVcAuthChannelCredentialRequest credentialRequest;
    private static SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest> sdJwtBuilder;
    private static SdJwtBuilder multiCertificatesChainSdJwtBuilder;
    private static String holderBindingKey;

    private static DvsVerifier dvsVerifier;

    @BeforeAll
    static void setUp() throws JOSEException, NoSuchAlgorithmException, InvalidKeyException, CertificateException,
            IOException, KeyStoreException, UnrecoverableKeyException, OperatorCreationException {
        // Generate verifier keys for dvs
        var keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        var verifierKeyPair = keyPairGenerator.generateKeyPair();
        var verifierPublicKey = (ECPublicKey) verifierKeyPair.getPublic();
        var verifierPrivateKey = (ECPrivateKey) verifierKeyPair.getPrivate();

        // Load signer keys
        var ks = KeyStore.getInstance("pkcs12");
        ks.load(Objects.requireNonNull(IssuerSignedSdJwtBuilderTest.class.getResourceAsStream("/keystore/issuance-test-keystore.p12")), "issuance-test".toCharArray());
        var alias = ks.aliases().nextElement();
        var signerPrivateKey = (ECPrivateKey) ks.getKey(alias, "issuance-test".toCharArray());
        var signerPublicKey = (ECPublicKey) ks.getCertificate(alias).getPublicKey();
        var certificateChain = Arrays.stream(ks.getCertificateChain(alias)).map(X509Certificate.class::cast).toList();

        setUpMultiCertificatesChainSdJwtBuilder(signerPrivateKey);

        // Create verifier
        var dvsVerifierKey = new DVSP256SHA256Key(verifierPrivateKey, signerPublicKey);
        dvsVerifier = new DvsVerifier(dvsVerifierKey);

        // Create sdJwtBuilder
        sdJwtBuilder = new DvsSignedSdJwtBuilder(ISSUER_URL, CREDENTIAL_LIFETIME, signerPrivateKey, certificateChain);

        // Prepare parameters
        holderBindingKey = ECKey.load(ks, alias, "issuance-test".toCharArray()).toPublicJWK().toJSONString();
        var verifierPub = new ECKey.Builder(new Curve(Curve.P_256.getName()), verifierPublicKey).build();
        credentialRequest = new SdJwtVcAuthChannelCredentialRequest(SdJwtVcAuthChannelCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.SD_JWT_VCTYPE, verifierPub);
    }

    private static void setUpMultiCertificatesChainSdJwtBuilder(ECPrivateKey signerPrivateKey) throws CertIOException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        var multiCertificatesChain = createMultiCertificatesChain();

        multiCertificatesChainSdJwtBuilder = new DvsSignedSdJwtBuilder(ISSUER_URL, CREDENTIAL_LIFETIME, signerPrivateKey, multiCertificatesChain);
    }

    @NotNull
    private static List<X509Certificate> createMultiCertificatesChain() throws CertIOException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        var keyPairGenerator = KeyPairGenerator.getInstance("EC");

        KeyPair rootKeyPair = keyPairGenerator.generateKeyPair();
        X509Certificate rootCert = generateCertificate("CN=Root CA", rootKeyPair, null, null);

        KeyPair intermediateKeyPair = keyPairGenerator.generateKeyPair();
        X509Certificate intermediateCert = generateCertificate("CN=Intermediate CA", intermediateKeyPair, rootCert, rootKeyPair.getPrivate());

        KeyPair endEntityKeyPair = keyPairGenerator.generateKeyPair();
        X509Certificate endEntityCert = generateCertificate("CN=End Entity", endEntityKeyPair, intermediateCert, intermediateKeyPair.getPrivate());

        return Arrays.stream(new X509Certificate[] {endEntityCert, intermediateCert, rootCert}).toList();
    }

    private static X509Certificate generateCertificate(String dn, KeyPair keyPair, X509Certificate issuerCert, PrivateKey issuerPrivateKey) throws CertIOException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        X500Name issuer = issuerCert == null ? new X500Name(dn) : new X500Name(issuerCert.getSubjectX500Principal().getName());
        X500Name subject = new X500Name(dn);

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        if (issuerCert == null) { // Root CA
            certBuilder.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
        } else { // Intermediate or End Entity
            certBuilder.addExtension(org.bouncycastle.asn1.x509.Extension.authorityKeyIdentifier, false,
                    new org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuerCert));
            certBuilder.addExtension(org.bouncycastle.asn1.x509.Extension.subjectKeyIdentifier, false,
                    new org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(issuerPrivateKey == null ? keyPair.getPrivate() : issuerPrivateKey);
        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    @DisplayName("DVS signed SdJwt contained the correct plain properties iss, issuing_authority, _sd_alg and vct")
    @Test
    void test001() throws ParseException {
        var sdJwt = sdJwtBuilder.build(getPidCredentialData(), credentialRequest, holderBindingKey);

        String decodedHeaderString = getDecodedHeader(sdJwt);
        String decodedPayloadString = getDecodedPayload(sdJwt);

        verifySignature(sdJwt, dvsVerifier);
        assertThat(decodedHeaderString).contains("\"x5c\":[\"MIICaTCCAg+gAwIBAgIUShyxcIZGiPV3wBRp4YOlNp1I13YwCgYIKoZIzj0EAwIwgYkxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIDAZiZHIuZGUxDzANBgNVBAcMBkJlcmxpbjEMMAoGA1UECgwDQkRSMQ8wDQYDVQQLDAZNYXVyZXIxHTAbBgNVBAMMFGlzc3VhbmNlLXRlc3QuYmRyLmRlMRowGAYJKoZIhvcNAQkBFgt0ZXN0QGJkci5kZTAeFw0yNDA1MjgwODIyMjdaFw0zNDA0MDYwODIyMjdaMIGJMQswCQYDVQQGEwJERTEPMA0GA1UECAwGYmRyLmRlMQ8wDQYDVQQHDAZCZXJsaW4xDDAKBgNVBAoMA0JEUjEPMA0GA1UECwwGTWF1cmVyMR0wGwYDVQQDDBRpc3N1YW5jZS10ZXN0LmJkci5kZTEaMBgGCSqGSIb3DQEJARYLdGVzdEBiZHIuZGUwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASygZ1Ma0m9uif4n8g3CiCP+E1r2KWFxVmS6LRWqUBMgn5fODKIBftdzVSbv/38gujy5qxh/q5bLcT+yLilazCao1MwUTAdBgNVHQ4EFgQUMGdPNMIdo3iHfqt2jlTnBNCfRNAwHwYDVR0jBBgwFoAUMGdPNMIdo3iHfqt2jlTnBNCfRNAwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiAu2h5xulXReb5IhgpkYiYR1BONTtsjT7nfzQAhL4ISOQIhAK6jKwwf6fTTSZwvJUOAu7dz1Dy/DmH19Lef0zqaNNht\"]");
        assertThat(decodedHeaderString).contains("\"kid\":\"MIGoMIGPpIGMMIGJMQswCQYDVQQGEwJERTEPMA0GA1UECAwGYmRyLmRlMQ8wDQYDVQQHDAZCZXJsaW4xDDAKBgNVBAoMA0JEUjEPMA0GA1UECwwGTWF1cmVyMR0wGwYDVQQDDBRpc3N1YW5jZS10ZXN0LmJkci5kZTEaMBgGCSqGSIb3DQEJARYLdGVzdEBiZHIuZGUCFEocsXCGRoj1d8AUaeGDpTadSNd2\"");
        assertThat(decodedPayloadString).contains("\"issuing_authority\":\"DE\"", "\"vct\":\"https://example.bmi.bund.de/credential/pid/1.0\"", "\"_sd_alg\":\"sha-256\"", "\"iss\":\"http://localhost:8080/b\"");
    }

    @DisplayName("DVS signed SdJwt contained a disclosure for all sd properties")
    @Test
    void test002() throws ParseException, NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        String sdJwt = sdJwtBuilder.build(getPidCredentialData(), credentialRequest, holderBindingKey);

        String decodedPayload = getDecodedPayload(sdJwt);
        String decodedSignature = getSignature(sdJwt);

        List<String> disclosures = getDisclosures(decodedSignature);
        List<String> listSdHashes = getAllSdHashVales(decodedPayload);
        List<String> hashedDisclosures = toHashedDisclosures(disclosures, messageDigest);

        verifySignature(sdJwt, dvsVerifier);
        assertThat(listSdHashes)
                .hasSameSizeAs(disclosures)
                .hasSameElementsAs(hashedDisclosures);
    }

    @DisplayName("Builder throws exception on missing verifier_pub")
    @Test
    void test003() {
        assertThatThrownBy(() -> new SdJwtVcAuthChannelCredentialRequest(SdJwtVcAuthChannelCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.SD_JWT_VCTYPE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("Builder throws exception on invalid verifier_pub")
    @Test
    void test004() {
        var verifierPub = TestUtils.CLIENT_PUBLIC_KEY;
        var noVerifierPubRequest = new SdJwtVcAuthChannelCredentialRequest(SdJwtVcAuthChannelCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.SD_JWT_VCTYPE, verifierPub);
        var pidCredentialData = getPidCredentialData();

        assertThatThrownBy(() -> sdJwtBuilder.build(pidCredentialData, noVerifierPubRequest, holderBindingKey))
                .isInstanceOf(FaultyRequestParameterException.class);
    }

    @DisplayName("DVS signed SD-JWT contained the complete certificate chain")
    @Test
    void test005() throws ParseException, JsonProcessingException {
        // When
        String sdJwt = multiCertificatesChainSdJwtBuilder.build(getPidCredentialData(), credentialRequest, holderBindingKey);

        // Then
        var headerMap = parseHeader(sdJwt);

        List<?> x5c = (List<?>) headerMap.get("x5c");
        assertThat(x5c)
                .isNotNull()
                .hasSize(3)
                .allSatisfy(c -> assertThatNoException()
                        .isThrownBy(() -> X509CertUtils.parse(Base64.getDecoder().decode((String) c))));
    }

    private static HashMap<?, ?> parseHeader(String sdJwt) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        String decodedHeader = getDecodedHeader(sdJwt);
        return (HashMap<?, ?>) objectMapper.readValue(decodedHeader, HashMap.class);
    }
}
