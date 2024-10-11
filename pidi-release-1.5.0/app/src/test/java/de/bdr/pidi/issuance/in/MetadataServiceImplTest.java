/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.out.issuance.MetadataService;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.issuance.core.IssuanceConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static de.bdr.pidi.issuance.ConfigTestData.ISSUANCE_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataServiceImplTest {

    @Spy
    IssuanceConfiguration configuration = ISSUANCE_CONFIG;

    @Test
    void shouldProcess() {
        MetadataService metadataService = new MetadataServiceImpl(configuration);

        Collection<JWK> jwks = metadataService.getJwks();
        assertThat(jwks).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenKeyStoreNotFound() {
        when(configuration.getSignerPath()).thenReturn("invalid.p12");

        var e = assertThrows(PidServerException.class, () -> new MetadataServiceImpl(configuration));
        assertThat(e.getMessage()).isEqualTo("KeyStore not found");
    }

    @Test
    void shouldThrowExceptionWhenKeyStorePasswordInvalid() {
        when(configuration.getSignerPassword()).thenReturn("invalid");

        var e = assertThrows(PidServerException.class, () -> new MetadataServiceImpl(configuration));
        assertThat(e.getMessage()).isEqualTo("Keystore password invalid");
    }
}