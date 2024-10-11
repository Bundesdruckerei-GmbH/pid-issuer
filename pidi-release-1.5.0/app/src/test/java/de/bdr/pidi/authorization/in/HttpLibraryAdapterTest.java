/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.exception.InvalidProofException;
import lombok.Cleanup;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HttpLibraryAdapterTest {

    private final HttpLibraryAdapter adapter = new HttpLibraryAdapter();

    @Test
    void test001() {
        var headers = new HttpHeaders();
        var params = new LinkedMultiValueMap<String, String>();
        var body = """
                {
                    "format": "vc+sd-jwt",
                    "vct": "https://example.bmi.bund.de/credential/pid/1.0",
                    "proofs": {
                       \s
                    }
                }\s
                """;
        @Cleanup MockedStatic<ServletUriComponentsBuilder> servletUriComponentsBuilderMock = mockStatic(ServletUriComponentsBuilder.class);
        var mock = Mockito.mock(ServletUriComponentsBuilder.class);
        servletUriComponentsBuilderMock.when(ServletUriComponentsBuilder::fromCurrentRequest).thenReturn(mock);
        when(mock.build()).thenReturn(UriComponentsBuilder.fromHttpUrl("http://localhost:8080/c1/credentails").build());

        assertThatThrownBy(() -> adapter.getLibraryCredentialRequest(FlowVariant.C1, HttpMethod.POST, headers, params, body))
                .isInstanceOf(InvalidProofException.class)
                .hasMessage("Proof JWT could not be parsed");
    }
}