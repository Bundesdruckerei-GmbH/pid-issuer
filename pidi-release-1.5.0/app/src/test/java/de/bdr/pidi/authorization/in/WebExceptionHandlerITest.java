/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in;

import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.core.exception.ParameterTooLongException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.authorization.core.flows.C1FlowController;
import de.bdr.pidi.authorization.core.particle.UseDpopNonceException;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.stream.Stream;

import static de.bdr.pidi.authorization.in.WebExceptionHandlerTest.DPOP_SIGNING_ALGS;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebExceptionHandlerITest {
    // I need a full SpringBootTest, so that the ControllerAdvice gets picked up,
    // a Web slice test ignores it

    @MockBean
    private C1FlowController flowController;

    @Autowired
    private MockMvc mockMvc;

    static Stream<Exception> springInternalExceptions() {
        return Stream.of(
                new ConversionNotSupportedException("Conversion object here.", null, null),
                new HttpMessageNotWritableException("Http Message not writable!"),
                new AsyncRequestTimeoutException()
        );
    }

    static Stream<Exception> serverExceptions() {
        return Stream.of(
                new NullPointerException("null pointer exception"),
                new ArrayIndexOutOfBoundsException("array index out of bounds, as runtime exception"),
                new IllegalArgumentException("infinity")
        );
    }

    private void given_Mock_throws_exception_on_authorize(Exception exception) {
        Mockito.when(flowController.processAuthRequest(any()))
                .thenThrow(exception);
    }

    private ResultActions when_get_authorize() throws Exception {
        return this.mockMvc.perform(get("/c1/authorize")
                .with(request -> {
                    request.setSecure(true);
                    return request;
                }));
    }

    @Test
    void given_MockMvc_when_getUnknownPath_then_NotFound() throws Exception {
        this.mockMvc.perform(get("/no-wombat-here")
                        .with(request -> {
                            request.setSecure(true);
                            return request;
                        }))
                .andExpect(status().is4xxClientError())
                .andExpect(status().isNotFound());
    }

    @Test
    void given_MockMvc_when_UseDpopNonceException_then_BadRequest() throws Exception {
        var nonce = RandomUtil.randomString();
        given_Mock_throws_exception_on_authorize(new UseDpopNonceException(nonce, "nonononce"));

        when_get_authorize()
                .andExpect(status().isBadRequest())
                .andExpect(header().stringValues("DPoP-Nonce", nonce));
    }

    @Test
    void given_MockMvc_when_UnauthorizedException_then_Unauthorized() throws Exception {
        var unauthorizedException = new UnauthorizedException(TokenType.DPOP.getValue(), "invalid_dpop_proof", "nooo");
        given_Mock_throws_exception_on_authorize(unauthorizedException);

        when_get_authorize()
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("DPoP-Nonce"))
                .andExpect(header().stringValues(HttpHeaders.WWW_AUTHENTICATE, "DPoP realm=\"oid4vci\", error=\"invalid_dpop_proof\", error_description=\"nooo\", algs=" + DPOP_SIGNING_ALGS));
    }

    @Test
    void given_MockMvc_when_UnauthorizedUseDpopNonceException_then_Unauthorized() throws Exception {
        var nonce = RandomUtil.randomString();
        var useDpopNonceException = new UseDpopNonceException(nonce, "nonononce");
        var unauthorizedException = new UnauthorizedException(TokenType.DPOP.getValue(), "invalid_dpop_proof", useDpopNonceException);
        given_Mock_throws_exception_on_authorize(unauthorizedException);

        when_get_authorize()
                .andExpect(status().isUnauthorized())
                .andExpect(header().stringValues("DPoP-Nonce", nonce))
                .andExpect(header().stringValues(HttpHeaders.WWW_AUTHENTICATE, "DPoP realm=\"oid4vci\", error=\"invalid_dpop_proof\", error_description=\"nonononce\", algs=" + DPOP_SIGNING_ALGS));
    }

    @Test
    void given_MockMvc_when_RequestTooLargeException_then_PayloadTooLarge() throws Exception {
        given_Mock_throws_exception_on_authorize(new ParameterTooLongException("state", 2048));

        when_get_authorize()
                .andExpect(status().isPayloadTooLarge());
    }

    @ParameterizedTest
    @MethodSource({"serverExceptions", "springInternalExceptions"})
    void given_MockMvc_when_exception_then_ServerError(Exception exception) throws Exception {

        given_Mock_throws_exception_on_authorize(exception);

        when_get_authorize()
                .andExpect(status().is5xxServerError());
    }
}
