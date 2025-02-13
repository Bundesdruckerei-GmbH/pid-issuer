/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.signing.Pkcs12Signer;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.service.IssuerMetadataService;
import de.bdr.pidi.authorization.in.model.SdJwtVcMetadata;
import de.bdr.pidi.base.PidServerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataControllerTest {

    private static JWK testJwk;

    @Spy
    private AuthorizationConfiguration authConfig = AUTH_CONFIG;
    @Mock
    private IssuerMetadataService issuerMetadataService;

    @InjectMocks
    private MetadataController metadataController;

    @BeforeAll
    static void setUpAll() {
        var pkcs12Signer = new Pkcs12Signer(Objects.requireNonNull(MetadataControllerTest.class.getResourceAsStream("/keystore/issuance-test-keystore.p12")), "issuance-test");
        testJwk = pkcs12Signer.getKeys().getJwk();
    }

    @Test
    void getSdJwtVcMetadataEmptyKeyStore() {
        when(issuerMetadataService.getIssuerJwks()).thenReturn(Collections.emptyList());

        var e = assertThrows(PidServerException.class, () -> metadataController.getSdJwtVcMetadata("c"));
        assertThat(e.getMessage()).isEqualTo("No certificate found for c");
    }

    @Test
    void getSdJwtVcMetadataKeyStoreWithTwoCertificates() {
        when(issuerMetadataService.getIssuerJwks()).thenReturn(List.of(testJwk, testJwk));

        SdJwtVcMetadata response = metadataController.getSdJwtVcMetadata("c");
        assertThat(response).isNotNull();
        assertThat(response.issuer()).isEqualTo(authConfig.getCredentialIssuerIdentifier(FlowVariant.C));
        assertThat(response.jwks().keys()).hasSize(2);
    }
}