/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.signing.Pkcs12Signer;
import de.bdr.pidi.authorization.out.issuance.MetadataService;
import de.bdr.pidi.base.FileResourceHelper;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.issuance.core.IssuanceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Service
public class MetadataServiceImpl implements MetadataService {

    private final Collection<JWK> cachedJwks;

    @Autowired
    public MetadataServiceImpl(IssuanceConfiguration configuration) {
        cachedJwks = readJwks(configuration.getSignerPath(), configuration.getSignerPassword());
    }

    @Override
    public Collection<JWK> getJwks() {
        return Collections.unmodifiableCollection(cachedJwks);
    }

    @SuppressWarnings({"java:S4970"}) // IOException could be thrown in Pkcs12Signer constructor
    private Collection<JWK> readJwks(String signerPath, String signerPassword) {
        final Collection<JWK> jwks = new ArrayList<>();
        try {
            var pkcs12Signer = new Pkcs12Signer(FileResourceHelper.getFileInputStream(signerPath), signerPassword);
            jwks.add(pkcs12Signer.getKeys().getJwk());
            return jwks;
        } catch (FileNotFoundException e) {
            throw new PidServerException("KeyStore not found", e);
        } catch (IOException e) {
            throw new PidServerException("Keystore password invalid", e);
        }
    }
}
