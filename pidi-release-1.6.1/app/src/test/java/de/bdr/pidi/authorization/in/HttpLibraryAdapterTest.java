/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.exception.InvalidProofException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.Cleanup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static de.bdr.pidi.base.PidDataConst.SD_JWT_VCTYPE_PATH;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpLibraryAdapterTest {

    private static final String AUTHORITY = "http://localhost:8080/";
    private static final String SD_JWT_VCTYPE = AUTHORITY + SD_JWT_VCTYPE_PATH;

    @Test
    void test001() throws URISyntaxException, MalformedURLException {
        var headers = new HttpHeaders();
        var params = new LinkedMultiValueMap<String, String>();
        var body = """
                {
                    "format": "vc+sd-jwt",
                    "vct": "%s",
                    "proofs": {
                       \s
                    }
                }\s
                """.formatted(SD_JWT_VCTYPE);
        @Cleanup MockedStatic<ServletUriComponentsBuilder> servletUriComponentsBuilderMock = mockStatic(ServletUriComponentsBuilder.class);
        var mock = Mockito.mock(ServletUriComponentsBuilder.class);
        var configuration = Mockito.mock(AuthorizationConfiguration.class);
        servletUriComponentsBuilderMock.when(ServletUriComponentsBuilder::fromCurrentRequest).thenReturn(mock);
        when(mock.build()).thenReturn(UriComponentsBuilder.fromUriString(AUTHORITY + "c1/credentails").build());
        when(configuration.getBaseUrl()).thenReturn(new URI(AUTHORITY).toURL());
        var adapter = new HttpLibraryAdapter(configuration);

        assertThatThrownBy(() -> adapter.getLibraryCredentialRequest(FlowVariant.C1, HttpMethod.POST, headers, params, body))
                .isInstanceOf(InvalidProofException.class)
                .hasMessage("Proof JWT could not be parsed");
    }
}