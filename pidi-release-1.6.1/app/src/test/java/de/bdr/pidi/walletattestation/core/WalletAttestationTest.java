/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.walletattestation.core;

import com.nimbusds.jwt.SignedJWT;
import de.bdr.openid4vc.vci.service.attestation.ClientAttestationCnf;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import de.bdr.pidi.walletattestation.ClientAttestationJwt;
import de.bdr.pidi.walletattestation.ClientAttestationPopJwt;
import de.bdr.pidi.walletattestation.WalletAttestationRequest;
import de.bdr.pidi.walletattestation.config.AttestationConfiguration;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletAttestationTest {
    @Spy
    static AttestationConfiguration configuration;
    @Mock
    private ClientConfigurationService clientConfigurationService;

    @InjectMocks
    private WalletAttestation walletAttestation;

    static {
        configuration = new AttestationConfiguration();
        configuration.setProofValidity(Duration.ofSeconds(30));
        configuration.setProofTimeTolerance(Duration.ofSeconds(30));
    }

    @DisplayName("Verify valid wallet attestation")
    @Test
    void test001() {
        when(clientConfigurationService.isValidClientId(any())).thenReturn(true);
        when(clientConfigurationService.getJwk(any())).thenReturn(TestUtils.CLIENT_PUBLIC_KEY);
        SignedJWT clientAttestationSignedJwt = TestUtils.getClientAttestationJwt("issuer", "subject", now().plusSeconds(30L), now(), now(), "cnf", Map.of("jwk", TestUtils.getClientInstanceKeyMap()));
        ClientAttestationJwt clientAttestationJwt = new ClientAttestationJwt(clientAttestationSignedJwt, ClientIds.validClientId().toString(), ClientIds.validClientId().toString(), now().plusSeconds(30L), now(), now(), new ClientAttestationCnf(Map.of("jwk", TestUtils.getClientInstanceKeyMap())));
        SignedJWT clientAttestationPopSignedJwt = TestUtils.getClientAttestationPopJwt("issuer", now().plusSeconds(30L), now(), now(), List.of("audience"), "jwtId");
        ClientAttestationPopJwt clientAttestationPopJwt = new ClientAttestationPopJwt(clientAttestationPopSignedJwt, ClientIds.validClientId().toString(), now().plusSeconds(30L), "jti", List.of("audience"), now(), now());
        WalletAttestationRequest walletAttestationRequest = new WalletAttestationRequest(clientAttestationJwt, clientAttestationPopJwt, ClientIds.validClientId().toString(), "audience");

        boolean isValidWallet = walletAttestation.isValidWallet(walletAttestationRequest);
        assertThat(isValidWallet).isTrue();
    }

    @DisplayName("Verify wallet attestation with invalid clientId")
    @Test
    void test002() {
        when(clientConfigurationService.isValidClientId(any())).thenReturn(false);
        SignedJWT clientAttestationSignedJwt = TestUtils.getClientAttestationJwt("issuer", "subject", now().plusSeconds(30L), now(), now(), "cnf", Map.of("jwk", TestUtils.getClientInstanceKeyMap()));
        ClientAttestationJwt clientAttestationJwt = new ClientAttestationJwt(clientAttestationSignedJwt, ClientIds.validClientId().toString(), ClientIds.validClientId().toString(), now().plusSeconds(30L), now(), now(), new ClientAttestationCnf(Map.of("jwk", TestUtils.getClientInstanceKeyMap())));
        SignedJWT clientAttestationPopSignedJwt = TestUtils.getClientAttestationPopJwt("issuer", now().plusSeconds(30L), now(), now(), List.of("audience"), "jwtId");
        ClientAttestationPopJwt clientAttestationPopJwt = new ClientAttestationPopJwt(clientAttestationPopSignedJwt, ClientIds.validClientId().toString(), now().plusSeconds(30L), "jti", List.of("audience"), now(), now());
        WalletAttestationRequest walletAttestationRequest = new WalletAttestationRequest(clientAttestationJwt, clientAttestationPopJwt, ClientIds.validClientId().toString(), "audience");

        var exception = assertThrows(IllegalArgumentException.class, () -> walletAttestation.isValidWallet(walletAttestationRequest));
        MatcherAssert.assertThat(exception.getMessage(), is("Client attestation issuer '" + ClientIds.validClientId().toString() + "' is not supported"));
    }

    @DisplayName("Verify wallet attestation with invalid subject at ClientAttestationJwt")
    @Test
    void test003() {
        UUID badSub = UUID.randomUUID();

        when(clientConfigurationService.isValidClientId(any())).thenReturn(true);
        SignedJWT clientAttestationSignedJwt = TestUtils.getClientAttestationJwt("issuer", "subject", now().plusSeconds(30L), now(), now(), "cnf", Map.of("jwk", TestUtils.getClientInstanceKeyMap()));
        ClientAttestationJwt clientAttestationJwt = new ClientAttestationJwt(clientAttestationSignedJwt, ClientIds.validClientId().toString(), badSub.toString(), now().plusSeconds(30L), now(), now(), new ClientAttestationCnf(Map.of("jwk", TestUtils.getClientInstanceKeyMap())));
        SignedJWT clientAttestationPopSignedJwt = TestUtils.getClientAttestationPopJwt("issuer", now().plusSeconds(30L), now(), now(), List.of("audience"), "jwtId");
        ClientAttestationPopJwt clientAttestationPopJwt = new ClientAttestationPopJwt(clientAttestationPopSignedJwt, ClientIds.validClientId().toString(), now().plusSeconds(30L), "jti", List.of("audience"), now(), now());
        WalletAttestationRequest walletAttestationRequest = new WalletAttestationRequest(clientAttestationJwt, clientAttestationPopJwt, ClientIds.validClientId().toString(), "audience");

        var exception = assertThrows(IllegalArgumentException.class, () -> walletAttestation.isValidWallet(walletAttestationRequest));
        MatcherAssert.assertThat(exception.getMessage(), is("Client attestation subject '" + badSub + "' does not match the client id '" + ClientIds.validClientId().toString() + "'"));
    }

    @DisplayName("Verify wallet attestation with invalid issuer at ClientAttestationPopJwt")
    @Test
    void test004() {
        UUID badIss = UUID.randomUUID();

        when(clientConfigurationService.isValidClientId(any())).thenReturn(true);
        when(clientConfigurationService.getJwk(any())).thenReturn(TestUtils.CLIENT_PUBLIC_KEY);
        SignedJWT clientAttestationSignedJwt = TestUtils.getClientAttestationJwt("issuer", "subject", now().plusSeconds(30L), now(), now(), "cnf", Map.of("jwk", TestUtils.getClientInstanceKeyMap()));
        ClientAttestationJwt clientAttestationJwt = new ClientAttestationJwt(clientAttestationSignedJwt, ClientIds.validClientId().toString(), ClientIds.validClientId().toString(), now().plusSeconds(30L), now(), now(), new ClientAttestationCnf(Map.of("jwk", TestUtils.getClientInstanceKeyMap())));
        SignedJWT clientAttestationPopSignedJwt = TestUtils.getClientAttestationPopJwt("issuer", now().plusSeconds(30L), now(), now(), List.of("audience"), "jwtId");
        ClientAttestationPopJwt clientAttestationPopJwt = new ClientAttestationPopJwt(clientAttestationPopSignedJwt, badIss.toString(), now().plusSeconds(30L), "jti", List.of("audience"), now(), now());
        WalletAttestationRequest walletAttestationRequest = new WalletAttestationRequest(clientAttestationJwt, clientAttestationPopJwt, ClientIds.validClientId().toString(), "audience");

        var exception = assertThrows(IllegalArgumentException.class, () -> walletAttestation.isValidWallet(walletAttestationRequest));
        MatcherAssert.assertThat(exception.getMessage(), is("Client attestation issuer '" + badIss + "' does not match the client id '" + ClientIds.validClientId().toString() + "'"));
    }

    @DisplayName("Verify wallet attestation with invalid audience at ClientAttestationPopJwt")
    @Test
    void test005() {
        when(clientConfigurationService.isValidClientId(any())).thenReturn(true);
        when(clientConfigurationService.getJwk(any())).thenReturn(TestUtils.CLIENT_PUBLIC_KEY);
        SignedJWT clientAttestationSignedJwt = TestUtils.getClientAttestationJwt("issuer", "subject", now().plusSeconds(30L), now(), now(), "cnf", Map.of("jwk", TestUtils.getClientInstanceKeyMap()));
        ClientAttestationJwt clientAttestationJwt = new ClientAttestationJwt(clientAttestationSignedJwt, ClientIds.validClientId().toString(), ClientIds.validClientId().toString(), now().plusSeconds(30L), now(), now(), new ClientAttestationCnf(Map.of("jwk", TestUtils.getClientInstanceKeyMap())));
        SignedJWT clientAttestationPopSignedJwt = TestUtils.getClientAttestationPopJwt("issuer", now().plusSeconds(30L), now(), now(), List.of("audience"), "jwtId");
        ClientAttestationPopJwt clientAttestationPopJwt = new ClientAttestationPopJwt(clientAttestationPopSignedJwt, ClientIds.validClientId().toString(), now().plusSeconds(30L), "jti", List.of("BadAudience"), now(), now());
        WalletAttestationRequest walletAttestationRequest = new WalletAttestationRequest(clientAttestationJwt, clientAttestationPopJwt, ClientIds.validClientId().toString(), "audience");

        var exception = assertThrows(IllegalArgumentException.class, () -> walletAttestation.isValidWallet(walletAttestationRequest));
        MatcherAssert.assertThat(exception.getMessage(), is("Client attestation issuer audience unknown"));
    }
}
