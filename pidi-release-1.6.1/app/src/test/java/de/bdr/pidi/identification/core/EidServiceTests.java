/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.identification.in.EidService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

@Slf4j
@SpringBootTest
class EidServiceTests {
    @Autowired
    private EidService eidService;
    @Value("${pidi.identification.server.url}")
    private String eidServerUrl;
    @Test
    void test001() throws MalformedURLException, URISyntaxException {
        String issuerState = RandomUtil.randomString();

        var response = eidService.startIdentificationProcess(URI.create("https://redirect.to.me").toURL(), issuerState, null);
        assertThat(response.toString(), startsWith(eidServerUrl + "?SAMLRequest="));
        MultiValueMap<String, String> requestParameters = UriComponentsBuilder.fromUri(response.toURI()).build().getQueryParams();
        var samlRequest = decode(requestParameters.getFirst("SAMLRequest"));
        var relayState = decode(requestParameters.getFirst("RelayState"));
        var sigAlg = decode(requestParameters.getFirst("SigAlg"));
        var signature = decode(requestParameters.getFirst("Signature"));

        log.info("samlRequest: {}", samlRequest);
        log.info("relayState: {}", relayState);
        log.info("sigAlg: {}", sigAlg);
        log.info("signature: {}", signature);
    }

    private static String decode(String enc) {
        return Optional.ofNullable(enc).map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8)).orElse(null);
    }
}
