/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.persistence;

import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.domain.PidIssuerNonce;
import de.bdr.pidi.end2end.integration.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

class PidiNonceAdapterTest extends IntegrationTest {

    @Autowired
    private PidiNonceAdapter pidiNonceAdapter;

    @Autowired
    private PidiNonceRepository pidiNonceRepository;

    @Test
    void testAdapter() {
        PidIssuerNonce nonce = pidiNonceAdapter.createAndSave(NonceFactory.createSecureRandomNonce(AUTH_CONFIG.getSessionExpirationTime()), AUTH_CONFIG.getSessionExpirationTime());

        assertThat(nonce.isUsed()).isFalse();
        assertThat(pidiNonceRepository.existsById(nonce.getId())).isTrue();
        assertThat(pidiNonceAdapter.findByNonce(nonce.getNonce().nonce(), AUTH_CONFIG.getSessionExpirationTime())).get().isEqualTo(nonce);

        nonce.setUsed(true);
        pidiNonceAdapter.setUsed(nonce);
        assertThat(pidiNonceRepository.findById(nonce.getId())).get().extracting(PidiNonceEntity::isUsed).isEqualTo(true);
    }
}