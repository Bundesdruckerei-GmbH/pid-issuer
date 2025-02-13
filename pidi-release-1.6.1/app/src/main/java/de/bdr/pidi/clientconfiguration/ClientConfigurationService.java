/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.clientconfiguration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.clientconfiguration.config.ClientConfiguration;
import org.springframework.stereotype.Service;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientConfigurationService {

    private final Map<UUID, JWK> jwkMap;

    public ClientConfigurationService(ClientConfiguration configuration) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");

        jwkMap = configuration.getClientCert().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> {
                    try {
                        var is = getClass().getClassLoader().getResourceAsStream(entry.getValue());
                        var cert = (X509Certificate) cf.generateCertificate(is);
                        return JWK.parse(cert);
                    } catch (CertificateException | JOSEException e) {
                        throw new ClientConfigurationInitException("Failed initializing ClientConfigurationService, error at certificate for clientId " + entry.getKey(), e);
                    }
                }
        ));
    }

    public boolean isValidClientId(UUID clientId) {
        return jwkMap.containsKey(clientId);
    }
    public JWK getJwk(UUID clientId) {
        return jwkMap.get(clientId);
    }
}
